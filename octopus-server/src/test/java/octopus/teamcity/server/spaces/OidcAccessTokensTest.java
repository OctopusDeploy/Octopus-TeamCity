/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.server.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.client.OctopusTokenExchange;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.junit.jupiter.api.Test;

class OidcAccessTokensTest {

  private static final String SERVER_URL = "https://stored.example.com";

  private final OctopusConnectionsManager connectionsManager =
      mock(OctopusConnectionsManager.class);
  private final SProject project = mock(SProject.class);

  private final List<String> idTokenRequests = new ArrayList<>();
  private final List<String> exchangeUrls = new ArrayList<>();
  private final List<String> exchangeBodies = new ArrayList<>();
  private IOException idTokenFailure = null;

  private Map<String, String> connectionParams =
      Collections.singletonMap(ConnectionPropertyNames.OIDC_CONNECTION_ID, " oidc-1 ");

  private String accessToken(final SBuildType buildType) throws CredentialsUnavailableException {
    final OidcAccessTokens accessTokens =
        new OidcAccessTokens(
            connectionsManager,
            (tokenProject, oidcConnectionId, buildTypeId) -> {
              idTokenRequests.add(
                  oidcConnectionId
                      + " for "
                      + (buildTypeId == null ? "no build configuration" : buildTypeId));
              if (idTokenFailure != null) {
                throw idTokenFailure;
              }
              return "id.token.value";
            },
            new OctopusTokenExchange(
                (url, json) -> {
                  exchangeUrls.add(url);
                  exchangeBodies.add(json);
                  return "{\"access_token\":\"octopus-access\"}";
                }));
    return accessTokens.accessToken(connectionParams, project, buildType, SERVER_URL);
  }

  private String refusal() {
    try {
      accessToken(null);
    } catch (final CredentialsUnavailableException e) {
      return e.getMessage();
    }
    throw new AssertionError("Expected no access token");
  }

  private void oidcConnectorWith(final Map<String, String> params) {
    final OAuthConnectionDescriptor oidcConnector = mock(OAuthConnectionDescriptor.class);
    when(oidcConnector.getParameters()).thenReturn(params);
    when(connectionsManager.resolve(eq(project), eq("oidc-1")))
        .thenReturn(Optional.of(oidcConnector));
  }

  private void oidcConnectorWithAudience() {
    oidcConnectorWith(
        Collections.singletonMap(
            ConnectionPropertyNames.OIDC_CONNECTOR_AUDIENCE, "service-account-guid"));
  }

  @Test
  void exchangesAnIdTokenForTheBuildConfigurationForAnAccessToken() throws Exception {
    oidcConnectorWithAudience();
    final SBuildType buildType = mock(SBuildType.class);
    when(buildType.getExternalId()).thenReturn("MyProject_Deploy");

    assertThat(accessToken(buildType)).isEqualTo("octopus-access");
    assertThat(idTokenRequests).containsExactly("oidc-1 for MyProject_Deploy");
    assertThat(exchangeUrls).containsExactly(SERVER_URL + "/token/v1");
    assertThat(exchangeBodies.get(0)).contains("service-account-guid").contains("id.token.value");
  }

  @Test
  void requestsAProjectScopedIdTokenWithoutABuildConfiguration() throws Exception {
    oidcConnectorWithAudience();

    accessToken(null);

    assertThat(idTokenRequests).containsExactly("oidc-1 for no build configuration");
  }

  @Test
  void surfacesTheOidcPluginsRefusal() {
    oidcConnectorWithAudience();
    idTokenFailure =
        new IOException(
            "Enable the connection subject dimension on this OIDC connection to use it outside a"
                + " build.");

    assertThat(refusal()).contains("Enable the connection subject dimension");
    assertThat(exchangeUrls).isEmpty();
  }

  @Test
  void refusesAConnectionThatNamesNoOidcConnector() {
    connectionParams = Collections.emptyMap();

    assertThat(refusal()).contains("does not name an OIDC connection");
    assertThat(idTokenRequests).isEmpty();
  }

  @Test
  void reportsAnUnresolvableOidcConnector() {
    when(connectionsManager.resolve(eq(project), eq("oidc-1"))).thenReturn(Optional.empty());

    assertThat(refusal()).contains("'oidc-1' could not be resolved");
    assertThat(idTokenRequests).isEmpty();
  }

  @Test
  void refusesAnOidcConnectorWithoutAnAudience() {
    oidcConnectorWith(Collections.emptyMap());

    assertThat(refusal()).contains("'oidc-1' has no audience");
    assertThat(idTokenRequests).isEmpty();
  }
}
