package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.space.SpaceHome;

import java.time.Duration;
import java.util.Collections;

import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/** Runs a published runbook through the Run runbook step and asserts Octopus recorded the run. */
class OctopusRunRunbookE2ETest {

  private static final String OCTOPUS_PROJECT = "RunRunbookIT";
  private static final String RUNBOOK = "RunRunbookIT-Say hello";
  // Unique to this test — environment names are space-global on the shared Octopus.
  private static final String ENVIRONMENT = "RunRunbookIT-Development";

  @Test
  void runRunbookStepUsingConnectionRunsTheRunbook() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();
      final SpaceHome spaceHome = stack.spaceHome(client);

      final String environmentId =
          OctopusProvisioning.createEnvironment(client, spaceHome, ENVIRONMENT);
      OctopusProvisioning.createProjectWithServerScriptStep(
          client,
          spaceHome,
          stack.octopusUrlForHost(),
          stack.octopusApiKey(),
          OCTOPUS_PROJECT,
          Collections.singletonList(environmentId));
      final String runbookId =
          OctopusProvisioning.createRunbookWithServerScriptStep(
              client,
              spaceHome,
              stack.octopusUrlForHost(),
              stack.octopusApiKey(),
              OCTOPUS_PROJECT,
              RUNBOOK);

      final TeamCityRest tc = stack.rest();
      tc.createProject("RunbookIT", "Runbook IT");
      final String connectionId =
          tc.createOctopusConnection(
              "RunbookIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("RunbookIT_Run", "Run runbook", "RunbookIT");
      tc.addRunRunbookStepUsingConnection(
          "RunbookIT_Run", connectionId, OCTOPUS_PROJECT, RUNBOOK, ENVIRONMENT);

      final String buildId = tc.triggerBuild("RunbookIT_Run");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(6));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      assertThat(
              OctopusProvisioning.hasRunbookRun(
                  spaceHome, stack.octopusUrlForHost(), stack.octopusApiKey(), runbookId))
          .withFailMessage("No run of runbook %s found. Log:\n%s", RUNBOOK, log)
          .isTrue();

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
