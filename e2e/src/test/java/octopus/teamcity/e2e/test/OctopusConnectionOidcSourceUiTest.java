package octopus.teamcity.e2e.test;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import octopus.teamcity.e2e.dsl.OctopusTeamCityStack;
import octopus.teamcity.e2e.dsl.PlaywrightUi;
import octopus.teamcity.e2e.dsl.SharedStack;
import octopus.teamcity.e2e.dsl.TeamCityRest;
import org.junit.jupiter.api.Test;

/**
 * Cross-plugin integration: proves our Octopus connection form discovers OIDC connectors provided
 * by the separate teamcity-oidc-plugin. With an {@code oidc-identity-token} connection present in
 * the project, the connection edit form must offer the "Use an OIDC token" source and list that
 * connector in the dropdown — driven by {@code OctopusOidcConnectorsUiData} reading the other
 * plugin's connections through the stable type string. This also exercises that the connection
 * dialog request carries the project id the accessor needs.
 */
class OctopusConnectionOidcSourceUiTest {

  @Test
  void octopusConnectionFormListsOidcConnectorsFromTheOidcPlugin() throws Exception {
    try (final OctopusTeamCityStack stack = SharedStack.full()) {
      final TeamCityRest tc = stack.rest();
      tc.createProject("OidcUiIT", "OIDC UI IT");
      tc.createOidcConnector("OidcUiIT", "IT OIDC Connector", "ServiceAccounts-123", "jwt.token");

      PlaywrightUi.withLoggedInPage(
          stack,
          page -> {
            page.navigate(
                stack.tcBaseUrl()
                    + "/admin/editProject.html?projectId=OidcUiIT&tab=oauthConnections");
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // Open the Add Connection dialog and choose the Octopus Deploy provider; this
            // AJAX-loads
            // editOctopusConnection.jsp into the dialog.
            page.click("a:has-text(\"Add Connection\")");
            page.locator("#typeSelector").waitFor(new Locator.WaitForOptions().setTimeout(30000));
            page.selectOption("#typeSelector", new SelectOption().setValue("OctopusConnection"));

            // The OIDC source option only renders when an OIDC connector is visible to the project,
            // so its presence proves the cross-plugin discovery worked.
            final Locator sourceSelect =
                page.locator("#OAuthConnectionDialog #octopusApiKeySource");
            sourceSelect.waitFor(new Locator.WaitForOptions().setTimeout(30000));
            assertThat(sourceSelect.locator("option").allInnerTexts())
                .withFailMessage(
                    "OIDC source option missing — the form did not discover the OIDC connector")
                .anyMatch(text -> text.contains("Use an OIDC token"));

            // Selecting it reveals the OIDC connector dropdown, which must list our connector.
            page.selectOption(
                "#OAuthConnectionDialog #octopusApiKeySource", new SelectOption().setValue("oidc"));
            final Locator connectorSelect =
                page.locator("#OAuthConnectionDialog #octopus_oidc_connection_id");
            connectorSelect.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            assertThat(connectorSelect.locator("option").allInnerTexts())
                .withFailMessage("OIDC connector not listed in the dropdown")
                .anyMatch(text -> text.contains("IT OIDC Connector"));
          });
    }
  }
}
