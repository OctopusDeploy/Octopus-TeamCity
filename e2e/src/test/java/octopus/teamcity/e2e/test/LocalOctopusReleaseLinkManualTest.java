package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.octopus.sdk.api.ReleaseApi;
import com.octopus.sdk.api.SpaceHomeApi;
import com.octopus.sdk.domain.Release;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.model.space.SpaceHome;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.microsoft.playwright.options.LoadState;
import octopus.teamcity.common.ReleaseSummary;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.PlaywrightUi;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Drives the release link against a developer's own Octopus rather than the containerised free-tier
 * one: a real project, a real space that is not the default, and a server built from recent code.
 * Run under a key-injecting wrapper so the API key never appears on a command line.
 */
class LocalOctopusReleaseLinkManualTest {

  private static final String OCTOPUS_FROM_CONTAINERS = "http://host.docker.internal:8065";
  private static final String OCTOPUS_FROM_HOST = "http://localhost:8065";
  private static final String SPACE_NAME = "TeamCity";
  private static final String SPACE_ID = "Spaces-22";
  private static final String OCTOPUS_PROJECT = "TC-Plugin-Timeout-Test";

  private static String apiKey;
  private static OctopusTeamCityStack stack;
  private static TeamCityRest tc;
  private static String connectionBySpaceName;
  private static String connectionBySpaceId;
  private static final List<String> createdReleaseIds = new ArrayList<>();

  @BeforeAll
  static void startStack() throws Exception {
    apiKey = System.getenv("OCTOPUS_API_KEY");
    assumeTrue(apiKey != null && !apiKey.isEmpty(), "needs OCTOPUS_API_KEY for the local Octopus");

    stack = OctopusTeamCityStack.startWithAgent();
    tc = stack.rest();
    tc.createProject("LocalLink", "Local link IT");
    connectionBySpaceName =
        tc.createOctopusConnection(
            "LocalLink",
            "Local Octopus by space name",
            OCTOPUS_FROM_CONTAINERS,
            apiKey,
            SPACE_NAME);
    connectionBySpaceId =
        tc.createOctopusConnection(
            "LocalLink", "Local Octopus by space id", OCTOPUS_FROM_CONTAINERS, apiKey, SPACE_ID);
  }

  @AfterAll
  static void removeCreatedReleases() throws Exception {
    if (apiKey == null) {
      return;
    }
    final ReleaseApi releases = ReleaseApi.create(client(), spaceHome());
    for (final String releaseId : createdReleaseIds) {
      releases.delete(releaseId);
    }
    System.out.println("Deleted releases: " + createdReleaseIds);
  }

  private static OctopusClient client() throws Exception {
    return OctopusClientFactory.createClient(
        new ConnectData(new URL(OCTOPUS_FROM_HOST), apiKey, Duration.ofSeconds(30)));
  }

  private static SpaceHome spaceHome() throws Exception {
    return new SpaceHomeApi(client()).getByName(SPACE_NAME);
  }

  private static String uniqueVersion() {
    return "9.9." + (System.currentTimeMillis() % 10_000_000);
  }

  /** The release of the given version in the local space, remembered so it can be cleaned up. */
  private static Release release(final String version) throws Exception {
    final Optional<Release> found =
        ReleaseApi.create(client(), spaceHome()).getAll().stream()
            .filter(r -> version.equals(r.getProperties().getVersion()))
            .findFirst();
    assertThat(found).withFailMessage("Release %s not found in Octopus", version).isPresent();
    createdReleaseIds.add(found.get().getProperties().getId());
    return found.get();
  }

  private static String expectedLink(final Release release) {
    return OCTOPUS_FROM_CONTAINERS + release.getProperties().getWebLink();
  }

  /** Runs a build and returns its log, failing with the log if it did not succeed. */
  private static String runBuild(final String buildTypeId) throws Exception {
    final String buildId = tc.triggerBuild(buildTypeId);
    final String status = tc.waitForBuildFinished(buildId, Duration.ofMinutes(5));
    final String log = tc.downloadBuildLog(buildId);
    assertThat(status).withFailMessage("Build did not succeed. Log:%n%s", log).isEqualTo("SUCCESS");
    lastBuildId = buildId;
    return log;
  }

  private static String lastBuildId;

  /** How many release summaries a build published; a build that published none has no directory. */
  private static int summaryCount(final String buildId) throws Exception {
    final String listing;
    try {
      listing = tc.listHiddenBuildArtifacts(buildId, ReleaseSummary.ARTIFACT_DIRECTORY);
    } catch (final IllegalStateException notFound) {
      assertThat(notFound).hasMessageContaining("404");
      return 0;
    }
    // Each file appears three times in the listing (name, metadata href, content href).
    return listing.split("\"name\":\"release-", -1).length - 1;
  }

  private static String buildTypeWithCreateRelease(
      final String id, final String connectionId, final String version, final boolean newCli)
      throws Exception {
    tc.createBuildType(id, id, "LocalLink");
    if (newCli) {
      tc.setParameter(id, "env.OCTOPUS_NEW_CLI", "true");
    }
    tc.addCreateReleaseStepUsingConnection(id, connectionId, OCTOPUS_PROJECT, version);
    return id;
  }

