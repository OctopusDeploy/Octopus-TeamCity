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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
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
 *   <li>the credentials come from a saved connection, never from the request, under the rules in
 *       {@link SpacesLookupCredentials};
 *   <li>no credential - API key, ID token or access token - is echoed back, in an error message or
 *       otherwise.
 * </ul>
 */
public class OctopusSpacesController extends BaseController {

  public static final String PATH = "/octopus/listSpaces.html";

  private static final Gson GSON = new GsonBuilder().create();

  /** How the current user is found. A seam so the permission checks are testable. */
  interface UserResolver {
    SUser resolve(HttpServletRequest request);
  }

  private final ProjectManager projectManager;
  private final SpacesLookupCredentials credentials;
  private final OctopusSpacesFetcher fetcher;
  private final UserResolver userResolver;

  public OctopusSpacesController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final ProjectManager projectManager,
      final ApplicationContext applicationContext) {
    this(
        webControllerManager,
        projectManager,
        new SpacesLookupCredentials(
            connectionsManager,
            projectManager,
            new OidcAccessTokens(connectionsManager, applicationContext)),
        new OctopusSpacesFetcher(),
        SessionUser::getUser);
  }

  OctopusSpacesController(
      final WebControllerManager webControllerManager,
      final ProjectManager projectManager,
      final SpacesLookupCredentials credentials,
      final OctopusSpacesFetcher fetcher,
      final UserResolver userResolver) {
    this.projectManager = projectManager;
    this.credentials = credentials;
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

    final Credentials resolved;
    try {
      resolved =
          credentials.resolve(
              project,
              user,
              request.getParameter("connectionId"),
              request.getParameter("buildTypeId"));
    } catch (final CredentialsUnavailableException e) {
      writeError(response, HttpServletResponse.SC_OK, e.getMessage());
      return null;
    }

    try {
      final List<OctopusSpace> spaces = fetcher.fetch(resolved.serverUrl, resolved.auth);
      writeSpaces(response, spaces);
    } catch (final IOException e) {
      Loggers.SERVER.warn(
          "Could not list the spaces on "
              + resolved.serverUrl
              + " for project "
              + project.getExternalId(),
          e);
      // The fetcher's messages describe the failure without quoting the credential back.
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
}
