package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Full end-to-end for the Pack runner: it invokes the real bundled {@code octo} CLI on the agent to
 * package files. Pack is purely local — it does not talk to an Octopus server — so this runs with a
 * TeamCity + agent stack and no Octopus.
 *
 * <p>This is the cheapest proof that the bundled CLI actually extracts and executes on a Linux
 * agent (the create/deploy/promote/push runners share that machinery).
 */
class OctopusPackPackageE2ETest {

  private static final String PACKAGE_ID = "packit.example";
  private static final String PACKAGE_VERSION = "1.0.0";

  @Test
  void packStepProducesAPackageArtifact() throws Exception {
    try (final OctopusTeamCityStack stack = OctopusTeamCityStack.startWithAgent()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("PackIT", "Pack IT");
      tc.createBuildType("PackIT_Pack", "Pack a package", "PackIT");
      // Lay down something to pack, then pack it as a zip and publish the result as an artifact.
      tc.addCommandLineStep(
          "PackIT_Pack", "Create source", "mkdir -p topack && printf 'hello' > topack/readme.txt");
      tc.addPackStep("PackIT_Pack", PACKAGE_ID, "zip", PACKAGE_VERSION, "topack", "packed");

      final String buildId = tc.triggerBuild("PackIT_Pack");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(4));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");
      // The packed file (id.version.format) is published as a build artifact — proves octo ran.
      final String packageFile = PACKAGE_ID + "." + PACKAGE_VERSION + ".zip";
      assertThat(tc.listBuildArtifacts(buildId))
          .withFailMessage("Expected artifact %s. Log:\n%s", packageFile, log)
          .contains(packageFile);
    }
  }
}
