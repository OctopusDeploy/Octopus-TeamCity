package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.PlaywrightUi;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * UI test for the space picker: the "Load spaces" button really reaches the Octopus container,
 * lists its spaces, and stores the chosen space's <em>id</em> rather than its name.
 *
 * <p>Storing the id is the whole point of the picker - a name breaks as soon as the space is
 * renamed in Octopus, whereas the id does not.
 */
class OctopusSpacePickerUiTest {

  @Test
  void loadsSpacesFromOctopusAndStoresTheSpaceId() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerIT", "Space picker IT");
      // A connection with a real URL and key, so the lookup can actually succeed. The TeamCity
      // server makes the call, so it needs the container-network URL.
      final String connectionId =
          tc.createOctopusConnection(
              "SpacePickerIT",
              "Real Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      assertThat(connectionId).isNotEmpty();
      tc.createBuildType("SpacePickerIT_Release", "Create release", "SpacePickerIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerIT_Release");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            page.navigate(
                stack.tcBaseUrl()
                    + "/admin/editRunType.html?id=buildType:SpacePickerIT_Release&runnerId="
                    + runnerId,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // The picker is rendered by the shared include.
            final Locator loadButton = page.locator("button.octopusLoadSpaces").first();
            loadButton.waitFor(new Locator.WaitForOptions().setTimeout(30000));
            // The props taglib renders name="prop:<property>", id="<property>".
            assertThat(page.locator("input[name='prop:octopus_space_id']").count())
                .isGreaterThan(0);

            // Pick the connection so the lookup uses its stored credentials rather than the
            // (empty) inline fields.
            page.selectOption("#octopusConnectionId", new String[] {connectionId});

            loadButton.click();

            // The select is only revealed once spaces come back from Octopus.
            final Locator select = page.locator("select.octopusSpaceSelect").first();
            select.waitFor(
                new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                    .setTimeout(60000));

            // A fresh Octopus has the Default space, and options are labelled "<name> (<id>)".
            assertThat(select.locator("option").allInnerTexts())
                .anyMatch(text -> text.contains("Default") && text.contains("Spaces-"));

            // Choosing it stores the id, and mirrors the readable name into the name field.
            final String firstSpaceId =
                select.locator("option[value^='Spaces-']").first().getAttribute("value");
            page.selectOption("select.octopusSpaceSelect", new String[] {firstSpaceId});

            assertThat(page.locator("input[name='prop:octopus_space_id']").first().inputValue())
                .isEqualTo(firstSpaceId);
            assertThat(page.locator("#octopus_space_name").first().inputValue())
                .isEqualTo("Default");
          });
    }
  }

  @Test
  void explainsWhySpacesCannotBeListedForAnOidcConnection() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerOidcIT", "Space picker OIDC IT");
      // An API key held in a build parameter is only resolved when a build runs, so the picker
      // cannot look spaces up at edit time - it must say so rather than fail opaquely.
      tc.createOctopusConnectionWithApiKeyParameter(
          "SpacePickerOidcIT",
          "Parameter Octopus",
          stack.octopusUrlForContainers(),
          "%octopus.apikey%",
          "");
      tc.createBuildType("SpacePickerOidcIT_Release", "Create release", "SpacePickerOidcIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerOidcIT_Release");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            page.navigate(
                stack.tcBaseUrl()
                    + "/admin/editRunType.html?id=buildType:SpacePickerOidcIT_Release&runnerId="
                    + runnerId,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            final Locator loadButton = page.locator("button.octopusLoadSpaces").first();
            loadButton.waitFor(new Locator.WaitForOptions().setTimeout(30000));
            page.selectOption(
                "#octopusConnectionId",
                new com.microsoft.playwright.options.SelectOption().setLabel("Parameter Octopus"));

            loadButton.click();

            final Locator status = page.locator("span.octopusSpaceStatus").first();
            status.waitFor(new Locator.WaitForOptions().setTimeout(30000));
            page.waitForCondition(() -> status.innerText().contains("build parameter"));
            assertThat(status.innerText()).contains("space name instead");
          });
    }
  }
}
