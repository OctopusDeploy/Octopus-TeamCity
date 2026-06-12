package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test that the shared {@code connectionSelector.jsp} fragment renders on <em>every</em>
 * connection-aware step's edit form — not just Create release (which {@link
 * OctopusConnectionUiTest} covers). Each form includes the same fragment and tags its own {@code
 * octopusInlineConnectionField} rows; a per-form regression here is exactly the class of bug that
 * previously slipped through (the fragment's empty {@code keys} bean). No Octopus or agent
 * required.
 */
class OctopusConnectionFormSmokeUiTest {

  @Test
  void connectionDropdownRendersOnEveryConnectionAwareForm() throws Exception {
    try (final OctopusTeamCityStack stack = OctopusTeamCityStack.startTeamCityOnly()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("FormIT", "Form IT");
      tc.createOctopusConnection(
          "FormIT",
          "Smoke Connection",
          "https://octopus.example.com",
          "API-EXAMPLEKEY0000000000000",
          "3.0+",
          "");
      tc.createBuildType("FormIT_Steps", "Steps", "FormIT");

      // One empty step per connection-aware runner; map display name -> runner id for diagnostics.
      final Map<String, String> runnerIdsByForm = new LinkedHashMap<>();
      runnerIdsByForm.put(
          "Deploy release", tc.addEmptyStep("FormIT_Steps", "octopus.deploy.release", "Deploy"));
      runnerIdsByForm.put(
          "Promote release", tc.addEmptyStep("FormIT_Steps", "octopus.promote.release", "Promote"));
      runnerIdsByForm.put(
          "Push package", tc.addEmptyStep("FormIT_Steps", "octopus.push.package", "Push"));
      runnerIdsByForm.put(
          "Build information", tc.addEmptyStep("FormIT_Steps", "octopus.metadata", "Build info"));

      try (final Playwright pw = Playwright.create()) {
        final Browser browser =
            pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        final Page page = browser.newPage();

        page.navigate(stack.tcBaseUrl() + "/login.html");
        page.fill("#username", stack.adminUsername());
        page.fill("#password", stack.adminPassword());
        page.click("input.loginButton, input[type=submit]");
        page.waitForLoadState(LoadState.NETWORKIDLE);

        for (final Map.Entry<String, String> form : runnerIdsByForm.entrySet()) {
          page.navigate(
              stack.tcBaseUrl()
                  + "/admin/editRunType.html?id=buildType:FormIT_Steps&runnerId="
                  + form.getValue(),
              new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
          page.waitForLoadState(LoadState.NETWORKIDLE);
          page.locator("#octopusConnectionId")
              .waitFor(new Locator.WaitForOptions().setTimeout(30000));

          assertThat(page.locator("#octopusConnectionId").count())
              .withFailMessage("Connection dropdown missing on the %s form", form.getKey())
              .isGreaterThan(0);
          assertThat(page.locator("#octopusConnectionId option").allInnerTexts())
              .withFailMessage("Connection not listed on the %s form", form.getKey())
              .anyMatch(t -> t.contains("Smoke Connection"));
        }

        browser.close();
      }
    }
  }
}