  @Test
  void linksToTheReleaseWhenTheConnectionNamesTheSpace() throws Exception {
    final String version = uniqueVersion();
    final String buildType =
        buildTypeWithCreateRelease("LocalLink_ByName", connectionBySpaceName, version, true);
    tc.setParameter(buildType, "octopus.release.url", "");
    tc.setParameter(buildType, "octopus.release.number", "");
    tc.addCommandLineStep(
        buildType,
        "Read the release back",
        "echo READ_BACK_URL=%octopus.release.url%\necho READ_BACK_VERSION=%octopus.release.number%");

    final String log = runBuild(buildType);
    final Release created = release(version);

    assertThat(log).contains("View this release in Octopus Deploy: " + expectedLink(created));
    assertThat(log).contains("READ_BACK_URL=" + expectedLink(created));
    assertThat(log).contains("READ_BACK_VERSION=" + version);
    assertThat(log).doesNotContain(apiKey);
    assertThat(created.getProperties().getSpaceId()).isEqualTo(SPACE_ID);

    assertThat(summaryCount(lastBuildId)).isEqualTo(1);

    assertTabLinksTo(buildType, lastBuildId, expectedLink(created));
  }

  @Test
  void skipsTheSpaceLookupWhenTheConnectionAlreadyGivesTheSpaceId() throws Exception {
    final String version = uniqueVersion();
    final String buildType =
        buildTypeWithCreateRelease("LocalLink_ById", connectionBySpaceId, version, true);

    final String log = runBuild(buildType);
    final Release created = release(version);

    assertThat(log).contains("View this release in Octopus Deploy: " + expectedLink(created));
    assertThat(log).doesNotContain("space view");
  }

  @Test
  void linksToEveryReleaseABuildCreates() throws Exception {
    final String first = uniqueVersion();
    final String second = "8.8." + (System.currentTimeMillis() % 10_000_000);
    final String buildType =
        buildTypeWithCreateRelease("LocalLink_Two", connectionBySpaceName, first, true);
    tc.addCreateReleaseStepUsingConnection(
        buildType, connectionBySpaceName, OCTOPUS_PROJECT, second);

    runBuild(buildType);
    final Release firstRelease = release(first);
    final Release secondRelease = release(second);

    assertThat(summaryCount(lastBuildId)).isEqualTo(2);

    final String buildId = lastBuildId;
    PlaywrightUi.withLoggedInPage(
        stack,
        page -> {
          page.navigate(
              stack.tcBaseUrl()
                  + "/buildConfiguration/"
                  + buildType
                  + "/"
                  + buildId
                  + "?buildTab=octopusRelease");
          page.waitForLoadState(LoadState.NETWORKIDLE);
          final List<String> hrefs =
              page.frameLocator("iframe[title='Octopus Deploy']")
                  .locator("div.octopusRelease a")
                  .allTextContents();
          System.out.println("Tab shows: " + hrefs);
          assertThat(hrefs).hasSize(2);
        });

    assertThat(expectedLink(firstRelease)).isNotEqualTo(expectedLink(secondRelease));
  }

  @Test
  void legacyCliCreatesTheReleaseWithoutLinkingToIt() throws Exception {
    final String version = uniqueVersion();
    final String buildType =
        buildTypeWithCreateRelease("LocalLink_Legacy", connectionBySpaceName, version, false);

    final String log = runBuild(buildType);
    release(version);

    assertThat(log).contains("create-release");
    assertThat(log).doesNotContain("View this release in Octopus Deploy");
    assertThat(summaryCount(lastBuildId)).isZero();
  }

  @Test
  void aBuildThatCreatedNoReleaseHasNoOctopusTab() throws Exception {
    tc.createBuildType("LocalLink_NoOctopus", "No Octopus step", "LocalLink");
    tc.addCommandLineStep("LocalLink_NoOctopus", "Say hello", "echo hello");

    runBuild("LocalLink_NoOctopus");
    final String buildId = lastBuildId;

    PlaywrightUi.withLoggedInPage(
        stack,
        page -> {
          page.navigate(stack.tcBaseUrl() + "/buildConfiguration/LocalLink_NoOctopus/" + buildId);
          page.waitForLoadState(LoadState.NETWORKIDLE);
          assertThat(page.locator("[aria-label='Octopus Deploy']").count()).isZero();
        });
  }

  private static void assertTabLinksTo(
      final String buildTypeId, final String buildId, final String link) throws Exception {
    PlaywrightUi.withLoggedInPage(
        stack,
        page -> {
          page.navigate(
              stack.tcBaseUrl()
                  + "/buildConfiguration/"
                  + buildTypeId
                  + "/"
                  + buildId
                  + "?buildTab=octopusRelease");
          page.waitForLoadState(LoadState.NETWORKIDLE);
          assertThat(page.locator("[aria-label='Octopus Deploy']").count()).isPositive();
          assertThat(
                  page.frameLocator("iframe[title='Octopus Deploy']")
                      .locator("div.octopusRelease a")
                      .first()
                      .getAttribute("href"))
              .isEqualTo(link);
        });
  }
}
