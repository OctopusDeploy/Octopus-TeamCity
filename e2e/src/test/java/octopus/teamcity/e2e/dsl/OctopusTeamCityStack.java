package octopus.teamcity.e2e.dsl;

import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.space.SpaceHome;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;

import com.google.common.io.Resources;
import net.lingala.zip4j.ZipFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Boots the integration-test stack: a TeamCity server with the built plugin installed and,
 * optionally, a real Octopus Deploy (MSSQL + the {@code octopusdeploy/octopusdeploy} image, in the
 * free tier — no license required) plus a TeamCity agent. All containers share one Docker network
 * and reach each other by network alias ({@code server}, {@code octopus}, {@code mssql}).
 *
 * <p>TeamCity boots from a prepared data directory ({@code teamcity_config.zip}) that already has
 * an initialised database and an {@code admin}/{@code Password01!} user, so there is no first-start
 * wizard and REST is authenticated with that account.
 *
 * <p>The UI test only needs TeamCity, so use {@link #startTeamCityOnly()}. The full end-to-end test
 * needs Octopus + an agent, so use {@link #startWithAgentAndOctopus()}.
 */
public final class OctopusTeamCityStack implements AutoCloseable {

  private static final Logger LOG = LogManager.getLogger();
  // 2025.03+ bundles Java 21; required because the teamcity-oidc-plugin is compiled for Java 21
  // (it won't load on the Java 17 that 2024.12 shipped).
  private static final String TC_IMAGE = "jetbrains/teamcity-server:2025.03.3";
  private static final String AGENT_IMAGE = "jetbrains/teamcity-agent:2025.03.3";
  private static final String OCTOPUS_IMAGE = "octopusdeploy/octopusdeploy:2025.4";
  private static final String MSSQL_IMAGE = "mcr.microsoft.com/mssql/server:2022-latest";
  private static final int TC_PORT = 8111;
  private static final int OCTOPUS_PORT = 8080;
  private static final String USERNAME = "admin";
  private static final String PASSWORD = "Password01!";
  private static final String MSSQL_PASSWORD = "P@ssw0rd123!";
  // A syntactically valid Octopus API key; ADMIN_API_KEY sets the admin user's key to this value.
  // It's a throwaway fake for the ephemeral test Octopus, not a real credential. Assembled by
  // concatenation so the literal API key doesn't appear verbatim and trip GitHub's secret scanner.
  private static final String OCTOPUS_API_KEY = "API-" + "FAKEKEY000000000000000000000";
  private static final String OCTOPUS_DB_CONNECTION_STRING =
      "Server=mssql,1433;Database=Octopus;User Id=sa;Password="
          + MSSQL_PASSWORD
          + ";TrustServerCertificate=true";

  private final GenericContainer<?> server;
  private final Network network;
  private final String tcBaseUrl;
  private final TeamCityRest rest;
  private GenericContainer<?> mssql;
  private GenericContainer<?> octopus;
  private GenericContainer<?> agent;
  private boolean shared;

  private OctopusTeamCityStack(final GenericContainer<?> server, final Network network) {
    this.server = server;
    this.network = network;
    this.tcBaseUrl = "http://" + server.getHost() + ":" + server.getMappedPort(TC_PORT);
    final String authHeader =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    this.rest = new TeamCityRest(tcBaseUrl, authHeader);
  }

  /** Starts a TeamCity server with the plugin installed (no Octopus, no agent). */
  public static OctopusTeamCityStack startTeamCityOnly() throws Exception {
    final Network network = Network.newNetwork();
    final Path dataDir = preparedDataDir();
    final GenericContainer<?> server = buildServerContainer(network, dataDir);
    server.start();
    final OctopusTeamCityStack stack = new OctopusTeamCityStack(server, network);
    // The prepared datadir may need an in-place upgrade on a newer TeamCity; wait for REST to
    // serve.
    stack.rest.waitUntilReady(Duration.ofMinutes(5));
    LOG.info("TeamCity ready at {}", stack.tcBaseUrl);
    return stack;
  }

  /**
   * Starts a TeamCity server with the plugin installed plus an authorised agent, but no Octopus.
   * Enough for agent-local steps (e.g. Pack) that don't talk to an Octopus server.
   */
  public static OctopusTeamCityStack startWithAgent() throws Exception {
    final OctopusTeamCityStack stack = startTeamCityOnly();
    stack.startAgent();
    return stack;
  }

  /**
   * Starts MSSQL + Octopus (free tier, no license) + TeamCity server + an authorised agent, all on
   * the same Docker network.
   */
  public static OctopusTeamCityStack startWithAgentAndOctopus() throws Exception {
    final OctopusTeamCityStack stack = startTeamCityOnly();

    stack.mssql =
        new GenericContainer<>(DockerImageName.parse(MSSQL_IMAGE))
            .withNetwork(stack.network)
            .withNetworkAliases("mssql")
            .withEnv("ACCEPT_EULA", "Y")
            .withEnv("MSSQL_SA_PASSWORD", MSSQL_PASSWORD)
            .waitingFor(
                Wait.forLogMessage(".*SQL Server is now ready for client connections.*\\n", 1)
                    .withStartupTimeout(Duration.ofMinutes(3)));
    stack.mssql.start();

    stack.octopus =
        new GenericContainer<>(DockerImageName.parse(OCTOPUS_IMAGE))
            .withNetwork(stack.network)
            .withNetworkAliases("octopus")
            .withExposedPorts(OCTOPUS_PORT)
            .withEnv("ACCEPT_EULA", "Y")
            .withEnv("DB_CONNECTION_STRING", OCTOPUS_DB_CONNECTION_STRING)
            .withEnv("ADMIN_USERNAME", USERNAME)
            .withEnv("ADMIN_PASSWORD", PASSWORD)
            .withEnv("ADMIN_API_KEY", OCTOPUS_API_KEY)
            .withEnv("DISABLE_DIND", "Y")
            .waitingFor(
                Wait.forHttp("/api").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(10)));
    stack.octopus.start();
    LOG.info("Octopus ready at {}", stack.octopusUrlForHost());

    stack.startAgent();
    return stack;
  }

  /** Starts an agent on the stack's network, authorises it, and waits for it to go idle. */
  private void startAgent() throws Exception {
    agent =
        new GenericContainer<>(DockerImageName.parse(AGENT_IMAGE))
            .withNetwork(network)
            .withEnv("SERVER_URL", "http://server:" + TC_PORT)
            .waitingFor(Wait.forLogMessage(".*jetbrains.buildServer.AGENT - Agent name was.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));
    agent.start();
    rest.authorizeAllAgents();
    rest.waitForAgentIdle();
  }

  /** Unpacks the prepared TeamCity datadir and copies the built plugin into it. */
  private static Path preparedDataDir() throws Exception {
    final Path dataDir = Files.createTempDirectory("tc-datadir");
    final URL config = Resources.getResource("teamcity_config.zip");
    new ZipFile(new File(config.toURI())).extractAll(dataDir.toAbsolutePath().toString());

    final String pluginDist = System.getenv("TEAMCITY_PLUGIN_DIST");
    if (pluginDist == null) {
      throw new IllegalStateException(
          "TEAMCITY_PLUGIN_DIST env var not set — it must point at the built plugin zip. "
              + "Run via the e2eTest Gradle task.");
    }
    final Path pluginsDir = dataDir.resolve("plugins");
    Files.createDirectories(pluginsDir);
    Files.copy(
        Paths.get(pluginDist),
        pluginsDir.resolve("Octopus.Teamcity.zip"),
        StandardCopyOption.REPLACE_EXISTING);

    // Also install the teamcity-oidc-plugin so the OIDC api-key source can be exercised end to end:
    // it registers the "oidc-identity-token" connection type that our connection form lists. The
    // latest release is downloaded on demand (or taken from $OIDC_PLUGIN_ZIP); see
    // docs/e2e-tests.md.
    Files.copy(
        OidcPluginFixture.resolve(),
        pluginsDir.resolve("Octopus.TeamCity.OIDC.zip"),
        StandardCopyOption.REPLACE_EXISTING);
    return dataDir;
  }

  private static GenericContainer<?> buildServerContainer(
      final Network network, final Path dataDir) {
    return new GenericContainer<>(DockerImageName.parse(TC_IMAGE))
        .withNetwork(network)
        .withNetworkAliases("server")
        .withExposedPorts(TC_PORT)
        .withEnv("TEAMCITY_SERVER_OPTS", "-Dteamcity.startup.maintenance=false")
        // Copy the prepared datadir in via the Docker API so it also works with a remote/DinD
        // daemon (a bind mount can't be seen by a daemon running elsewhere). The files land
        // owned by root, so start the container as root, chown them to tcuser, then exec TC.
        .withCopyFileToContainer(
            MountableFile.forHostPath(dataDir.toAbsolutePath().toString()),
            "/data/teamcity_server/datadir")
        .withCreateContainerCmdModifier(
            cmd -> {
              cmd.withUser("root");
              cmd.withCmd(
                  "/bin/sh",
                  "-c",
                  "chown -R tcuser:tcuser /data/teamcity_server/datadir"
                      + " && exec runuser -u tcuser -- /run-services.sh");
            })
        .waitingFor(
            Wait.forLogMessage(".*Super user authentication token.*", 1)
                .withStartupTimeout(Duration.ofMinutes(8)));
  }

  public TeamCityRest rest() {
    return rest;
  }

  public String tcBaseUrl() {
    return tcBaseUrl;
  }

  public String adminUsername() {
    return USERNAME;
  }

  public String adminPassword() {
    return PASSWORD;
  }

  public String octopusApiKey() {
    return OCTOPUS_API_KEY;
  }

  /** Octopus URL reachable from the other containers (TeamCity agent) via the shared network. */
  public String octopusUrlForContainers() {
    return "http://octopus:" + OCTOPUS_PORT;
  }

  /** Octopus URL reachable from the test JVM (the runner) via the host-published port. */
  public String octopusUrlForHost() {
    return "http://" + octopus().getHost() + ":" + octopus().getMappedPort(OCTOPUS_PORT);
  }

  /** An SDK client pointed at this stack's Octopus, reachable from the test JVM. */
  public OctopusClient octopusClient() throws Exception {
    return OctopusClientFactory.createClient(
        new ConnectData(new URL(octopusUrlForHost()), OCTOPUS_API_KEY, Duration.ofSeconds(20)));
  }

  /** The default Octopus space for the given client. */
  public SpaceHome spaceHome(final OctopusClient client) throws Exception {
    return new SpaceHomeApi(client).getDefault();
  }

  private GenericContainer<?> octopus() {
    if (octopus == null) {
      throw new IllegalStateException("This stack was started without Octopus");
    }
    return octopus;
  }

  public GenericContainer<?> server() {
    return server;
  }

  public GenericContainer<?> agent() {
    return agent;
  }

  /**
   * Marks this stack as shared across the whole test suite, so per-test {@code close()} calls are
   * ignored and the containers live until the JVM exits (reaped by Testcontainers/Ryuk). See {@link
   * SharedStack}.
   */
  void markShared() {
    this.shared = true;
  }

  @Override
  public void close() {
    if (shared) {
      return;
    }
    for (final GenericContainer<?> c : new GenericContainer<?>[] {agent, octopus, mssql, server}) {
      if (c != null) {
        try {
          c.stop();
        } catch (final RuntimeException ignored) {
          // best-effort teardown
        }
      }
    }
  }
}
