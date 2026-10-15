package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.List;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import octopus.teamcity.e2e.dsl.OctopusProvisioning;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.PlaywrightUi;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * UI test for the space picker: the space name field suggests the connection's spaces, loaded from
 * the Octopus container on their own, and choosing one stores the space's <em>id</em> rather than
 * only its name. Anything else typed - such as a parameter reference - is kept as the name.
 *
 * <p>Storing the id is the whole point of the picker - a name breaks as soon as the space is
 * renamed in Octopus, whereas the id does not.
 */
class OctopusSpacePickerUiTest {

  private static final String CANNOT_LIST =
      "Unable to retrieve spaces, please enter space name manually";

  @Test
  void suggestsTheConnectionsSpacesAndStoresTheChosenId() throws Exception {
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
      tc.createBuildType("SpacePickerIT_Release", "Create release", "SpacePickerIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerIT_Release");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerIT_Release", runnerId);
            // No connection yet, so there is nothing to suggest.
            assertThat(status(page).innerText()).contains("Select a connection");
            assertNoSpacesListed(page);

            // Choosing the connection loads its spaces without any further click.
            page.selectOption("#octopusConnectionId", new String[] {connectionId});
            waitForSpaces(page);

            // A fresh Octopus has the Default space, suggested as "<name> (<id>)".
            assertThat(suggestions(page)).contains("Default (Spaces-1)");
            page.locator("ul.ui-autocomplete li", new Page.LocatorOptions().setHasText("Default"))
                .first()
                .click();

            assertThat(spaceId(page)).isEqualTo("Spaces-1");
            assertThat(spaceName(page).inputValue()).isEqualTo("Default");
          });
    }
  }

  @Test
  void keepsATypedNameAndStoresTheIdOnlyForAnExactMatch() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerTypedIT", "Space picker typed name IT");
      final String connectionId =
          tc.createOctopusConnection(
              "SpacePickerTypedIT",
              "Real Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("SpacePickerTypedIT_Release", "Create release", "SpacePickerTypedIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerTypedIT_Release");
      tc.setStepProperty(
          "SpacePickerTypedIT_Release", runnerId, "octopus_connection_id", connectionId);

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerTypedIT_Release", runnerId);
            waitForSpaces(page);

            // Typing narrows the suggestions to spaces whose name contains the text.
            spaceName(page).fill("");
            spaceName(page).pressSequentially("defa");
            assertThat(suggestions(page)).containsExactly("Default (Spaces-1)");

            // An exact name, in any case, picks the space.
            spaceName(page).fill("default");
            assertThat(spaceId(page)).isEqualTo("Spaces-1");

            // After a %, TeamCity's parameters are suggested instead, and choosing one completes
            // the reference.
            spaceName(page).fill("");
            spaceName(page).pressSequentially("%build.num");
            page.locator(
                    "ul.ui-autocomplete:visible li",
                    new Page.LocatorOptions().setHasText("build.number"))
                .first()
                .click();
            assertThat(spaceName(page).inputValue()).isEqualTo("%build.number%");
            assertThat(spaceId(page)).isEmpty();

            // A parameter reference is resolved when the build runs, so it is kept as the name and
            // not checked against Octopus.
            spaceName(page).fill("%octopus.space%");
            spaceName(page).blur();
            assertThat(spaceId(page)).isEmpty();
            assertThat(missingSpace(page).isVisible()).isFalse();

            // Any other name is kept as typed, and flagged.
            spaceName(page).fill("Nowhere");
            spaceName(page).blur();
            assertThat(spaceId(page)).isEmpty();
            assertThat(missingSpace(page).innerText())
                .isEqualTo("No space named 'Nowhere' was found in Octopus.");
          });
    }
  }

  /**
   * The parameter completion comes from TeamCity internals the picker calls into. If those change,
   * the field must still suggest spaces and accept whatever is typed.
   */
  @Test
  void stillSuggestsSpacesWhenTeamCitysParameterCompletionFails() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerBrokenIT", "Space picker broken completion IT");
      final String connectionId =
          tc.createOctopusConnection(
              "SpacePickerBrokenIT",
              "Real Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("SpacePickerBrokenIT_Release", "Create release", "SpacePickerBrokenIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerBrokenIT_Release");
      tc.setStepProperty(
          "SpacePickerBrokenIT_Release", runnerId, "octopus_connection_id", connectionId);

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            final List<String> pageErrors = new java.util.ArrayList<>();
            page.onPageError(pageErrors::add);
            openStep(page, stack, "SpacePickerBrokenIT_Release", runnerId);
            waitForSpaces(page);
            page.evaluate(
                "() => { BS.AvailableParams.sliceForCompletion = function () {"
                    + " throw new Error('changed in this TeamCity version'); };"
                    + " BS.AvailableParams.sourceForElement = undefined; }");

            spaceName(page).fill("");
            spaceName(page).pressSequentially("%octo");
            spaceName(page).fill("");
            spaceName(page).pressSequentially("Def");
            assertThat(suggestions(page)).containsExactly("Default (Spaces-1)");
            spaceName(page).fill("%octopus.space%");
            assertThat(spaceName(page).inputValue()).isEqualTo("%octopus.space%");
            assertThat(pageErrors).isEmpty();
          });
    }
  }

  @Test
  void flagsASavedSpaceThatOctopusDoesNotHave() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerMissingIT", "Space picker missing space IT");
      final String connectionId =
          tc.createOctopusConnection(
              "SpacePickerMissingIT",
              "Real Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("SpacePickerMissingIT_Release", "Create release", "SpacePickerMissingIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerMissingIT_Release");
      tc.setStepProperty(
          "SpacePickerMissingIT_Release", runnerId, "octopus_connection_id", connectionId);
      tc.setStepProperty(
          "SpacePickerMissingIT_Release", runnerId, "octopus_space_id", "Spaces-999");
      tc.setStepProperty("SpacePickerMissingIT_Release", runnerId, "octopus_space_name", "Gone");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerMissingIT_Release", runnerId);
            waitForSpaces(page);

            assertThat(missingSpace(page).innerText())
                .isEqualTo(
                    "The saved space 'Gone' (Spaces-999) was not found. Please enter the space"
                        + " name.");
            // The saved setting is left alone until someone changes it.
            assertThat(spaceId(page)).isEqualTo("Spaces-999");
            assertThat(spaceName(page).inputValue()).isEqualTo("Gone");
          });
    }
  }

  @Test
  void listsSpacesWithAnApiKeyHeldInAPasswordParameter() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerParamIT", "Space picker parameter IT");
      tc.createOctopusConnectionWithApiKeyParameter(
          "SpacePickerParamIT",
          "Parameter Octopus",
          stack.octopusUrlForContainers(),
          "%octopus.apikey%",
          "");
      tc.createBuildType("SpacePickerParamIT_Release", "Create release", "SpacePickerParamIT");
      // A password parameter, so this also shows the value is resolved, not its masked form.
      tc.setPasswordParameter(
          "SpacePickerParamIT_Release", "octopus.apikey", stack.octopusApiKey());
      final String runnerId = tc.addCreateReleaseStep("SpacePickerParamIT_Release");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerParamIT_Release", runnerId);
            selectConnection(page, "Parameter Octopus");
            waitForSpaces(page);

            assertThat(suggestions(page)).contains("Default (Spaces-1)");
          });
    }
  }

  @Test
  void switchingConnectionDropsTheSavedSpaceId() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerSwitchIT", "Space picker connection switch IT");
      final String connectionId =
          tc.createOctopusConnection(
              "SpacePickerSwitchIT",
              "Real Octopus",
              stack.octopusUrlForContainers(),
              stack.octopusApiKey(),
              "");
      tc.createBuildType("SpacePickerSwitchIT_Release", "Create release", "SpacePickerSwitchIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerSwitchIT_Release");
      tc.setStepProperty(
          "SpacePickerSwitchIT_Release", runnerId, "octopus_connection_id", connectionId);
      tc.setStepProperty("SpacePickerSwitchIT_Release", runnerId, "octopus_space_id", "Spaces-1");
      tc.setStepProperty("SpacePickerSwitchIT_Release", runnerId, "octopus_space_name", "Default");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerSwitchIT_Release", runnerId);
            waitForSpaces(page);
            assertThat(spaceId(page)).isEqualTo("Spaces-1");

            page.selectOption("#octopusConnectionId", new String[] {""});

            assertThat(spaceId(page)).isEmpty();
            assertThat(spaceName(page).inputValue()).isEqualTo("Default");
          });
    }
  }

  @Test
  void connectionFormSuggestsTheConnectionsOwnSpaces() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerConnFormIT", "Space picker connection form IT");
      tc.createOctopusConnection(
          "SpacePickerConnFormIT",
          "Picker Octopus",
          stack.octopusUrlForContainers(),
          stack.octopusApiKey(),
          "");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            page.navigate(
                stack.tcBaseUrl()
                    + "/admin/editProject.html?projectId=SpacePickerConnFormIT"
                    + "&tab=oauthConnections");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            page.locator("tr", new Page.LocatorOptions().setHasText("Picker Octopus"))
                .locator("a:has-text(\"Edit\")")
                .first()
                .click();

            waitForSpaces(page);
            assertThat(suggestions(page)).contains("Default (Spaces-1)");
          });
    }
  }

  @Test
  void explainsAnApiKeyParameterThatOnlyResolvesDuringABuild() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerUnsetIT", "Space picker unset parameter IT");
      tc.createOctopusConnectionWithApiKeyParameter(
          "SpacePickerUnsetIT",
          "Unset parameter Octopus",
          stack.octopusUrlForContainers(),
          "%octopus.apikey.unset%",
          "");
      tc.createBuildType("SpacePickerUnsetIT_Release", "Create release", "SpacePickerUnsetIT");
      final String runnerId = tc.addCreateReleaseStep("SpacePickerUnsetIT_Release");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerUnsetIT_Release", runnerId);
            selectConnection(page, "Unset parameter Octopus");

            page.waitForCondition(() -> reason(page).contains("can't be resolved here"));
            assertThat(status(page).innerText()).contains(CANNOT_LIST);
            assertThat(reason(page))
                .contains("%octopus.apikey.unset%")
                .contains("space name instead");
            assertNoSpacesListed(page);

            // The reason is shown in a tooltip from the help icon.
            page.locator("span.octopusSpaceDetails").first().hover();
            final Locator tooltip = page.locator("div[id^='messagePopup_']:visible").first();
            tooltip.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            assertThat(tooltip.innerText()).contains("%octopus.apikey.unset%");
          });
    }
  }

  /**
   * The spaces are listed with an OIDC token Octopus exchanges for an access token. Octopus trusts
   * tokens for one build configuration only, so the step in the other one is refused, and the
   * picker says why.
   */
  @Test
  void listsSpacesWhenOctopusTrustsTheOidcTokenAndExplainsWhenItDoesNot() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("SpacePickerOidcIT", "Space picker OIDC IT");
      tc.createBuildType("SpacePickerOidcIT_Trusted", "Trusted", "SpacePickerOidcIT");
      tc.createBuildType("SpacePickerOidcIT_Untrusted", "Untrusted", "SpacePickerOidcIT");
      final String trustedRunnerId = tc.addCreateReleaseStep("SpacePickerOidcIT_Trusted");
      final String untrustedRunnerId = tc.addCreateReleaseStep("SpacePickerOidcIT_Untrusted");

      final String audience =
          OctopusProvisioning.createOidcServiceAccount(
              stack.octopusUrlForHost(),
              stack.octopusApiKey(),
              "space-picker-oidc-it",
              OctopusTeamCityStack.ROOT_URL,
              tc.oidcSubject("SpacePickerOidcIT", "SpacePickerOidcIT_Trusted"));
      final String connectorId =
          tc.createOidcConnector("SpacePickerOidcIT", "IT OIDC Connector", audience, "jwt.token");
      tc.createOctopusConnectionWithOidc(
          "SpacePickerOidcIT", "OIDC Octopus", stack.octopusUrlForContainers(), connectorId, "");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            openStep(page, stack, "SpacePickerOidcIT_Trusted", trustedRunnerId);
            selectConnection(page, "OIDC Octopus");
            final Locator status = status(page);
            page.waitForCondition(() -> !status.innerText().startsWith("Loading"));
            assumeFalse(
                reason(page).contains("Update the OIDC Identity Token plugin"),
                "the installed OIDC plugin cannot issue tokens outside a build; set"
                    + " OIDC_PLUGIN_ZIP to one that can");
            waitForSpaces(page);
            assertThat(suggestions(page)).anyMatch(label -> label.endsWith("(Spaces-1)"));

            openStep(page, stack, "SpacePickerOidcIT_Untrusted", untrustedRunnerId);
            selectConnection(page, "OIDC Octopus");
            page.waitForCondition(() -> reason(page).contains("did not accept"));
            assertThat(status.innerText()).contains(CANNOT_LIST);
            assertThat(reason(page)).contains("Octopus did not accept the OIDC token");
            assertNoSpacesListed(page);
          });
    }
  }

  private static void openStep(
      final Page page,
      final OctopusTeamCityStack stack,
      final String buildTypeId,
      final String runnerId) {
    page.navigate(
        stack.tcBaseUrl()
            + "/admin/editRunType.html?id=buildType:"
            + buildTypeId
            + "&runnerId="
            + runnerId,
        new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    page.waitForLoadState(LoadState.NETWORKIDLE);
  }

  private static void selectConnection(final Page page, final String connectionName) {
    page.selectOption("#octopusConnectionId", new SelectOption().setLabel(connectionName));
  }

  /** Why the spaces could not be listed, as the help icon's tooltip shows it. */
  private static String reason(final Page page) {
    final String reason =
        page.locator("span.octopusSpaceDetails").first().getAttribute("data-reason");
    return reason == null ? "" : reason;
  }

  private static Locator status(final Page page) {
    return page.locator("span.octopusSpaceStatus").first();
  }

  private static Locator spaceName(final Page page) {
    return page.locator("#octopus_space_name").first();
  }

  private static String spaceId(final Page page) {
    return page.locator("input[name='prop:octopus_space_id']").first().inputValue();
  }

  private static Locator missingSpace(final Page page) {
    return page.locator("span.octopusSpaceMissing").first();
  }

  /** The by-id note is shown exactly when the connection's spaces have been listed. */
  private static void waitForSpaces(final Page page) {
    page.locator("span.octopusSpaceByIdNote")
        .first()
        .waitFor(
            new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(60000));
  }

  private static void assertNoSpacesListed(final Page page) {
    assertThat(spaceName(page).isVisible()).isTrue();
    assertThat(page.locator("span.octopusSpaceByIdNote").first().isVisible()).isFalse();
  }

  /** What the space name field suggests for what it currently holds. */
  private static List<String> suggestions(final Page page) {
    spaceName(page).focus();
    final Locator items = page.locator("ul.ui-autocomplete:visible li");
    items.first().waitFor(new Locator.WaitForOptions().setTimeout(10000));
    return items.allInnerTexts();
  }
}
