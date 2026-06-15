package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.DeploymentApi;
import com.octopus.sdk.domain.Deployment;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.space.SpaceHome;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Full end-to-end for the Deploy release runner: create a release then deploy it to an environment,
 * both via a connection, and wait for the deployment to finish. The deployment process is a single
 * run-on-server script step, so no deployment targets are needed; the environment is provisioned
 * via the SDK (a fresh free-tier Octopus has none). Verified via the SDK that a deployment to the
 * environment exists.
 */
class OctopusDeployReleaseE2ETest {

  private static final String OCTOPUS_PROJECT = "DeployReleaseIT";
  private static final String RELEASE_VERSION = "1.0.0";
  // Unique to this test — environment names are space-global on the shared Octopus.
  private static final String ENVIRONMENT = "DeployReleaseIT-Development";

  @Test
  void deployReleaseStepUsingConnectionDeploysToTheEnvironment() throws Exception {
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

      // Provision TeamCity: project, connection, build type with create-then-deploy steps.
      final TeamCityRest tc = stack.rest();
      tc.createProject("DepIT", "Deploy IT");
      final String connectionId =
          tc.createOctopusConnection(
              "DepIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "3.0+",
              "");
      tc.createBuildType("DepIT_Deploy", "Create and deploy", "DepIT");
      tc.addCreateReleaseStepUsingConnection(
          "DepIT_Deploy", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);
      tc.addDeployReleaseStepUsingConnection(
          "DepIT_Deploy", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION, ENVIRONMENT);

      final String buildId = tc.triggerBuild("DepIT_Deploy");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(6));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // The real side effect: a deployment to the target environment exists (and the build waited
      // for it to finish, so it succeeded).
      final List<Deployment> deployments = DeploymentApi.create(client, spaceHome).getAll();
      assertThat(deployments)
          .withFailMessage("No deployment to environment %s found", ENVIRONMENT)
          .anyMatch(d -> environmentId.equals(d.getProperties().getEnvironmentId()));

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
