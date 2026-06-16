package octopus.teamcity.e2e.dsl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;

/** Helpers for the Playwright-driven TeamCity UI tests. */
public final class PlaywrightUi {

  private PlaywrightUi() {}

  /** An action against a logged-in TeamCity page; may throw (assertions, Playwright calls). */
  public interface PageAction {
    void run(Page page) throws Exception;
  }

  /**
   * Launches headless Chromium, logs into TeamCity with the stack's admin account, runs {@code
   * action} against the resulting page, and always tears the browser down afterwards. Lets the UI
   * tests share the browser-launch and login boilerplate.
   */
  public static void withLoggedInPage(final OctopusTeamCityStack stack, final PageAction action)
      throws Exception {
    try (Playwright playwright = Playwright.create()) {
      final Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      try {
        final Page page = browser.newPage();
        // Log in via the TeamCity login form using the prepared datadir's admin account, and wait
        // for the post-login navigation to settle before handing the page to the test.
        page.navigate(stack.tcBaseUrl() + "/login.html");
        page.fill("#username", stack.adminUsername());
        page.fill("#password", stack.adminPassword());
        page.click("input.loginButton, input[type=submit]");
        page.waitForLoadState(LoadState.NETWORKIDLE);
        action.run(page);
      } finally {
        browser.close();
      }
    }
  }
}
