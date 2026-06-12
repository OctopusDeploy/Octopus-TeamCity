package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.ReleaseApi;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.domain.Release;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.space.SpaceHome;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Full end-to-end for the flagship Create release runner: a step that references a connection
 * creates a real release in Octopus — proving the connection's URL/key (and injected version) drive
 * a real {@code octo create-release}.
 *
 * <p>The Octopus project is provisioned via {@link OctopusProvisioning} (a project with one
 * run-on-server script step); {@code octo create-release} won't build a viable release plan for a
 * project with an empty deployment process.
 */
class OctopusCreateReleaseE2ETest {

  private static final String OCTOPUS_PROJECT = "CreateReleaseIT";
  private static final String RELEASE_VERSION = "1.0.0";

  @Test
  void createReleaseStepUsingConnectionCreatesARealRelease() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client =
          OctopusClientFactory.createClient(
              new ConnectData(
                  new URL(stack.octopusUrlForHost()),
                  stack.octopusApiKey(),
                  Duration.ofSeconds(20)));
      final SpaceHome spaceHome = new SpaceHomeApi(client).getDefault();

      OctopusProvisioning.createProjectWithServerScriptStep(
          client, spaceHome, stack.octopusUrlForHost(), stack.octopusApiKey(), OCTOPUS_PROJECT);

      // Provision TeamCity: project, connection, build type + create-release step.
      final TeamCityRest tc = stack.rest();
      tc.createProject("RelIT", "Release IT");
      final String connectionId =
          tc.createOctopusConnection(
              "RelIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "3.0+",
              "");
      tc.createBuildType("RelIT_Create", "Create release", "RelIT");
      tc.addCreateReleaseStepUsingConnection(
          "RelIT_Create", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);

      final String buildId = tc.triggerBuild("RelIT_Create");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(5));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // The real side effect: the release exists in Octopus.
      final List<Release> releases = ReleaseApi.create(client, spaceHome).getAll();
      assertThat(releases)
          .withFailMessage("Release %s not found in Octopus", RELEASE_VERSION)
          .anyMatch(r -> RELEASE_VERSION.equals(r.getProperties().getVersion()));

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
