package octopus.teamcity.e2e.test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitUntilState;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Not a test - a driver for looking at the space picker by hand.
 *
 * <p>It stands the real stack up, provisions an Octopus with several spaces so the dropdown has
 * something to show, drives the forms in a <em>headed</em> browser, screenshots each interesting
 * state, and waits at each checkpoint so a human can click around before it moves on.
 *
 * <p>Gated on {@code OCTOPUS_PICKER_DEMO=1} so a normal {@code e2eTest} run never picks it up -
 * without that it would block forever waiting for a checkpoint file.
 *
 * <p>Resume a checkpoint by creating the file it names; the stack stays up throughout.
 */
class SpacePickerManualTest {

  /**
   * Where screenshots land. Defaults inside the build directory; override to put them elsewhere.
   */
  private static final Path OUT =
      Paths.get(
          System.getenv().getOrDefault("SPACE_PICKER_OUTPUT_DIR", "build/reports/space-picker"));

  /** Checkpoint files live with the screenshots, so a run leaves nothing outside the build dir. */
  private static final Path SIGNALS = OUT.resolve("checkpoints");

  /** {@code 1} pauses at each checkpoint; {@code auto} runs straight through. */
  private static final String MODE = System.getenv("OCTOPUS_PICKER_DEMO");

  private static boolean pausesEnabled() {
    return !"auto".equalsIgnoreCase(MODE);
  }

  private static final String PROJECT = "PickerDemo";
  private static final String BUILD_TYPE = "PickerDemo_Release";

