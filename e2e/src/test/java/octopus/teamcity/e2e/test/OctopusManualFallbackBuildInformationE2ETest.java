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
 * Backward-compatibility end-to-end: a Build Information step configured the <em>old</em> way — an
 * inline Octopus URL + API key, with <em>no</em> connection — still publishes to Octopus. This is
 * the promise that the connection feature is purely additive; the connection e2e ({@link
 * OctopusConnectionBuildInformationE2ETest}) proves the new path, this proves the manual path is
 * untouched.
 */
class OctopusManualFallbackBuildInformationE2ETest {

  private static final String PACKAGE_ID = "manual.fallback.example";
  private static final String PACKAGE_VERSION = "1.0.0";

  @Test
  void buildInfoStepWithInlineUrlAndKeyPublishesToOctopus() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();

      final TeamCityRest tc = stack.rest();
      tc.createProject("ManualIT", "Manual IT");
      tc.createBuildType("ManualIT_BuildInfo", "Publish build info (manual)", "ManualIT");
      // No connection — inline URL + key, exactly as before the connection feature existed.
      tc.addBuildInfoStepInline(
          "ManualIT_BuildInfo",
          stack.octopusUrlForContainers(),
          stack.octopusApiKey(),
          PACKAGE_ID,
          PACKAGE_VERSION);

      final String buildId = tc.triggerBuild("ManualIT_BuildInfo");
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

      // The inline API key is masked in the build log.
      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
