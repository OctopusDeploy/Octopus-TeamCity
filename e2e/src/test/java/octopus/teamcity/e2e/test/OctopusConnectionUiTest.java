package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitUntilState;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * UI test: the connection dropdown rendered by the controller/JSP appears on a step's edit page,
 * and selecting a connection hides the manual fields. No Octopus or agent required.
 */
class OctopusConnectionUiTest {

  @Test
  void connectionDropdownRendersAndTogglesManualFields() throws Exception {
    try (final OctopusTeamCityStack stack = OctopusTeamCityStack.startTeamCityOnly()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("UiIT", "UI IT");
      tc.createOctopusConnection(
          "UiIT",
          "My Octopus Connection",
          "https://octopus.example.com",
          "API-EXAMPLEKEY0000000000000",
          "3.0+",
          "Default");
      tc.createBuildType("UiIT_Release", "Create release", "UiIT");
      final String runnerId = tc.addCreateReleaseStep("UiIT_Release");

      try (final Playwright pw = Playwright.create()) {
        final Browser browser =
            pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        final Page page = browser.newPage();

        // Log in via the TeamCity login form using the prepared datadir's admin account, and wait
        // for the post-login navigation to settle before moving on.
        page.navigate(stack.tcBaseUrl() + "/login.html");
        page.fill("#username", stack.adminUsername());
        page.fill("#password", stack.adminPassword());
        page.click("input.loginButton, input[type=submit]");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Open the edit page for the step (use the real runner id returned by REST).
        page.navigate(
            stack.tcBaseUrl()
                + "/admin/editRunType.html?id=buildType:UiIT_Release&runnerId="
                + runnerId,
            new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

        // TeamCity lazy-loads the runner-parameters form (our controller-served JSP) via AJAX after
        // the page loads, so wait for it to settle and for our dropdown to appear before asserting.
        page.waitForLoadState(LoadState.NETWORKIDLE);
        page.locator("#octopusConnectionId")
            .waitFor(new Locator.WaitForOptions().setTimeout(30000));

        // The connection dropdown is present and lists our connection.
        assertThat(page.locator("#octopusConnectionId").count()).isGreaterThan(0);
        assertThat(page.locator("#octopusConnectionId option").allInnerTexts())
            .anyMatch(t -> t.contains("My Octopus Connection"));

        // Selecting the connection hides the manual fields (tr.octopusInlineConnectionField ->
        // hidden).
        page.selectOption(
            "#octopusConnectionId", new SelectOption().setLabel("My Octopus Connection"));
        assertThat(page.locator("tr.octopusInlineConnectionField").first().isHidden()).isTrue();

        // The selected connection defines a space ("Default"), so the step's Space field is hidden
        // — the connection's space is used.
        assertThat(page.locator("#octopus_space_name").isHidden()).isTrue();

        // Create-release Git Ref/Commit rows are driven by the *connection's* version: 3.0+
        // supports
        // Git projects, so the rows become visible once the connection is selected.
        assertThat(page.locator("#gitRefRow").isVisible()).isTrue();
        assertThat(page.locator("#gitCommitRow").isVisible()).isTrue();

        browser.close();
      }
    }
  }
}
