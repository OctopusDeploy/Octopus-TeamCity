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

  private static final String TC_PROJECT_ID = "RelLinkIT";
  private static final String TC_BUILD_TYPE_ID = "RelLinkIT_Create";

  private static final String RELEASE_URL_PARAMETER = "octopus.release.url";
  private static final String RELEASE_NUMBER_PARAMETER = "octopus.release.number";

  // Markers the follow-on step echoes the parameters under, so the assertions can find them in a
  // log that also carries the step's own mention of each value.
  private static final String READ_BACK_URL_PREFIX = "READ_BACK_URL=";
  private static final String READ_BACK_VERSION_PREFIX = "READ_BACK_VERSION=";

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
      tc.createProject(TC_PROJECT_ID, "Release link IT");
      final String connectionId =
          tc.createOctopusConnection(
              TC_PROJECT_ID,
              "IT Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType(TC_BUILD_TYPE_ID, "Create release", TC_PROJECT_ID);
      tc.setParameter(TC_BUILD_TYPE_ID, "env.OCTOPUS_NEW_CLI", "true");
      // Declared empty so a later step can reference them; the step fills them in as it runs.
      tc.setParameter(TC_BUILD_TYPE_ID, RELEASE_URL_PARAMETER, "");
      tc.setParameter(TC_BUILD_TYPE_ID, RELEASE_NUMBER_PARAMETER, "");
      final String createReleaseStepId =
          tc.addCreateReleaseStepUsingConnection(
              TC_BUILD_TYPE_ID, connectionId, OCTOPUS_PROJECT, RELEASE_VERSION);
      tc.addCommandLineStep(
          TC_BUILD_TYPE_ID,
          "Read the release back",
          "echo "
              + READ_BACK_URL_PREFIX
              + reference(RELEASE_URL_PARAMETER)
              + "\necho "
              + READ_BACK_VERSION_PREFIX
              + reference(RELEASE_NUMBER_PARAMETER));

      final String buildId = tc.triggerBuild(TC_BUILD_TYPE_ID);
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
          .withFailMessage("Build log did not name what it created. Log:\n%s", log)
          .contains("Created release " + RELEASE_VERSION + " of project " + OCTOPUS_PROJECT);
      assertThat(log)
          .withFailMessage("Later steps could not read the release back. Log:\n%s", log)
          .contains(READ_BACK_URL_PREFIX + expectedLink)
          .contains(READ_BACK_VERSION_PREFIX + RELEASE_VERSION);

      // The summary the build overview is rendered from (see OctopusReleaseLinkUiTest).
      final String hiddenArtifacts =
          tc.listHiddenBuildArtifacts(buildId, ReleaseSummary.ARTIFACT_DIRECTORY);
      assertThat(hiddenArtifacts)
          .withFailMessage("Release summary was not published. Artifacts:\n%s", hiddenArtifacts)
          .contains(ReleaseSummary.artifactNameFor(createReleaseStepId));

      assertThat(log).doesNotContain(stack.octopusApiKey());
    }
  }

  /** TeamCity's syntax for reading a build parameter from inside a step's script. */
  private static String reference(final String parameter) {
    return "%" + parameter + "%";
  }
}
