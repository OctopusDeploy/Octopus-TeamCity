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

import octopus.teamcity.common.ReleaseSummary;
import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * The Create release step run on the new CLI ends by pointing at what it made: the build log links
 * to the release, and later steps can read that link out of a build parameter.
 *
 * <p>The link is asserted against the release's own {@code Links.Web} as Octopus reports it, so the
 * test fails if the plugin ever invents an address of its own. The summary the build overview is
 * later rendered from has to be published too; {@link OctopusReleaseLinkUiTest} covers the page
 * itself.
 */
class OctopusCreateReleaseLinkE2ETest {

  private static final String OCTOPUS_PROJECT = "ReleaseLinkIT";
  private static final String RELEASE_VERSION = "1.0.0";

  @Test
  void createReleaseStepLinksToTheReleaseItCreated() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final OctopusClient client = stack.octopusClient();
      final SpaceHome spaceHome = stack.spaceHome(client);

      OctopusProvisioning.ensureProjectWithServerScriptStep(
          client,
          spaceHome,
          stack.octopusUrlForHost(),
          stack.octopusApiKey(),
          OCTOPUS_PROJECT,
          Collections.emptyList());

      final TeamCityRest tc = stack.rest();
      tc.createProject("RelLinkIT", "Release link IT");
      final String connectionId =
          tc.createOctopusConnection(
              "RelLinkIT",
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("RelLinkIT_Create", "Create release", "RelLinkIT");
      tc.setParameter("RelLinkIT_Create", "env.OCTOPUS_NEW_CLI", "true");
      // Declared empty so a later step can reference them; the step fills them in as it runs.
      tc.setParameter("RelLinkIT_Create", "octopus.release.url", "");
      tc.setParameter("RelLinkIT_Create", "octopus.release.number", "");
      final String createReleaseStepId =
          tc.addCreateReleaseStepUsingConnection(
              "RelLinkIT_Create", connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);
      tc.addCommandLineStep(
          "RelLinkIT_Create",
          "Read the release back",
          "echo READ_BACK_URL=%octopus.release.url%\necho READ_BACK_VERSION=%octopus.release.number%");

      final String buildId = tc.triggerBuild("RelLinkIT_Create");
      final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(5));
      final String log = tc.downloadBuildLog(buildId);

      assertThat(status)
          .withFailMessage("Build did not succeed. Log:\n%s", log)
          .isEqualTo("SUCCESS");

      // The suite shares one Octopus, where other projects hold releases of this version too.
      final Project project =
          ProjectApi.create(client, spaceHome)
              .getByName(OCTOPUS_PROJECT)
              .orElseThrow(() -> new AssertionError("Project " + OCTOPUS_PROJECT + " not found"));
      final String projectId = project.getProperties().getId();
      final List<Release> releases = ReleaseApi.create(client, spaceHome).getAll();
      final Release created =
          releases.stream()
              .filter(r -> projectId.equals(r.getProperties().getProjectId()))
              .filter(r -> RELEASE_VERSION.equals(r.getProperties().getVersion()))
              .findFirst()
              .orElseThrow(
                  () -> new AssertionError("Release " + RELEASE_VERSION + " not found in Octopus"));

      final String expectedLink =
          stack.octopusUrlForContainers() + created.getProperties().getWebLink();

      assertThat(log)
          .withFailMessage("Build log did not link to the release. Log:\n%s", log)
          .contains("View this release in Octopus Deploy: " + expectedLink);
      assertThat(log)
          .withFailMessage("Later steps could not read the release back. Log:\n%s", log)
          .contains("READ_BACK_URL=" + expectedLink)
          .contains("READ_BACK_VERSION=" + RELEASE_VERSION);

      // The summary the build overview is rendered from (see OctopusReleaseLinkUiTest).
      final String hiddenArtifacts =
          tc.listHiddenBuildArtifacts(buildId, ReleaseSummary.ARTIFACT_DIRECTORY);
      assertThat(hiddenArtifacts)
          .withFailMessage("Release summary was not published. Artifacts:\n%s", hiddenArtifacts)
          .contains(ReleaseSummary.artifactNameFor(createReleaseStepId));
      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }
}
