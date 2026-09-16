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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Lists an Octopus server's spaces for the space picker in the connection and step forms.
 *
 * <p>This endpoint makes an outbound request to a URL the caller influences, so it is deliberately
 * narrow:
 *
 * <ul>
 *   <li>the caller must be logged in and hold {@link Permission#EDIT_PROJECT} on the project whose
 *       form they are editing - the same permission needed to configure a connection in the first
 *       place, so it grants no reach a user did not already have;
 *   <li>when a saved connection is named, its credentials are read server-side and the request's
 *       own {@code apiKey} is ignored, because TeamCity renders a stored secret as a placeholder
 *       and the browser therefore does not hold the real key;
 *   <li>the API key is never echoed back, in an error message or otherwise.
 * </ul>
 */
public class OctopusSpacesController extends BaseController {

  public static final String PATH = "/octopus/listSpaces.html";

  private static final Gson GSON = new GsonBuilder().create();

  /** How the current user is found. A seam so the permission checks are testable. */
  interface UserResolver {
    SUser resolve(HttpServletRequest request);
  }

  private final OctopusConnectionsManager connectionsManager;
  private final ProjectManager projectManager;
  private final OctopusSpacesFetcher fetcher;
  private final UserResolver userResolver;

  public OctopusSpacesController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final ProjectManager projectManager) {
    this(
        webControllerManager,
        connectionsManager,
        projectManager,
        new OctopusSpacesFetcher(),
        SessionUser::getUser);
  }

  OctopusSpacesController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final ProjectManager projectManager,
      final OctopusSpacesFetcher fetcher,
      final UserResolver userResolver) {
    this.connectionsManager = connectionsManager;
    this.projectManager = projectManager;
    this.fetcher = fetcher;
    this.userResolver = userResolver;
    if (webControllerManager != null) {
      webControllerManager.registerController(PATH, this);
    }
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(
      @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response)
      throws Exception {
    response.setContentType("application/json; charset=UTF-8");
    // This is a lookup for a form; a stale list must never be served from a cache.
    response.setHeader("Cache-Control", "no-store");

    final SUser user = userResolver.resolve(request);
    if (user == null) {
      writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "You are not logged in.");
      return null;
    }

    final SProject project = findProject(request.getParameter("projectId"));
    if (project == null) {
      writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Unknown project.");
      return null;
    }
    if (!user.isPermissionGrantedForProject(project.getProjectId(), Permission.EDIT_PROJECT)) {
      writeError(
          response,
          HttpServletResponse.SC_FORBIDDEN,
          "You do not have permission to edit this project.");
      return null;
    }

    final Credentials credentials;
    try {
      credentials = resolveCredentials(request, project);
    } catch (final CredentialsUnavailableException e) {
      writeError(response, HttpServletResponse.SC_OK, e.getMessage());
      return null;
    }

    try {
      final List<OctopusSpace> spaces = fetcher.fetch(credentials.serverUrl, credentials.apiKey);
      writeSpaces(response, spaces);
    } catch (final IOException e) {
      // The fetcher's messages describe the failure without quoting the key back.
      writeError(response, HttpServletResponse.SC_OK, e.getMessage());
    }
    return null;
  }

  @Nullable
  private SProject findProject(@Nullable final String projectExternalId) {
    if (projectExternalId == null || projectExternalId.trim().isEmpty()) {
      return null;
    }
    return projectManager.findProjectByExternalId(projectExternalId.trim());
  }

  /**
   * Credentials for the lookup, always read from a saved connection.
   *
   * <p>The request names a connection; it never supplies a URL or key of its own. That is
   * deliberate: it keeps the server from being asked to fetch an arbitrary caller-supplied address,
   * so the only URLs it will ever call are ones an admin has already persisted into project
   * configuration. It also sidesteps TeamCity rendering a saved secret as a placeholder, which
   * means the browser could not supply the real key anyway.
   *
   * <p>The cost is that a brand-new connection has to be saved once before its spaces can be
   * listed, and a step using inline credentials cannot list spaces at all. Both fall back to typing
   * the space name.
   */
  private Credentials resolveCredentials(final HttpServletRequest request, final SProject project)
      throws CredentialsUnavailableException {
    final String connectionId = request.getParameter("connectionId");
    if (connectionId == null || connectionId.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "Spaces can only be listed for a saved Octopus connection. Save this connection (or"
              + " select one) and try again, or enter the space name instead.");
    }

    final Optional<OAuthConnectionDescriptor> connection =
        connectionsManager.resolve(project, connectionId.trim());
    if (!connection.isPresent()) {
      throw new CredentialsUnavailableException("That Octopus connection could not be resolved.");
    }

    final Map<String, String> params = connection.get().getParameters();
    final String source =
        params.getOrDefault(
            ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_KEY);
    if (ConnectionPropertyNames.API_KEY_SOURCE_PARAMETER.equals(source)) {
      throw new CredentialsUnavailableException(
          "This connection reads its API key from a build parameter, which is only resolved when"
              + " a build runs, so spaces cannot be listed here. Enter the space name instead.");
    }
    if (ConnectionPropertyNames.API_KEY_SOURCE_OIDC.equals(source)) {
      throw new CredentialsUnavailableException(
          "This connection authenticates with OIDC, and its token is only issued while a build"
              + " runs, so spaces cannot be listed here. Enter the space name instead.");
    }
    return new Credentials(
        params.get(ConnectionPropertyNames.SERVER_URL),
        params.get(ConnectionPropertyNames.API_KEY));
  }

  private static void writeSpaces(
      final HttpServletResponse response, final List<OctopusSpace> spaces) throws IOException {
    final List<Map<String, String>> items = new ArrayList<>();
    for (final OctopusSpace space : spaces) {
      final Map<String, String> item = new LinkedHashMap<>();
      item.put("id", space.getId());
      item.put("name", space.getName());
      items.add(item);
    }
    final Map<String, Object> body = new LinkedHashMap<>();
    body.put("spaces", items);
    writeJson(response, body);
  }

  private static void writeError(
      final HttpServletResponse response, final int status, final String message)
      throws IOException {
    response.setStatus(status);
    final Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", message);
    writeJson(response, body);
  }

  private static void writeJson(final HttpServletResponse response, final Map<String, Object> body)
      throws IOException {
    try (PrintWriter writer = response.getWriter()) {
      writer.write(GSON.toJson(body));
    }
  }

  private static final class Credentials {
    private final String serverUrl;
    private final String apiKey;

    private Credentials(final String serverUrl, final String apiKey) {
      this.serverUrl = serverUrl;
      this.apiKey = apiKey;
    }
  }

  private static final class CredentialsUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    private CredentialsUnavailableException(final String message) {
      super(message);
    }
  }
}
