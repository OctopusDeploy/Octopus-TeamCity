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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.parameters.ProcessingResult;
import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.users.SUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.client.OctopusTokenExchange;
import octopus.teamcity.server.connection.OctopusConnection;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpacesLookupCredentialsTest {

  private static final String PROJECT_INTERNAL_ID = "project123";
  private static final String CONNECTION_ID = "octopus-conn-1";

  private final OctopusConnectionsManager connectionsManager =
      mock(OctopusConnectionsManager.class);
  private final ProjectManager projectManager = mock(ProjectManager.class);
  private final SUser user = mock(SUser.class);
  private final SProject project = mock(SProject.class);
  private final List<String> idTokenRequests = new ArrayList<>();

  private String buildTypeId = null;

  @BeforeEach
  void setUp() {
    when(project.getProjectId()).thenReturn(PROJECT_INTERNAL_ID);
    when(user.isPermissionGrantedForProject(PROJECT_INTERNAL_ID, Permission.RUN_BUILD))
        .thenReturn(true);
  }

  private Credentials resolve() throws CredentialsUnavailableException {
    final OidcAccessTokens oidcAccessTokens =
        new OidcAccessTokens(
            connectionsManager,
            (tokenProject, oidcConnectionId, requestedBuildTypeId) -> {
              idTokenRequests.add(oidcConnectionId + " for " + requestedBuildTypeId);
              return "id.token.value";
            },
            new OctopusTokenExchange((url, json) -> "{\"access_token\":\"octopus-access\"}"));
    return new SpacesLookupCredentials(connectionsManager, projectManager, oidcAccessTokens)
        .resolve(project, user, CONNECTION_ID, buildTypeId);
  }

  private String refusal() {
    try {
      resolve();
    } catch (final CredentialsUnavailableException e) {
      return e.getMessage();
    }
    throw new AssertionError("Expected the credentials to be unavailable");
  }

  private void connection(final Map<String, String> params) {
    connection(params, OctopusConnection.TYPE);
  }

  private void connection(final Map<String, String> params, final String providerType) {
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    final OAuthConnectionDescriptor connection = mock(OAuthConnectionDescriptor.class);
    when(connection.getParameters()).thenReturn(params);
    final OAuthProvider provider = mock(OAuthProvider.class);
    when(provider.getType()).thenReturn(providerType);
    when(connection.getOauthProvider()).thenReturn(provider);
    when(connectionsManager.resolve(eq(project), eq(CONNECTION_ID)))
        .thenReturn(Optional.of(connection));
  }

  private void parameterSourcedConnection() {
    final Map<String, String> params = new HashMap<>();
    params.put(
        ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_PARAMETER);
    params.put(ConnectionPropertyNames.API_KEY_PARAMETER, " %octopus.apikey% ");
    connection(params);
  }

  private void oidcSourcedConnection() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_OIDC);
    params.put(ConnectionPropertyNames.OIDC_CONNECTION_ID, "oidc-1");
    connection(params);
    final OAuthConnectionDescriptor oidcConnector = mock(OAuthConnectionDescriptor.class);
    when(oidcConnector.getParameters())
        .thenReturn(
            Collections.singletonMap(
                ConnectionPropertyNames.OIDC_CONNECTOR_AUDIENCE, "service-account-guid"));
    when(connectionsManager.resolve(eq(project), eq("oidc-1")))
        .thenReturn(Optional.of(oidcConnector));
  }

  private static ValueResolver resolverReturning(final String value, final boolean resolved) {
    final ProcessingResult result = mock(ProcessingResult.class);
    when(result.getResult()).thenReturn(value);
    when(result.isFullyResolved()).thenReturn(resolved);
    final ValueResolver resolver = mock(ValueResolver.class);
    when(resolver.resolve("%octopus.apikey%")).thenReturn(result);
    return resolver;
  }

  private SBuildType buildTypeInProject(final String projectInternalId) {
    final SBuildType buildType = mock(SBuildType.class);
    when(buildType.getProjectId()).thenReturn(projectInternalId);
    when(buildType.getExternalId()).thenReturn("MyProject_Deploy");
    when(projectManager.findBuildTypeByExternalId("MyProject_Deploy")).thenReturn(buildType);
    buildTypeId = "MyProject_Deploy";
    return buildType;
  }

  @Test
  void usesTheStoredKeyOfAConnectionThatHoldsOne() throws Exception {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    connection(params);

    final Credentials credentials = resolve();

    assertThat(credentials.serverUrl).isEqualTo("https://stored.example.com");
    assertThat(credentials.auth.value()).isEqualTo("API-STORED");
  }

  @Test
  void refusesWithoutASavedConnection() {
    // The request must never supply the address to fetch: the only URLs this endpoint will call are
    // ones an admin already persisted into project configuration.
    assertThatThrownBy(
            () ->
                new SpacesLookupCredentials(connectionsManager, projectManager, null)
                    .resolve(project, user, " ", null))
        .hasMessageContaining("saved Octopus connection");
  }

  @Test
  void reportsAnUnresolvableConnection() {
    when(connectionsManager.resolve(eq(project), any())).thenReturn(Optional.empty());

    assertThat(refusal()).contains("could not be resolved");
  }

  @Test
  void refusesAConnectionThatIsNotAnOctopusConnection() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    connection(params, "GitHubApp");

    assertThat(refusal()).contains("could not be resolved");
  }

  @Test
  void resolvesAParameterSourcedKeyAgainstTheBuildConfiguration() throws Exception {
    parameterSourcedConnection();
    final SBuildType buildType = buildTypeInProject(PROJECT_INTERNAL_ID);
    final ValueResolver resolver = resolverReturning("API-FROM-PARAMETER", true);
    when(buildType.getValueResolver()).thenReturn(resolver);

    assertThat(resolve().auth.value()).isEqualTo("API-FROM-PARAMETER");
  }

  @Test
  void resolvesAParameterSourcedKeyAgainstTheProjectWithoutABuildConfiguration() throws Exception {
    parameterSourcedConnection();
    final ValueResolver resolver = resolverReturning("API-FROM-PROJECT", true);
    when(project.getValueResolver()).thenReturn(resolver);

    assertThat(resolve().auth.value()).isEqualTo("API-FROM-PROJECT");
  }

  @Test
  void explainsAParameterThatOnlyResolvesDuringABuild() {
    parameterSourcedConnection();
    final ValueResolver resolver = resolverReturning("%octopus.apikey%", false);
    when(project.getValueResolver()).thenReturn(resolver);

    assertThat(refusal()).contains("%octopus.apikey%").contains("space name instead");
  }

  @Test
  void refusesAParameterSourcedKeyToAUserWhoCannotRunBuilds() {
    parameterSourcedConnection();
    when(user.isPermissionGrantedForProject(PROJECT_INTERNAL_ID, Permission.RUN_BUILD))
        .thenReturn(false);
    final ValueResolver resolver = resolverReturning("API-FROM-PROJECT", true);
    when(project.getValueResolver()).thenReturn(resolver);

    assertThat(refusal())
        .contains("with an API key parameter by someone who can run builds")
        .contains("space name instead");
  }

  @Test
  void refusesABuildConfigurationFromAnotherProject() {
    parameterSourcedConnection();
    final SBuildType buildType = buildTypeInProject("project999");
    when(user.isPermissionGrantedForProject("project999", Permission.RUN_BUILD)).thenReturn(true);
    final ValueResolver resolver = resolverReturning("API-FROM-PARAMETER", true);
    when(buildType.getValueResolver()).thenReturn(resolver);

    assertThat(refusal()).contains("MyProject_Deploy").contains("was not found in this project");
  }

  @Test
  void refusesAnUnknownBuildConfiguration() {
    parameterSourcedConnection();
    buildTypeId = "Nope";

    assertThat(refusal()).contains("Nope").contains("was not found");
  }

  @Test
  void listsAnOidcConnectionsSpacesWithAnAccessTokenForTheBuildConfiguration() throws Exception {
    oidcSourcedConnection();
    buildTypeInProject(PROJECT_INTERNAL_ID);

    final Credentials credentials = resolve();

    assertThat(credentials.auth.value()).isEqualTo("Bearer octopus-access");
    assertThat(idTokenRequests).containsExactly("oidc-1 for MyProject_Deploy");
  }

  @Test
  void refusesAnOidcConnectionToAUserWhoCannotRunBuilds() {
    oidcSourcedConnection();
    when(user.isPermissionGrantedForProject(PROJECT_INTERNAL_ID, Permission.RUN_BUILD))
        .thenReturn(false);

    assertThat(refusal()).contains("with an OIDC connection by someone who can run builds");
    assertThat(idTokenRequests).isEmpty();
  }

  @Test
  void usesTheStoredKeyForABlankSource() throws Exception {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.API_KEY_SOURCE, "");
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    connection(params);

    assertThat(resolve().auth.value()).isEqualTo("API-STORED");
  }
}
