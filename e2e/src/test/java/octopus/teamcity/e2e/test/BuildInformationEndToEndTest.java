package octopus.teamcity.e2e.test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.BuildInformationApi;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.api.SpacesOverviewApi;
import com.octopus.sdk.api.UsersApi;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.buildinformation.OctopusPackageVersionBuildInformationMappedResource;
import com.octopus.sdk.model.spaces.SpaceHome;
import com.octopus.sdk.model.spaces.SpaceOverviewWithLinks;
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
    final SpacesOverviewApi spacesOverviewApi = SpacesOverviewApi.create(client);
    final UsersApi users = UsersApi.create(client);

    final SpaceOverviewWithLinks newSpace = new SpaceOverviewWithLinks();
    newSpace.setName(SPACE_NAME);
    newSpace.setSpaceManagersTeamMembers(singleton(users.getCurrentUser().getId()));
    spacesOverviewApi.create(newSpace);

    // This is required to ensure docker container (run as tcuser) is able to write
    Path teamcityDataDir = testDirectory.resolve("teamcitydata");
    Files.createDirectories(teamcityDataDir);
    teamcityDataDir.toFile().setWritable(true, false);

    final TeamCityFactory tcFactory = new TeamCityFactory(teamcityDataDir, network);

    final TeamCityContainers teamCityContainers =
        tcFactory.createTeamCityServerAndAgent(
            octoServer.getPort(), octoServer.getApiKey(), Paths.get(projectsImport.toURI()));

    final TeamCityInstance tcRestApi = teamCityContainers.getRestAPi();

    final BuildConfiguration buildConf =
        tcRestApi.buildConfiguration(new BuildConfigurationId("StepVnext_ExecuteBuildInfo"));
    final Build build =
        buildConf.runBuild(emptyMap(), true, true, true, "My Test build run", null, false);

    waitForBuildToFinish(build, tcRestApi);

    final File logDump = teamcityDataDir.resolve("build.log").toFile();
    build.downloadBuildLog(logDump);
    final String logData = new String(Files.readAllBytes(logDump.toPath()), StandardCharsets.UTF_8);
    LOG.info(teamCityContainers.getAgentContainer().getLogs());

    assertThat(build.getStatus()).withFailMessage(() -> logData).isEqualTo(BuildStatus.SUCCESS);

    final SpaceHomeApi spaceHomeApi = new SpaceHomeApi(client);
    final SpaceHome spaceHome = spaceHomeApi.getByName(SPACE_NAME);
    final BuildInformationApi buildInfoApi = BuildInformationApi.create(client, spaceHome);
    final List<OctopusPackageVersionBuildInformationMappedResource> items =
        buildInfoApi.getByQuery(emptyMap());

    assertThat(items.size()).isEqualTo(1);
    assertThat(items.get(0).getPackageId()).isEqualTo("mypackage.noreally");
  }

  private void waitForBuildToFinish(final Build build, final TeamCityInstance tcRestApi)
      throws InterruptedException {
    final Duration buildTimeout = Duration.ofSeconds(30);
    final Instant buildStart = Instant.now();
    LOG.info("Waiting for requested build {} to complete", build.getId());
    while (Duration.between(Instant.now(), buildStart).minus(buildTimeout).isNegative()) {
      final Build updatedBuild = tcRestApi.build(build.getId());
      if (updatedBuild.getState().equals(BuildState.FINISHED)) {
        return;
      }
      Thread.sleep(1000);
    }
    throw new RuntimeException("Build Failed to complete within 30 seconds");
  }
}
