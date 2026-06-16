package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.BuildInformationApi;
import com.octopus.sdk.domain.BuildInformation;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.space.SpaceHome;

import java.time.Duration;
import java.util.List;

import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * The headline of the connection feature: reuse. A connection is defined on a <em>parent</em>
 * project and consumed by a build config in a <em>child</em> project. This proves the connection
 * resolution walks the project ancestry (an inherited connection), not just the build config's own
 * project — exercised end-to-end against a real Octopus.
 */
class OctopusConnectionInheritanceE2ETest {

  private static final String PACKAGE_ID = "inherited.connection.example";
  private static final String PACKAGE_VERSION = "1.0.0";

  @Test
  void buildInChildProjectUsesConnectionInheritedFromParent() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();

      final TeamCityRest tc = stack.rest();
      // Connection lives on the PARENT project...
      tc.createProject("InhParent", "Inheritance parent");
      final String connectionId =
          tc.createOctopusConnection(
              "InhParent",
              "Parent Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "3.0+",
              "");
      // ...and is consumed by a build config in a CHILD project.
      tc.createProject("InhChild", "Inheritance child", "InhParent");
      tc.createBuildType("InhChild_BuildInfo", "Publish build info", "InhChild");
      tc.addBuildInfoStepUsingConnection(
          "InhChild_BuildInfo", connectionId, PACKAGE_ID, PACKAGE_VERSION);

      final String buildId = tc.triggerBuild("InhChild_BuildInfo");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(4));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      final SpaceHome spaceHome = stack.spaceHome(client);
      // The shared Octopus accumulates build information across tests, so scope to this package.
      final List<BuildInformation> items = BuildInformationApi.create(client, spaceHome).getAll();
      assertThat(items)
          .filteredOn(item -> PACKAGE_ID.equals(item.getProperties().getPackageId()))
          .hasSize(1);

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