  @Test
  void driveTheSpacePicker() throws Exception {
    // Skipped unless explicitly asked for. Without this the run would block forever on the first
    // checkpoint file, so it must never be picked up by an ordinary e2eTest run.
    assumeTrue(
        MODE != null && !MODE.isEmpty(),
        "set OCTOPUS_PICKER_DEMO=1 to drive the space picker by hand, or =auto to run it through");

    Files.createDirectories(OUT);
    Files.createDirectories(SIGNALS);

    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      // The free tier allows exactly one space ("You cannot create another space. This would
      // exceed the limits of your current license."), so the rename is demonstrated on the space
      // that exists: start it as the old name, and rename it under the configuration later.
      renameSpace(stack, "Spaces-1", "Modern Deployments");

      final TeamCityRest tc = stack.rest();
      tc.createProject(PROJECT, "Picker demo");
      final String connectionId =
          tc.createOctopusConnection(
              PROJECT, "Demo Octopus", stack.octopusUrlForContainers(), stack.octopusApiKey(), "");
      // A second connection whose key comes from a build parameter, to show the picker explaining
      // why it cannot list spaces for that one.
      tc.createOctopusConnectionWithApiKeyParameter(
          PROJECT, "Parameter Octopus", stack.octopusUrlForContainers(), "%octopus.apikey%", "");
      tc.createBuildType(BUILD_TYPE, "Create release", PROJECT);
      final String runnerId = tc.addCreateReleaseStep(BUILD_TYPE);

      banner(stack);

      try (Playwright playwright = Playwright.create()) {
        // Headed only when someone is actually watching; unattended runs go headless so they work
        // without a display.
        //
        // Note the deliberate absence of a viewport override when headed. Setting a viewport on a
        // headed browser applies a device-metrics override, which desyncs the rendered page from
        // the real OS window - and native <select> popups are positioned by the window, so they
        // open detached from the control. Sizing the window instead keeps them where they belong.
        final boolean headed = pausesEnabled();
        final Browser browser =
            playwright
                .chromium()
                .launch(
                    new BrowserType.LaunchOptions()
                        .setHeadless(!headed)
                        .setSlowMo(headed ? 400 : 0)
                        .setArgs(
                            Arrays.asList("--window-size=1600,1000", "--window-position=40,40")));
        try {
          final Page page =
              headed
                  ? browser.newPage(new Browser.NewPageOptions().setViewportSize(null))
                  : browser.newPage(
                      new Browser.NewPageOptions()
                          .setViewportSize(
                              new com.microsoft.playwright.options.ViewportSize(1600, 1000)));

          page.navigate(stack.tcBaseUrl() + "/login.html");
          page.fill("#username", stack.adminUsername());
          page.fill("#password", stack.adminPassword());
          page.click("input.loginButton, input[type=submit]");
          page.waitForLoadState(LoadState.NETWORKIDLE);

          // ---- 1. the step form, with the picker on it -------------------------------------
          page.navigate(
              stack.tcBaseUrl()
                  + "/admin/editRunType.html?id=buildType:"
                  + BUILD_TYPE
                  + "&runnerId="
                  + runnerId,
              new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
          page.waitForLoadState(LoadState.NETWORKIDLE);
          page.locator("button.octopusLoadSpaces")
              .first()
              .waitFor(new Locator.WaitForOptions().setTimeout(30000));
          shot(page, "01-step-form-picker");
          pause(1, "the step form with the Space picker (no connection selected yet)");

          // ---- 2. inline credentials: the picker says a saved connection is needed ---------
          page.locator("button.octopusLoadSpaces").first().click();
          page.waitForTimeout(1500);
          shot(page, "02-needs-saved-connection");
          pause(2, "Load spaces with NO connection selected - it asks you to save/select one");

          // ---- 3. pick the real connection and load the spaces ----------------------------
          page.selectOption("#octopusConnectionId", new String[] {connectionId});
          page.waitForTimeout(500);
          shot(page, "03-connection-selected");
          page.locator("button.octopusLoadSpaces").first().click();
          page.locator("select.octopusSpaceSelect")
              .first()
              .waitFor(
                  new Locator.WaitForOptions()
                      .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                      .setTimeout(60000));
          shot(page, "04-spaces-loaded");
          pause(3, "spaces loaded from the real Octopus - open the dropdown and look");

          // ---- 4. choose the space; the ID is what gets stored ---------------------------
          page.selectOption(
              "select.octopusSpaceSelect",
              new SelectOption().setLabel(labelFor(page, "Modern Deployments")));
          page.waitForTimeout(500);
          shot(page, "05-space-chosen-id-stored");
          System.out.println(
              "    stored octopus_space_id = "
                  + page.locator("input[name='prop:octopus_space_id']").first().inputValue()
                  + "   space name field = '"
                  + page.locator("#octopus_space_name").first().inputValue()
                  + "'");
          pause(4, "'Modern Deployments' chosen - note the stored space id under the field");

          // Persist the step so the stored id is what a build would actually use.
          page.click("input[name='submitButton'], input[value='Save']");
          page.waitForLoadState(LoadState.NETWORKIDLE);
          shot(page, "06-step-saved");
          pause(5, "step saved - the id is now in the build configuration");

          // ---- 5. rename the space in Octopus, exactly what broke us before ---------------
          renameSpace(stack, "Spaces-1", "Swordfish");
          System.out.println(
              "\n    *** the space is now called 'Swordfish' in Octopus."
                  + " The saved step still stores Spaces-1. ***\n");

          page.navigate(
              stack.tcBaseUrl()
                  + "/admin/editRunType.html?id=buildType:"
                  + BUILD_TYPE
                  + "&runnerId="
                  + runnerId,
              new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
          page.waitForLoadState(LoadState.NETWORKIDLE);
          page.locator("button.octopusLoadSpaces")
              .first()
              .waitFor(new Locator.WaitForOptions().setTimeout(30000));
          shot(page, "07-after-rename-reopened");
          System.out.println(
              "    still stored: octopus_space_id = "
                  + page.locator("input[name='prop:octopus_space_id']").first().inputValue()
                  + "   space name field = '"
                  + page.locator("#octopus_space_name").first().inputValue()
                  + "'  <- the NAME is now stale, the ID is not");
          pause(6, "reopened after the rename - stored id unchanged, stored name now stale");

          // Re-listing proves the id still points at the space, under its new name.
          page.locator("button.octopusLoadSpaces").first().click();
          page.locator("select.octopusSpaceSelect")
              .first()
              .waitFor(
                  new Locator.WaitForOptions()
                      .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                      .setTimeout(60000));
          shot(page, "08-reloaded-shows-new-name-same-id");
          System.out.println(
              "    dropdown now offers: "
                  + page.locator("select.octopusSpaceSelect option").allInnerTexts());
          pause(7, "the dropdown now shows 'Swordfish (Spaces-1)' - same id, new name");

          // ---- 6. the parameter-key connection explains itself ----------------------------
          page.selectOption(
              "#octopusConnectionId", new SelectOption().setLabel("Parameter Octopus"));
          page.waitForTimeout(500);
          page.locator("button.octopusLoadSpaces").first().click();
          page.waitForTimeout(2500);
          shot(page, "09-parameter-key-explained");
          pause(8, "a build-parameter API key cannot be resolved at edit time - it says so");

          // ---- 7. the connection form itself ----------------------------------------------
          page.navigate(
              stack.tcBaseUrl()
                  + "/admin/editProject.html?projectId="
                  + PROJECT
                  + "&tab=oauthConnections",
              new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
          page.waitForLoadState(LoadState.NETWORKIDLE);
          shot(page, "10-connections-tab");
          pause(
              9, "Connections tab - open 'Demo Octopus' to see the picker on the connection form");

          System.out.println("\n  Done. Screenshots in " + OUT);
        } finally {
          browser.close();
        }
      }
    }
  }

  /** The dropdown labels each option "<name> (<id>)"; find the one for this space name. */
  private static String labelFor(final Page page, final String spaceName) {
    for (final String label : page.locator("select.octopusSpaceSelect option").allInnerTexts()) {
      if (label.startsWith(spaceName + " (")) {
        return label;
      }
    }
    throw new IllegalStateException("No option for space '" + spaceName + "'");
  }

  /** Renames a space in Octopus, which is the thing that breaks name-based configuration. */
  private static void renameSpace(
      final OctopusTeamCityStack stack, final String spaceId, final String newName)
      throws Exception {
    final String url = stack.octopusUrlForHost() + "/api/spaces/" + spaceId;
    final String current = octopus(stack, "GET", url, null);
    final JsonObject space = JsonParser.parseString(current).getAsJsonObject();
    final String oldName = space.get("Name").getAsString();
    space.addProperty("Name", newName);
    octopus(stack, "PUT", url, space.toString());
    System.out.println("  renamed space " + spaceId + ": '" + oldName + "' -> '" + newName + "'");
  }

  private static String octopus(
      final OctopusTeamCityStack stack, final String method, final String url, final String body)
      throws Exception {
    final HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    c.setRequestMethod(method);
    c.setRequestProperty("X-Octopus-ApiKey", stack.octopusApiKey());
    c.setRequestProperty("Content-Type", "application/json");
    if (body != null) {
      c.setDoOutput(true);
      try (OutputStream out = c.getOutputStream()) {
        out.write(body.getBytes(StandardCharsets.UTF_8));
      }
    }
    final int status = c.getResponseCode();
    final java.io.InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
    final String response =
        in == null
            ? ""
            : new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))
                .lines()
                .reduce("", (a, b) -> a + b);
    c.disconnect();
    if (status >= 400) {
      throw new IllegalStateException(method + " " + url + " -> HTTP " + status + ": " + response);
    }
    return response;
  }

