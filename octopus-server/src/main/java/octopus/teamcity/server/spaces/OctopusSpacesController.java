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
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.parameters.ProcessingResult;
import jetbrains.buildServer.serverSide.ParametersSupport;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.client.OctopusAuth;
import octopus.teamcity.server.client.OctopusTokenExchange;
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
 *   <li>when a saved connection is named, its credentials are read server-side and the request's
 *       own {@code apiKey} is ignored, because TeamCity renders a stored secret as a placeholder
 *       and the browser therefore does not hold the real key;
 *   <li>a build configuration named by the request is used only when it belongs to that project, as
 *       a build of it would find the connection from there;
 *   <li>an API key held in a parameter is resolved only for a caller who also holds {@link
 *       Permission#RUN_BUILD} on the project, and each such lookup is logged. Its value may be a
 *       password the caller cannot read, but a caller who can edit and run a build can already send
 *       it anywhere from a build step;
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

  private final OctopusConnectionsManager connectionsManager;
  private final ProjectManager projectManager;
  private final OctopusSpacesFetcher fetcher;
  private final OidcIdTokens oidcIdTokens;
  private final OctopusTokenExchange tokenExchange;
  private final UserResolver userResolver;

  public OctopusSpacesController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final ProjectManager projectManager,
      final ApplicationContext applicationContext) {
    this(
        webControllerManager,
        connectionsManager,
        projectManager,
        new OctopusSpacesFetcher(),
        oidcIdTokensFor(applicationContext),
        new OctopusTokenExchange(),
        SessionUser::getUser);
  }

  OctopusSpacesController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final ProjectManager projectManager,
      final OctopusSpacesFetcher fetcher,
      final OidcIdTokens oidcIdTokens,
      final OctopusTokenExchange tokenExchange,
      final UserResolver userResolver) {
    this.connectionsManager = connectionsManager;
    this.projectManager = projectManager;
    this.fetcher = fetcher;
    this.oidcIdTokens = oidcIdTokens;
    this.tokenExchange = tokenExchange;
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
      credentials = resolveCredentials(request, project, user);
    } catch (final CredentialsUnavailableException e) {
      writeError(response, HttpServletResponse.SC_OK, e.getMessage());
      return null;
    }

    try {
      final List<OctopusSpace> spaces = fetcher.fetch(credentials.serverUrl, credentials.auth);
      writeSpaces(response, spaces);
    } catch (final IOException e) {
      Loggers.SERVER.warn(
          "Could not list the spaces on "
              + credentials.serverUrl
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
  private Credentials resolveCredentials(
      final HttpServletRequest request, final SProject project, final SUser user)
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
    final String serverUrl = params.get(ConnectionPropertyNames.SERVER_URL);
    final String source =
        params.getOrDefault(
            ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_KEY);
    if (ConnectionPropertyNames.API_KEY_SOURCE_PARAMETER.equals(source)) {
      final SBuildType buildType = projectBuildType(request, project);
      if (!user.isPermissionGrantedForProject(project.getProjectId(), Permission.RUN_BUILD)) {
        throw new CredentialsUnavailableException(
            "Spaces can only be listed with an API key parameter by someone who can run builds"
                + " here. Enter the space name instead.");
      }
      final String apiKey = resolveApiKeyParameter(params, buildType == null ? project : buildType);
      Loggers.SERVER.info(
          "User "
              + user.getUsername()
              + " resolved the API key parameter "
              + params.get(ConnectionPropertyNames.API_KEY_PARAMETER).trim()
              + " of Octopus connection "
              + connectionId.trim()
              + " in "
              + (buildType == null
                  ? "project " + project.getExternalId()
                  : "build configuration " + buildType.getExternalId())
              + " to list the spaces on "
              + serverUrl);
      return new Credentials(serverUrl, OctopusAuth.apiKey(apiKey));
    }
    if (ConnectionPropertyNames.API_KEY_SOURCE_OIDC.equals(source)) {
      final SBuildType buildType = projectBuildType(request, project);
      return new Credentials(
          serverUrl,
          OctopusAuth.accessToken(oidcAccessToken(params, project, buildType, serverUrl)));
    }
    return new Credentials(
        serverUrl, OctopusAuth.apiKey(params.get(ConnectionPropertyNames.API_KEY)));
  }

  /**
   * The step form's build configuration, or {@code null} on a form that has none. A missing one and
   * one in another project get the same message, so the request cannot be used to find out which
   * build configurations exist.
   */
  @Nullable
  private SBuildType projectBuildType(final HttpServletRequest request, final SProject project)
      throws CredentialsUnavailableException {
    final String buildTypeId = request.getParameter("buildTypeId");
    if (buildTypeId == null || buildTypeId.trim().isEmpty()) {
      return null;
    }
    final SBuildType buildType = projectManager.findBuildTypeByExternalId(buildTypeId.trim());
    if (buildType == null || !project.getProjectId().equals(buildType.getProjectId())) {
      throw new CredentialsUnavailableException(
          "Build configuration '" + buildTypeId.trim() + "' was not found in this project.");
    }
    return buildType;
  }

  /** Resolves the connection's {@code %parameter%} reference as a build of {@code scope} would. */
  private static String resolveApiKeyParameter(
      final Map<String, String> params, final ParametersSupport scope)
      throws CredentialsUnavailableException {
    final String reference = params.get(ConnectionPropertyNames.API_KEY_PARAMETER);
    if (reference == null || reference.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "This connection does not name an API key parameter.");
    }
    final ProcessingResult resolved = scope.getValueResolver().resolve(reference.trim());
    if (!resolved.isFullyResolved()
        || resolved.getResult() == null
        || resolved.getResult().trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "The API key parameter "
              + reference.trim()
              + " can't be resolved here - it may only be set when a build runs. Enter the space"
              + " name instead.");
    }
    return resolved.getResult();
  }

  private String oidcAccessToken(
      final Map<String, String> params,
      final SProject project,
      @Nullable final SBuildType buildType,
      final String serverUrl)
      throws CredentialsUnavailableException {
    final String oidcConnectionId = params.get(ConnectionPropertyNames.OIDC_CONNECTION_ID);
    if (oidcConnectionId == null || oidcConnectionId.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "This connection does not name an OIDC connection.");
    }
    final Optional<OAuthConnectionDescriptor> oidcConnector =
        connectionsManager.resolve(project, oidcConnectionId.trim());
    if (!oidcConnector.isPresent()) {
      throw new CredentialsUnavailableException(
          "The OIDC connection '" + oidcConnectionId.trim() + "' could not be resolved.");
    }
    // The connector's audience is the Octopus service account id, as at build time.
    final String audience =
        oidcConnector.get().getParameters().get(ConnectionPropertyNames.OIDC_CONNECTOR_AUDIENCE);
    if (audience == null || audience.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "The OIDC connection '" + oidcConnectionId.trim() + "' has no audience.");
    }
    try {
      final String idToken =
          oidcIdTokens.request(
              project,
              oidcConnectionId.trim(),
              buildType == null ? null : buildType.getExternalId());
      return tokenExchange.exchange(serverUrl, audience.trim(), idToken);
    } catch (final IOException e) {
      Loggers.SERVER.warn(
          "Could not get an Octopus access token from "
              + serverUrl
              + " through OIDC connection "
              + oidcConnectionId.trim()
              + " for project "
              + project.getExternalId(),
          e);
      throw new CredentialsUnavailableException(e.getMessage());
    }
  }

  private static OidcIdTokens oidcIdTokensFor(final ApplicationContext applicationContext) {
    if (OidcIdTokenRequester.isSupported()) {
      return new OidcIdTokenRequester(applicationContext);
    }
    return (project, oidcConnectionId, buildTypeExternalId) -> {
      throw new IOException(OidcIdTokenRequester.UNSUPPORTED_MESSAGE);
    };
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
    private final OctopusAuth auth;

    private Credentials(final String serverUrl, final OctopusAuth auth) {
      this.serverUrl = serverUrl;
      this.auth = auth;
    }
  }

  private static final class CredentialsUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    private CredentialsUnavailableException(final String message) {
      super(message);
    }
  }
}
