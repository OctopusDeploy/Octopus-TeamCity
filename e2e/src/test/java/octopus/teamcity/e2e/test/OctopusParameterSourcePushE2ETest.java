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
 * Full end-to-end for the "Reference a parameter" API key source: the connection stores a {@code
 * %param%} reference instead of a stored secret, and the referenced (secret) build parameter holds
 * the real key. Proves the build-start processor injects the reference into the api-key slot and
 * TeamCity resolves it at build time, so the {@code octo} CLI authenticates and the package lands
 * in Octopus — with the key still masked in the log.
 */
class OctopusParameterSourcePushE2ETest {

  private static final String PACKAGE_ID = "paramit.example";
  private static final String PACKAGE_VERSION = "1.0.0";

  @Test
  void parameterSourceConnectionAuthenticatesViaReferencedParameter() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();

      final TeamCityRest tc = stack.rest();
      tc.createProject("ParamIT", "Parameter Source IT");
      final String connectionId =
          tc.createOctopusConnectionWithApiKeyParameter(
              "ParamIT",
              "IT Param Octopus",
              stack.octopusUrlForContainers(),
              "%octopus.apikey%",
              "3.0+",
              "");
      tc.createBuildType("ParamIT_Push", "Pack and push via parameter", "ParamIT");
      // The secret the connection's %octopus.apikey% reference resolves to at build time.
      tc.setPasswordParameter("ParamIT_Push", "octopus.apikey", stack.octopusApiKey());
      tc.addCommandLineStep(
          "ParamIT_Push", "Create source", "mkdir -p topack && printf 'hi' > topack/readme.txt");
      tc.addPackStep("ParamIT_Push", PACKAGE_ID, "zip", PACKAGE_VERSION, "topack", "packed");
      tc.addPushStepUsingConnection(
          "ParamIT_Push", connectionId, "packed/" + PACKAGE_ID + "." + PACKAGE_VERSION + ".zip");

      final String buildId = tc.triggerBuild("ParamIT_Push");
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

      // The key resolved from the secret parameter is masked in the build log.
      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