  private static void shot(final Page page, final String name) {
    final Path file = OUT.resolve(name + ".png");
    page.screenshot(new Page.ScreenshotOptions().setPath(file).setFullPage(true));
    System.out.println("  screenshot: " + file);
  }

  /** Holds everything open until the checkpoint's file appears; a no-op in {@code auto} mode. */
  private static void pause(final int checkpoint, final String what) throws Exception {
    if (!pausesEnabled()) {
      System.out.println("  --- checkpoint " + checkpoint + " (not pausing): " + what);
      return;
    }
    final Path signal = SIGNALS.resolve("continue-" + checkpoint);
    Files.deleteIfExists(signal);
    System.out.println("\n  >>> CHECKPOINT " + checkpoint + ": " + what);
    System.out.println("  >>> resume with:  touch " + signal.toAbsolutePath() + "\n");
    while (!Files.exists(signal)) {
      Thread.sleep(1000);
    }
    System.out.println("  ... continuing\n");
  }

  /** Deliberately does not print the API key. */
  private static void banner(final OctopusTeamCityStack stack) {
    System.out.println("\n==================== picker demo ====================");
    System.out.println("  TeamCity : " + stack.tcBaseUrl());
    System.out.println("  login    : " + stack.adminUsername() + " / " + stack.adminPassword());
    System.out.println("  Octopus  : " + stack.octopusUrlForHost());
    System.out.println("=====================================================\n");
  }
}
