package octopus.teamcity.e2e.dsl;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Iterator;

import com.google.common.io.Resources;
import net.lingala.zip4j.ZipFile;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.teamcity.rest.BuildAgent;
import org.jetbrains.teamcity.rest.TeamCityInstance;
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class TeamCityFactory {

  private static final Logger LOG = LogManager.getLogger();
  protected static final String USERNAME = "admin";
  protected static final String PASSWORD = "Password01!";

  private final Path teamCityDataDir;
  private final Network dockerNetwork;

  public TeamCityFactory(final Path teamCityDataDir, final Network dockerNetwork) {
    this.teamCityDataDir = teamCityDataDir;
    this.dockerNetwork = dockerNetwork;
  }

  public TeamCityContainers createTeamCityServerAndAgent(
      final int octopusServerPort, final String octopusServerApiKey, final Path projectZipToInstall)
      throws IOException, URISyntaxException {
    final String serverUrl =
        String.format("http://host.testcontainers.internal:%d", octopusServerPort);
    Testcontainers.exposeHostPorts(octopusServerPort);
    final TeamCityContainers container =
        createTeamCityServerAndAgent(serverUrl, octopusServerApiKey, projectZipToInstall);
    try {
      container.getServerContainer().execInContainer("chmod -R 777 /data/teamcity_server/datadir");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return container;
  }

  // When OctopusServer's URL can be fully specified
  public TeamCityContainers createTeamCityServerAndAgent(
      final String octopusServerUrl,
      final String octopusServerApiKey,
      final Path projectZipToInstall)
      throws IOException, URISyntaxException {

    setupDataDir(teamCityDataDir, projectZipToInstall);

    // need to update the test directory to replace <param name="octopus_host"
    // value=<octopusServerUrl> />
    final Path projectFile =
        Paths.get(
            teamCityDataDir.toString(), "config", "projects", "StepVnext", "project-config.xml");
    updateProjectFile(projectFile, octopusServerUrl, octopusServerApiKey);

    final GenericContainer<?> teamCityServer = createAndStartServer();

    final String teamCityUrl =
        String.format(
            "http://%s:%d", teamCityServer.getHost(), teamCityServer.getFirstMappedPort());
    LOG.info("TeamCity server running on {}", teamCityUrl);

    try {
      final GenericContainer<?> teamCityAgent = createAndStartAgent();

      final String tcServerUrlOnHost =
          String.format("http://localhost:%d", teamCityServer.getFirstMappedPort());
      final TeamCityInstance tcInstance =
          TeamCityInstanceFactory.httpAuth(tcServerUrlOnHost, USERNAME, PASSWORD);
      authoriseAgents(tcInstance);
      return new TeamCityContainers(teamCityServer, teamCityAgent, tcInstance);
    } catch (final Exception e) {
      teamCityServer.stop();
      throw e;
    }
  }

  private GenericContainer<?> createAndStartServer() {
    final GenericContainer<?> teamCityServer =
        new GenericContainer<>(DockerImageName.parse("jetbrains/teamcity-server:2021.2.3"))
            .withExposedPorts(8111)
            .waitingFor(Wait.forLogMessage(".*Super user authentication token.*", 1))
            .withNetwork(dockerNetwork)
            .withNetworkAliases("server")
            .withEnv(
                "TEAMCITY_SERVER_OPTS",
                "-Droot.log.level=TRACE -Dteamcity.development.mode=true "
                    + "-Doctopus.enable.step.vnext=true")
            .withStartupTimeout(Duration.ofMinutes(2))
            .withFileSystemBind(
                teamCityDataDir.toAbsolutePath().toString(),
                "/data/teamcity_server/datadir",
                BindMode.READ_WRITE);

    teamCityServer.start();
    return teamCityServer;
  }

  private GenericContainer<?> createAndStartAgent() {
    final GenericContainer<?> teamCityAgent =
        new GenericContainer<>(DockerImageName.parse("jetbrains/teamcity-agent:2021.2.3"))
            .withNetwork(dockerNetwork)
            .withEnv("SERVER_URL", "http://server:8111")
            .waitingFor(Wait.forLogMessage(".*jetbrains.buildServer.AGENT - Agent name was.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));
    teamCityAgent.start();
    return teamCityAgent;
  }

  private void updateProjectFile(
      final Path projectFilePath, final String httpEndpoint, final String apiKey)
      throws IOException {
    String projectContent = new String(Files.readAllBytes(projectFilePath), StandardCharsets.UTF_8);
    projectContent = updateField(projectContent, ConnectionPropertyNames.SERVER_URL, httpEndpoint);
    projectContent = updateField(projectContent, ConnectionPropertyNames.API_KEY, apiKey);

    Files.write(projectFilePath, projectContent.getBytes(StandardCharsets.UTF_8));
  }

  private static String updateField(
      final String input, final String fieldName, final String newValue) {
    final String prefix = "<param name=\"" + fieldName + "\" ";
    return input.replaceAll(prefix + ".*", prefix + "value=\"" + newValue + "\" />");
  }

  protected void setupDataDir(final Path teamCityDataDir, final Path projectZipToInstall)
      throws IOException, URISyntaxException {
    LOG.info("starting test - teamcity data dir is at {}", teamCityDataDir);
    final URL teamcityInitialConfig = Resources.getResource("teamcity_config.zip");
    new ZipFile(new File(teamcityInitialConfig.toURI()).toString())
        .extractAll(teamCityDataDir.toAbsolutePath().toString());

    new ZipFile(projectZipToInstall.toString())
        .extractAll(teamCityDataDir.toAbsolutePath().toString());

    LOG.info("unzipped config into {}", teamCityDataDir);
    // teamcity_plugin_dist property will be set by gradle buildsystem
    final String pluginDistribution = System.getenv("TEAMCITY_PLUGIN_DIST");
    if (pluginDistribution == null) {
      throw new RuntimeException(
          "TEAMCITY_PLUGIN_DIST env var has not been set - it should reference an installable "
              + "zip");
    }
    Files.copy(
        Paths.get(pluginDistribution),
        teamCityDataDir.resolve("plugins").resolve("Octopus.Teamcity.zip"),
        StandardCopyOption.REPLACE_EXISTING);
  }

  private void authoriseAgents(final TeamCityInstance tcInstance) {
    final Iterator<BuildAgent> iBuildAgent = tcInstance.buildAgents().all(true).iterator();
    while (iBuildAgent.hasNext()) {
      final BuildAgent agent = iBuildAgent.next();
      agent.setAuthorized(true);
    }
  }
}
