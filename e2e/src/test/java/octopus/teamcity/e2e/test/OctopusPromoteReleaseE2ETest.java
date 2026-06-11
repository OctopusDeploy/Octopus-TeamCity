package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.DeploymentApi;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.domain.Deployment;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.space.SpaceHome;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Full end-to-end for the Promote release runner: create a release, deploy it to one environment,
 * then promote it to another — all via a connection. The deployment process is a single
 * run-on-server script step (no deployment targets needed); both environments are provisioned via
 * the SDK. Verified via the SDK that a deployment to the promote-target environment exists.
 */
class OctopusPromoteReleaseE2ETest {

  private static final String OCTOPUS_PROJECT = "PromoteReleaseIT";
  private static final String RELEASE_VERSION = "1.0.0";
  private static final String DEV = "Development";
  private static final String PROD = "Production";

  @Test
  void promoteReleaseStepUsingConnectionPromotesToTheNextEnvironment() throws Exception {
    try (final OctopusTeamCityStack stack = OctopusTeamCityStack.startWithAgentAndOctopus()) {
      final OctopusClient client =
          OctopusClientFactory.createClient(
              new ConnectData(
                  new URL(stack.octopusUrlForHost()),
                  stack.octopusApiKey(),
                  Duration.ofSeconds(20)));
      final SpaceHome spaceHome = new SpaceHomeApi(client).getDefault();

      OctopusProvisioning.createEnvironment(client, spaceHome, DEV);
      final String prodEnvironmentId =
          OctopusProvisioning.createEnvironment(client, spaceHome, PROD);
      OctopusProvisioning.createProjectWithServerScriptStep(
          client, spaceHome, stack.octopusUrlForHost(), stack.octopusApiKey(), OCTOPUS_PROJECT);

      // Provision TeamCity: project, connection, build type with create -> deploy(Dev) ->
      // promote(Dev -> Prod) steps.
      final TeamCityRest tc = stack.rest();
      tc.createProject("PromIT", "Promote IT");
      final String connectionId =
          tc.createOctopusConnection(
              "PromIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "3.0+",
              "");
      tc.createBuildType("PromIT_Promote", "Create, deploy and promote", "PromIT");
      tc.addCreateReleaseStepUsingConnection(
          "PromIT_Promote", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);
      tc.addDeployReleaseStepUsingConnection(
          "PromIT_Promote", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION, DEV);
      tc.addPromoteReleaseStepUsingConnection(
          "PromIT_Promote", connectionId, OCTOPUS_PROJECT, DEV, PROD);

      final String buildId = tc.triggerBuild("PromIT_Promote");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(7));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // The real side effect: the release was promoted — a deployment to Production exists.
      final List<Deployment> deployments = DeploymentApi.create(client, spaceHome).getAll();
      assertThat(deployments)
          .withFailMessage("No deployment to environment %s found", PROD)
          .anyMatch(d -> prodEnvironmentId.equals(d.getProperties().getEnvironmentId()));

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
