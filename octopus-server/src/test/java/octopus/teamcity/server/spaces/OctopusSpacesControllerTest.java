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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.serverSide.ProjectManager;
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

  private final List<String> fetchedUrls = new ArrayList<>();
  private final List<String> fetchedKeys = new ArrayList<>();

  private SUser currentUser = null;
  private IOException fetchFailure = null;

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
    final OidcAccessTokens noOidc =
        new OidcAccessTokens(
            connectionsManager,
            (tokenProject, oidcConnectionId, buildTypeId) -> {
              throw new IOException("not used");
            },
            new OctopusTokenExchange(
                (url, json) -> {
                  throw new IOException("not used");
                }));
    return new OctopusSpacesController(
        null,
        projectManager,
        new SpacesLookupCredentials(connectionsManager, projectManager, noOidc),
        fetcher,
        req -> currentUser);
  }

  private String handle() throws Exception {
    controller().doHandle(request, response);
    return written.toString();
  }

  private void storedKeyConnection() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://stored.example.com");
    params.put(ConnectionPropertyNames.API_KEY, "API-STORED");
    final OAuthConnectionDescriptor connection = mock(OAuthConnectionDescriptor.class);
    when(connection.getParameters()).thenReturn(params);
    when(connectionsManager.resolve(eq(project), any())).thenReturn(Optional.of(connection));
    when(request.getParameter("connectionId")).thenReturn("octopus-conn-1");
  }

  @Test
  void returnsSpacesForASavedConnection() throws Exception {
    storedKeyConnection();

    final String body = handle();

    assertThat(body).contains("Spaces-123").contains("Example Space");
    assertThat(fetchedKeys).containsExactly("API-STORED");
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
    storedKeyConnection();
    when(request.getParameter("apiKey")).thenReturn("API-ATTACKER-SUPPLIED");
    when(request.getParameter("serverUrl")).thenReturn("https://attacker.example.com");

    handle();

    assertThat(fetchedKeys).containsExactly("API-STORED");
    assertThat(fetchedUrls)
        .containsExactly("https://stored.example.com/api/spaces?skip=0&take=" + Integer.MAX_VALUE);
  }

  @Test
  void explainsWhyCredentialsAreUnavailable() throws Exception {
    final String body = handle();

    verify(response).setStatus(HttpServletResponse.SC_OK);
    assertThat(body).contains("saved Octopus connection");
    assertThat(fetchedUrls).isEmpty();
  }

  @Test
  void surfacesALookupFailureAsAnErrorMessage() throws Exception {
    storedKeyConnection();
    fetchFailure = new IOException("Octopus rejected the API key (HTTP 401).");

    final String body = handle();

    assertThat(body).contains("Octopus rejected the API key");
    // The key itself must never be echoed back to the browser.
    assertThat(body).doesNotContain("API-STORED");
  }

  @Test
  void neverCachesTheLookup() throws Exception {
    storedKeyConnection();

    handle();

    verify(response).setHeader("Cache-Control", "no-store");
    verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
  }
}
