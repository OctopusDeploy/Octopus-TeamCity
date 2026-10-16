package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.DeploymentApi;
import com.octopus.sdk.domain.Deployment;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.space.SpaceHome;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * The Promote release runner on the new (Go) CLI: create a release, deploy it to one environment,
 * then promote it to another — all via a connection, with {@code OCTOPUS_NEW_CLI} set so every step
 * takes the new-CLI path. {@link OctopusPromoteReleaseE2ETest} covers the same flow on the legacy
 * CLI. Verified via the SDK that a deployment to the promote-target environment exists, and via the
 * log that the promotion ran as {@code octopus release promote}.
 */
class OctopusPromoteReleaseNewCliE2ETest {

  private static final String OCTOPUS_PROJECT = "PromoteReleaseNewCliIT";
  private static final String RELEASE_VERSION = "1.0.0";
  // Unique to this test — environment names are space-global on the shared Octopus.
  private static final String DEV = "PromoteReleaseNewCliIT-Development";
  private static final String PROD = "PromoteReleaseNewCliIT-Production";

  private static final String TC_PROJECT_ID = "PromNewCliIT";
  private static final String TC_BUILD_TYPE_ID = "PromNewCliIT_Promote";

  @Test
  void promoteReleaseStepOnNewCliPromotesToTheNextEnvironment() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();
      final SpaceHome spaceHome = stack.spaceHome(client);

      final String devEnvironmentId = OctopusProvisioning.createEnvironment(client, spaceHome, DEV);
      final String prodEnvironmentId =
          OctopusProvisioning.createEnvironment(client, spaceHome, PROD);
      OctopusProvisioning.createProjectWithServerScriptStep(
          client,
          spaceHome,
          stack.octopusUrlForHost(),
          stack.octopusApiKey(),
          OCTOPUS_PROJECT,
          Arrays.asList(devEnvironmentId, prodEnvironmentId));

      // Provision TeamCity: project, connection, build type with create -> deploy(Dev) ->
      // promote(Dev -> Prod) steps, all on the new CLI.
      final TeamCityRest tc = stack.rest();
      tc.createProject(TC_PROJECT_ID, "Promote new CLI IT");
      final String connectionId =
          tc.createOctopusConnection(
              TC_PROJECT_ID,
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType(TC_BUILD_TYPE_ID, "Create, deploy and promote", TC_PROJECT_ID);
      tc.setParameter(TC_BUILD_TYPE_ID, "env.OCTOPUS_NEW_CLI", "true");
      tc.addCreateReleaseStepUsingConnection(
          TC_BUILD_TYPE_ID, connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);
      tc.addDeployReleaseStepUsingConnection(
          TC_BUILD_TYPE_ID, connectionId, OCTOPUS_PROJECT, RELEASE_VERSION, DEV);
      tc.addPromoteReleaseStepUsingConnection(
          TC_BUILD_TYPE_ID, connectionId, OCTOPUS_PROJECT, DEV, PROD);

      final String buildId = tc.triggerBuild(TC_BUILD_TYPE_ID);
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(7));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // The promotion went through the new CLI, not the legacy octo promote-release.
      assertThat(log)
          .withFailMessage("Promotion did not run on the new CLI. Log:\n%s", log)
          .contains("Running command:   octopus release promote");

      // The real side effect: the release was promoted — a deployment to Production exists.
      final List<Deployment> deployments = DeploymentApi.create(client, spaceHome).getAll();
      assertThat(deployments)
          .withFailMessage("No deployment to environment %s found", PROD)
          .anyMatch(d -> prodEnvironmentId.equals(d.getProperties().getEnvironmentId()));

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
