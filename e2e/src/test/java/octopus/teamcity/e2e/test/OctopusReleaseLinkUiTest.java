package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.octopus.sdk.api.ProjectApi;
import com.octopus.sdk.api.ReleaseApi;
import com.octopus.sdk.domain.Project;
import com.octopus.sdk.domain.Release;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.space.SpaceHome;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.microsoft.playwright.options.LoadState;
import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.PlaywrightUi;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * The server half of the release link: a build that created a release links to it. Only a browser
 * can prove this end of it — that the tab and summary extensions are registered, their JSP
 * resolves, and they find the summary the agent published as a hidden artifact.
 *
 * <p>Both surfaces are checked. The current UI renders a plugin's tab (inside its classic-UI
 * adapter) but has no slot for a fragment of the build summary, which only the classic build page
 * shows.
 */
class OctopusReleaseLinkUiTest {

  private static final String OCTOPUS_PROJECT = "ReleaseLinkUiIT";
  private static final String RELEASE_VERSION = "1.0.0";

  @Test
  void buildOverviewLinksToTheReleaseTheBuildCreated() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();
      final SpaceHome spaceHome = stack.spaceHome(client);

      OctopusProvisioning.createProjectWithServerScriptStep(
          client,
          spaceHome,
          stack.octopusUrlForHost(),
          stack.octopusApiKey(),
          OCTOPUS_PROJECT,
          Collections.emptyList());

      final TeamCityRest tc = stack.rest();
      tc.createProject("RelLinkUiIT", "Release link UI IT");
      final String connectionId =
          tc.createOctopusConnection(
              "RelLinkUiIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("RelLinkUiIT_Create", "Create release", "RelLinkUiIT");
      tc.setParameter("RelLinkUiIT_Create", "env.OCTOPUS_NEW_CLI", "true");
      tc.addCreateReleaseStepUsingConnection(
          "RelLinkUiIT_Create", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);

      final String buildId = tc.triggerBuild("RelLinkUiIT_Create");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(5));
      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", tc.downloadBuildLog(buildId))
          .isEqualTo("SUCCESS");

      final Project project =
          ProjectApi.create(client, spaceHome)
              .getByName(OCTOPUS_PROJECT)
              .orElseThrow(() -> new AssertionError("Project " + OCTOPUS_PROJECT + " not found"));
      final List<Release> releases = ReleaseApi.create(client, spaceHome).getAll();
      final Release created =
          releases.stream()
              .filter(r -> project.getProperties().getId().equals(r.getProperties().getProjectId()))
              .filter(r -> RELEASE_VERSION.equals(r.getProperties().getVersion()))
              .findFirst()
              .orElseThrow(
                  () -> new AssertionError("Release " + RELEASE_VERSION + " not found in Octopus"));
      final String expectedLink =
          stack.octopusUrlForContainers() + created.getProperties().getWebLink();

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            // The build page of the current UI: an "Octopus Deploy" tab holding the link.
            page.navigate(
                stack.tcBaseUrl()
                    + "/buildConfiguration/RelLinkUiIT_Create/"
                    + buildId
                    + "?buildTab=octopusRelease");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertThat(page.locator("[aria-label='Octopus Deploy']").count())
                .withFailMessage("Build page has no Octopus Deploy tab")
                .isPositive();
            assertThat(
                    page.frameLocator("iframe[title='Octopus Deploy']")
                        .locator("div.octopusRelease a")
                        .first()
                        .getAttribute("href"))
                .isEqualTo(expectedLink);

            // The classic build page, where the same link also sits in the build's summary.
            page.navigate(
                stack.tcBaseUrl()
                    + "/viewLog.html?tab=buildResultsDiv&buildId="
                    + buildId
                    + "&buildTypeId=RelLinkUiIT_Create&fromSakuraUI=true");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            assertThat(page.locator("div.octopusRelease a").first().getAttribute("href"))
                .isEqualTo(expectedLink);
          });
    }
  }
}
