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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.parameters.ProcessingResult;
import jetbrains.buildServer.parameters.ValueResolver;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.client.OctopusTokenExchange;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OctopusSpacesControllerTest {

  private static final String PROJECT_EXTERNAL_ID = "MyProject";
  private static final String PROJECT_INTERNAL_ID = "project123";

  private final OctopusConnectionsManager connectionsManager =
      mock(OctopusConnectionsManager.class);
  private final ProjectManager projectManager = mock(ProjectManager.class);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);
  private final SUser user = mock(SUser.class);
  private final SProject project = mock(SProject.class);
  private final StringWriter written = new StringWriter();

  private final List<String> fetchedUrls = new java.util.ArrayList<>();
  private final List<String> fetchedKeys = new java.util.ArrayList<>();

  private SUser currentUser = null;
  private IOException fetchFailure = null;
  private final List<String> idTokenRequests = new java.util.ArrayList<>();
  private IOException idTokenFailure = null;
  private final List<String> exchangeUrls = new java.util.ArrayList<>();
  private final List<String> exchangeBodies = new java.util.ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    when(response.getWriter()).thenReturn(new PrintWriter(written));
    when(project.getProjectId()).thenReturn(PROJECT_INTERNAL_ID);
    when(projectManager.findProjectByExternalId(PROJECT_EXTERNAL_ID)).thenReturn(project);
    when(request.getParameter("projectId")).thenReturn(PROJECT_EXTERNAL_ID);
    currentUser = user;
    grantEditProject(true);
  }

  private void grantEditProject(final boolean granted) {
    when(user.isPermissionGrantedForProject(PROJECT_INTERNAL_ID, Permission.EDIT_PROJECT))
        .thenReturn(granted);
  }

  private OctopusSpacesController controller() {
    final OctopusSpacesFetcher fetcher =
        new OctopusSpacesFetcher(
            (url, auth) -> {
              fetchedUrls.add(url);
              fetchedKeys.add(auth.value());
              if (fetchFailure != null) {
                throw fetchFailure;
              }
              return "{\"Items\":[{\"Id\":\"Spaces-123\",\"Name\":\"Example Space\"}]}";
            });
    final OidcIdTokens idTokens =
        (tokenProject, oidcConnectionId, buildTypeId) -> {
          idTokenRequests.add(
              oidcConnectionId
                  + " for "
                  + (buildTypeId == null ? "no build configuration" : buildTypeId));
          if (idTokenFailure != null) {
            throw idTokenFailure;
          }
          return "id.token.value";
        };
    final OctopusTokenExchange exchange =
        new OctopusTokenExchange(
            (url, json) -> {
              exchangeUrls.add(url);
              exchangeBodies.add(json);
              return "{\"access_token\":\"octopus-access\"}";
            });
    return new OctopusSpacesController(
        null, connectionsManager, projectManager, fetcher, idTokens, exchange, req -> currentUser);
  }

  private String handle() throws Exception {
    controller().doHandle(request, response);
    return written.toString();
  }

  private OAuthConnectionDescriptor connectionWith(final Map<String, String> params) {
    final OAuthConnectionDescriptor descriptor = mock(OAuthConnectionDescriptor.class);
    when(descriptor.getParameters()).thenReturn(params);
    return descriptor;
  }

  @Test
  void returnsSpacesForASavedConnection() throws Exception {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    final OAuthConnectionDescriptor connection = connectionWith(params);
    when(connectionsManager.resolve(eq(project), eq("octopus-conn-1")))
        .thenReturn(Optional.of(connection));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");

    final String body = handle();

    assertThat(body).contains("Spaces-123").contains("Example Space");
    assertThat(fetchedKeys).containsExactly("API-STORED");
  }

  @Test
  void refusesToFetchWithoutASavedConnection() throws Exception {
    // The request must never supply the address to fetch: the only URLs this endpoint will call are
    // ones an admin already persisted into project configuration.
    when(request.getParameter("serverUrl")).thenReturn("https://attacker.example.com");
    when(request.getParameter("apiKey")).thenReturn("API-TYPED");

    final String body = handle();

    assertThat(body).contains("saved Octopus connection");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void rejectsAnAnonymousCaller() throws Exception {
    currentUser = null;

    final String body = handle();

    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(body).contains("not logged in");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void rejectsAUserWithoutEditProjectPermission() throws Exception {
    // Without this the endpoint would let any logged-in user aim the server at a URL of their
    // choosing and test credentials through it.
    grantEditProject(false);

    final String body = handle();

    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    assertThat(body).contains("permission");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void rejectsAnUnknownProject() throws Exception {
    when(request.getParameter("projectId")).thenReturn("nope");

    final String body = handle();

    verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(body).contains("Unknown project");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void ignoresPostedCredentialsEntirely() throws Exception {
    // Posted values are not consulted at all, so they can neither redirect the fetch nor replace
    // the stored key.
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    final OAuthConnectionDescriptor connection = connectionWith(params);
    when(connectionsManager.resolve(eq(project), eq("octopus-conn-1")))
        .thenReturn(Optional.of(connection));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");
    when(request.getParameter("apiKey")).thenReturn("API-ATTACKER-SUPPLIED");
    when(request.getParameter("serverUrl")).thenReturn("https://attacker.example.com");

    handle();

    assertThat(fetchedKeys).containsExactly("API-STORED");
    assertThat(fetchedUrls)
        .containsExactly("https://stored.example.com/api/spaces?skip=0&take=1000");
  }

  private Map<String, String> parameterSourcedConnection() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(
        ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_PARAMETER);
    params.put(ConnectionPropertyNames.API_KEY_PARAMETER, "%octopus.apikey%");
    final OAuthConnectionDescriptor connection = connectionWith(params);
    when(connectionsManager.resolve(eq(project), eq("octopus-conn-1")))
        .thenReturn(Optional.of(connection));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");
    return params;
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
    when(request.getParameter("buildTypeId")).thenReturn("MyProject_Deploy");
    return buildType;
  }

  @Test
  void resolvesAParameterSourcedKeyAgainstTheBuildConfiguration() throws Exception {
    parameterSourcedConnection();
    final SBuildType buildType = buildTypeInProject(PROJECT_INTERNAL_ID);
    final ValueResolver resolver = resolverReturning("API-FROM-PARAMETER", true);
    when(buildType.getValueResolver()).thenReturn(resolver);

    final String body = handle();

    assertThat(body).contains("Spaces-123");
    assertThat(fetchedKeys).containsExactly("API-FROM-PARAMETER");
    assertThat(body).doesNotContain("API-FROM-PARAMETER");
  }

  @Test
  void resolvesAParameterSourcedKeyAgainstTheProjectWithoutABuildConfiguration() throws Exception {
    parameterSourcedConnection();
    final ValueResolver resolver = resolverReturning("API-FROM-PROJECT", true);
    when(project.getValueResolver()).thenReturn(resolver);

    handle();

    assertThat(fetchedKeys).containsExactly("API-FROM-PROJECT");
  }

  @Test
  void explainsAParameterThatOnlyResolvesDuringABuild() throws Exception {
    parameterSourcedConnection();
    final ValueResolver resolver = resolverReturning("%octopus.apikey%", false);
    when(project.getValueResolver()).thenReturn(resolver);

    final String body = handle();

    assertThat(body).contains("%octopus.apikey%").contains("space name instead");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void refusesABuildConfigurationTheUserCannotEdit() throws Exception {
    parameterSourcedConnection();
    buildTypeInProject("project999");
    when(user.isPermissionGrantedForProject("project999", Permission.EDIT_PROJECT))
        .thenReturn(false);

    final String body = handle();

    assertThat(body).contains("was not found, or you do not have permission to edit its project");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void refusesAnUnknownBuildConfiguration() throws Exception {
    parameterSourcedConnection();
    when(request.getParameter("buildTypeId")).thenReturn("Nope");

    assertThat(handle()).contains("Nope").contains("was not found");
    assertThat(fetchedUrls).isEmpty();
  }

  private void oidcSourcedConnection() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_OIDC);
    params.put(ConnectionPropertyNames.OIDC_CONNECTION_ID, "oidc-1");
    final OAuthConnectionDescriptor connection = connectionWith(params);
    when(connectionsManager.resolve(eq(project), eq("octopus-conn-1")))
        .thenReturn(Optional.of(connection));
    final OAuthConnectionDescriptor oidcConnector =
        connectionWith(
            Collections.singletonMap(
                ConnectionPropertyNames.OIDC_CONNECTOR_AUDIENCE, "service-account-guid"));
    when(connectionsManager.resolve(eq(project), eq("oidc-1")))
        .thenReturn(Optional.of(oidcConnector));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");
  }

  @Test
  void listsSpacesForAnOidcConnectionWithAnExchangedAccessToken() throws Exception {
    oidcSourcedConnection();
    buildTypeInProject(PROJECT_INTERNAL_ID);

    final String body = handle();

    assertThat(body).contains("Spaces-123");
    assertThat(idTokenRequests).containsExactly("oidc-1 for MyProject_Deploy");
    assertThat(exchangeUrls).containsExactly("https://stored.example.com/token/v1");
    assertThat(exchangeBodies.get(0)).contains("service-account-guid").contains("id.token.value");
    assertThat(fetchedKeys).containsExactly("Bearer octopus-access");
    assertThat(body).doesNotContain("id.token.value").doesNotContain("octopus-access");
  }

  @Test
  void requestsAProjectScopedOidcTokenWithoutABuildConfiguration() throws Exception {
    oidcSourcedConnection();

    handle();

    assertThat(idTokenRequests).containsExactly("oidc-1 for no build configuration");
  }

  @Test
  void surfacesTheOidcPluginsRefusal() throws Exception {
    oidcSourcedConnection();
    idTokenFailure =
        new IOException(
            "Enable the connection subject dimension on this OIDC connection to use it outside a"
                + " build.");

    final String body = handle();

    assertThat(body).contains("Enable the connection subject dimension");
    assertThat(exchangeUrls).isEmpty();
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void reportsAnUnresolvableConnection() throws Exception {
    when(connectionsManager.resolve(eq(project), any())).thenReturn(Optional.empty());
    when(request.getParameter("connectionId")).thenReturn("gone");

    assertThat(handle()).contains("could not be resolved");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void surfacesALookupFailureAsAnErrorMessage() throws Exception {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    final OAuthConnectionDescriptor connection = connectionWith(params);
    when(connectionsManager.resolve(eq(project), any())).thenReturn(Optional.of(connection));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");
    fetchFailure = new IOException("Octopus rejected the API key (HTTP 401).");

    final String body = handle();

    assertThat(body).contains("Octopus rejected the API key");
    // The key itself must never be echoed back to the browser.
    assertThat(body).doesNotContain("API-STORED");
  }

  @Test
  void neverCachesTheLookup() throws Exception {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    final OAuthConnectionDescriptor connection = connectionWith(params);
    when(connectionsManager.resolve(eq(project), any())).thenReturn(Optional.of(connection));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");

    handle();

    verify(response).setHeader("Cache-Control", "no-store");
    verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
  }
}
