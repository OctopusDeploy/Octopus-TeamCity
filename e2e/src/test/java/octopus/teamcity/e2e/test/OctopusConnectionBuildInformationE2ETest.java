package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.BuildInformationApi;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.domain.BuildInformation;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.space.SpaceHome;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Full end-to-end: a Build Information step that references an {@code OctopusConnection} (instead
 * of inline URL/API key) resolves the connection at build start and publishes build information to
 * a real Octopus Deploy.
 *
 * <p>Uses the default Space — the free-tier Octopus license allows only one space, so we don't
 * create another. Leaving the connection's space blank makes {@code octo} target the default space.
 */
class OctopusConnectionBuildInformationE2ETest {

  private static final String PACKAGE_ID = "mypackage.noreally";
  private static final String PACKAGE_VERSION = "1.0.0";

  @Test
  void buildInfoStepUsingConnectionPublishesToOctopus() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      // 1. Octopus SDK client (for verification) against the default space.
      final OctopusClient client =
          OctopusClientFactory.createClient(
              new ConnectData(
                  new URL(stack.octopusUrlForHost()),
                  stack.octopusApiKey(),
                  Duration.ofSeconds(20)));

      // 2. Provision TeamCity via REST: project, connection, build type + build-info step. The
      // connection's space is left blank so the step targets the default space.
      final TeamCityRest tc = stack.rest();
      tc.createProject("ConnIT", "Connection IT");
      final String connectionId =
          tc.createOctopusConnection(
              "ConnIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "3.0+",
              "");
      tc.createBuildType("ConnIT_BuildInfo", "Publish build info", "ConnIT");
      tc.addBuildInfoStepUsingConnection(
          "ConnIT_BuildInfo", connectionId, PACKAGE_ID, PACKAGE_VERSION);

      // 3. Run the build.
      final String buildId = tc.triggerBuild("ConnIT_BuildInfo");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(4));
      final String log = tc.downloadBuildLog(buildId);

      // 4a. Build succeeded (the connection resolved to a reachable Octopus + valid key).
      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // 4b. The real side effect happened in Octopus — proves the connection's URL+key were used.
      final SpaceHome spaceHome = new SpaceHomeApi(client).getDefault();
      // The shared Octopus accumulates build information across tests, so scope to this package.
      final List<BuildInformation> items = BuildInformationApi.create(client, spaceHome).getAll();
      assertThat(items)
          .filteredOn(item -> PACKAGE_ID.equals(item.getProperties().getPackageId()))
          .hasSize(1);

      // 4c. The API key is masked in the build log.
      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
