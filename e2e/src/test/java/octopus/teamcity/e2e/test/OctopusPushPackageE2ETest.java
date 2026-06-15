package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.PackageApi;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.packages.PackageResource;
import com.octopus.sdk.model.space.SpaceHome;

import java.time.Duration;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Full end-to-end for the Push runner: pack a package on the agent, then push it (via a connection)
 * to the Octopus built-in feed with the real {@code octo} CLI. Uses the default Space (the
 * free-tier license allows only one), so the connection's space is left blank.
 */
class OctopusPushPackageE2ETest {

  private static final String PACKAGE_ID = "pushit.example";
  private static final String PACKAGE_VERSION = "1.0.0";

  @Test
  void pushStepPushesPackedPackageToBuiltInFeed() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();

      final TeamCityRest tc = stack.rest();
      tc.createProject("PushIT", "Push IT");
      final String connectionId =
          tc.createOctopusConnection(
              "PushIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "3.0+",
              "");
      tc.createBuildType("PushIT_Push", "Pack and push", "PushIT");
      tc.addCommandLineStep(
          "PushIT_Push", "Create source", "mkdir -p topack && printf 'hi' > topack/readme.txt");
      tc.addPackStep("PushIT_Push", PACKAGE_ID, "zip", PACKAGE_VERSION, "topack", "packed");
      tc.addPushStepUsingConnection(
          "PushIT_Push", connectionId, "packed/" + PACKAGE_ID + "." + PACKAGE_VERSION + ".zip");

      final String buildId = tc.triggerBuild("PushIT_Push");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(6));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // The real side effect: the package now exists in Octopus's built-in feed.
      final SpaceHome spaceHome = stack.spaceHome(client);
      final List<PackageResource> packages = PackageApi.create(client, spaceHome).getAll();
      assertThat(packages)
          .withFailMessage(
              "Package %s %s not found in the built-in feed", PACKAGE_ID, PACKAGE_VERSION)
          .anyMatch(
              p -> PACKAGE_ID.equals(p.getPackageId()) && PACKAGE_VERSION.equals(p.getVersion()));

      // The connection's API key is masked in the build log.
      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
