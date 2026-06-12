package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Proves {@code OctopusConnectionPropertiesProcessor} is actually wired into TeamCity's connection
 * save path — something the REST-based tests cannot show, since posting a projectFeature bypasses
 * the validator. Drives the real "Add Connection" dialog, submits an Octopus connection with no
 * Server URL, and asserts the server-side validation error is rendered (and the connection is not
 * saved).
 */
class OctopusConnectionValidationUiTest {

  @Test
  void savingAnOctopusConnectionWithNoUrlIsRejected() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("ValIT", "Validation IT");

      try (final Playwright pw = Playwright.create()) {
        final Browser browser =
            pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        final Page page = browser.newPage();

        page.navigate(stack.tcBaseUrl() + "/login.html");
        page.fill("#username", stack.adminUsername());
        page.fill("#password", stack.adminPassword());
        page.click("input.loginButton, input[type=submit]");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        page.navigate(
            stack.tcBaseUrl() + "/admin/editProject.html?projectId=ValIT&tab=oauthConnections");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Open the Add Connection dialog and choose the Octopus Deploy provider (value is the
        // provider type id). Selecting it AJAX-loads editOctopusConnection.jsp into the dialog.
        // The control is an anchor styled as a button (<a class="btn">Add Connection</a>).
        page.click("a:has-text(\"Add Connection\")");
        page.locator("#typeSelector").waitFor(new Locator.WaitForOptions().setTimeout(30000));
        page.selectOption("#typeSelector", new SelectOption().setValue("OctopusConnection"));

        // Wait for the Octopus form to render, then save it with an empty Server URL. TeamCity's
        // props taglib renders the field with name "prop:octopus_host" but id "octopus_host".
        page.locator("#OAuthConnectionDialog #octopus_host")
            .waitFor(new Locator.WaitForOptions().setTimeout(30000));
        page.fill("#OAuthConnectionDialog #octopus_host", "");
        page.locator("#OAuthConnectionDialog input.submitButton").first().click();

        // The server-side processor renders its error into the matching span and the dialog stays
        // open (the connection is not saved).
        final Locator urlError = page.locator("#error_octopus_host");
        urlError.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        assertThat(urlError.textContent().trim()).contains("Server URL must be specified");
        assertThat(page.locator("#OAuthConnectionDialog").isVisible()).isTrue();

        browser.close();
      }
    }
  }
}
