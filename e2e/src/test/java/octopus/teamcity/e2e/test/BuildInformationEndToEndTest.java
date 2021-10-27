package octopus.teamcity.e2e.test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.BuildInformationApi;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.api.SpaceOverviewApi;
import com.octopus.sdk.api.UserApi;
import com.octopus.sdk.domain.BuildInformation;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.space.SpaceHome;
import com.octopus.sdk.model.space.SpaceOverviewWithLinks;
import com.octopus.testsupport.OctopusDeployServer;
import com.octopus.testsupport.OctopusDeployServerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.google.common.io.Resources;
import octopus.teamcity.e2e.dsl.TeamCityContainers;
import octopus.teamcity.e2e.dsl.TeamCityFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.BuildConfiguration;
import org.jetbrains.teamcity.rest.BuildConfigurationId;
import org.jetbrains.teamcity.rest.BuildState;
import org.jetbrains.teamcity.rest.BuildStatus;
import org.jetbrains.teamcity.rest.TeamCityInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Network;

public class BuildInformationEndToEndTest {

  private static final Logger LOG = LogManager.getLogger();

  // TODO(tmm): Should get this from the PROJECT!
  private final String SPACE_NAME = "My Space";

  @Test
  public void buildInformationStepPublishesToOctopusDeploy(@TempDir Path testDirectory)
      throws InterruptedException, IOException, URISyntaxException {

    final URL projectsImport = Resources.getResource("TeamCity_StepVnext.zip");

    final Network network = Network.newNetwork();
    final OctopusDeployServer octoServer = OctopusDeployServerFactory.create();

    final ConnectData connectData =
        new ConnectData(
            new URL(octoServer.getOctopusUrl()), octoServer.getApiKey(), Duration.ofSeconds(10));
    final OctopusClient client = OctopusClientFactory.createClient(connectData);
    final SpaceOverviewApi spaceOverviewApi = SpaceOverviewApi.create(client);
    final UserApi users = UserApi.create(client);

    final SpaceOverviewWithLinks newSpace =
        new SpaceOverviewWithLinks(
            SPACE_NAME, singleton(users.getCurrentUser().getProperties().getId()));
    spaceOverviewApi.create(newSpace);

    // This is required to ensure docker container (run as tcuser) is able to write
    final Path teamcityDataDir = testDirectory.resolve("teamcitydata");
    Files.createDirectories(teamcityDataDir);
    teamcityDataDir.toFile().setWritable(true, false);

    final TeamCityFactory tcFactory = new TeamCityFactory(teamcityDataDir, network);

    final TeamCityContainers teamCityContainers =
        tcFactory.createTeamCityServerAndAgent(
            octoServer.getPort(), octoServer.getApiKey(), Paths.get(projectsImport.toURI()));

    try {
      final TeamCityInstance tcRestApi = teamCityContainers.getRestAPi();

      final BuildConfiguration buildConf =
          tcRestApi.buildConfiguration(new BuildConfigurationId("StepVnext_ExecuteBuildInfo"));
      final Build build =
          buildConf.runBuild(emptyMap(), true, true, true, "My Test build run", null, false);

      waitForBuildToFinish(build, tcRestApi);

      final File logDump = teamcityDataDir.resolve("build.log").toFile();
      build.downloadBuildLog(logDump);
      final String logData =
          new String(Files.readAllBytes(logDump.toPath()), StandardCharsets.UTF_8);
      LOG.info(teamCityContainers.getAgentContainer().getLogs());

      assertThat(build.getStatus()).withFailMessage(() -> logData).isEqualTo(BuildStatus.SUCCESS);

      final SpaceHomeApi spaceHomeApi = new SpaceHomeApi(client);
      final SpaceHome spaceHome = spaceHomeApi.getByName(SPACE_NAME);
      final BuildInformationApi buildInfoApi = BuildInformationApi.create(client, spaceHome);
      final List<BuildInformation> items = buildInfoApi.getByQuery(emptyMap());

      assertThat(items.size()).isEqualTo(1);
      assertThat(items.get(0).getProperties().getPackageId()).isEqualTo("mypackage");
    } catch (final Exception e) {
      LOG.info("Failed to execute build");
      LOG.info(teamCityContainers.getAgentContainer().getLogs());
      throw e;
    } finally {
      // Turns out, some files get written to this directory by TC (as the tcuser) - and they need
      // to be destroyed.
      cleanupContainers(teamCityContainers);
    }
  }

  private void waitForBuildToFinish(final Build build, final TeamCityInstance tcRestApi)
      throws InterruptedException {
    final Duration buildTimeout = Duration.ofSeconds(60);
    final Instant buildStart = Instant.now();
    LOG.info("Waiting for requested build {} to complete", build.getId());
    while (Duration.between(buildStart, Instant.now()).minus(buildTimeout).isNegative()) {
      final Build updatedBuild = tcRestApi.build(build.getId());
      if (updatedBuild.getState().equals(BuildState.FINISHED)) {
        LOG.info("Build {} completed", build.getId());
        return;
      }
      Thread.sleep(1000);
    }
    LOG.warn("Build {} failed to complete within expected time limit", build.getId());
    throw new RuntimeException("Build Failed to complete within 30 seconds");
  }

  private void cleanupContainers(final TeamCityContainers teamCityContainers)
      throws IOException, InterruptedException {
    teamCityContainers
        .getServerContainer()
        .execInContainer("rm", "-rf", "/data/teamcity_server/datadir/system/buildserver.tmp");
    teamCityContainers
        .getServerContainer()
        .execInContainer("rm", "-rf", "/data/teamcity_server/datadir/system/artifacts");
    teamCityContainers
        .getServerContainer()
        .execInContainer(
            "rm", "-rf", "/data/teamcity_server/datadir/system/caches/plugins.unpacked");
    teamCityContainers
        .getServerContainer()
        .execInContainer(
            "rm", "-rf", "/data/teamcity_server/datadir/system/caches/pluginsDslCache/src");
    teamCityContainers
        .getServerContainer()
        .execInContainer(
            "rm",
            "-rf",
            "/data/teamcity_server/datadir/system/caches/buildsMetadata/metadataDB.tmp");
  }
}
