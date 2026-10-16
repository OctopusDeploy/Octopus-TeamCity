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

import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.parameters.ProcessingResult;
import jetbrains.buildServer.serverSide.ParametersSupport;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.users.SUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.client.OctopusAuth;
import octopus.teamcity.server.connection.OctopusConnection;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.jetbrains.annotations.Nullable;

/**
 * The credentials for listing a saved connection's spaces, read server-side from the connection.
 *
 * <p>The request names a connection and never supplies a URL or key of its own. The only URLs the
 * server calls are ones an admin has already saved into project configuration, and a stored secret,
 * which TeamCity renders to the browser as a placeholder, never has to come from the page. The cost
 * is that a brand-new connection has to be saved once before its spaces can be listed, and a step
 * using inline credentials cannot list spaces at all. Both fall back to typing the space name, as
 * does a connection that authenticates with OIDC.
 *
 * <ul>
 *   <li>A build configuration named by the request is used only when it belongs to the request's
 *       project, as a build of it would find the connection from there.
 *   <li>An API key held in a parameter is resolved only for a caller who also holds {@link
 *       Permission#RUN_BUILD} on the project, and each such lookup is logged. The parameter's value
 *       may be a password the caller cannot read, but a caller who can edit and run a build can
 *       already send the value anywhere from a build step.
 * </ul>
 */
class SpacesLookupCredentials {

  private final OctopusConnectionsManager connectionsManager;
  private final ProjectManager projectManager;

  SpacesLookupCredentials(
      final OctopusConnectionsManager connectionsManager, final ProjectManager projectManager) {
    this.connectionsManager = connectionsManager;
    this.projectManager = projectManager;
  }

  /**
   * @param connectionId the saved Octopus connection to list spaces for
   * @param buildTypeId the step form's build configuration, or {@code null} on the connection form
   */
  Credentials resolve(
      final SProject project,
      final SUser user,
      @Nullable final String connectionId,
      @Nullable final String buildTypeId)
      throws CredentialsUnavailableException {
    if (connectionId == null || connectionId.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "Spaces can only be listed for a saved Octopus connection. Save this connection (or"
              + " select one) and try again, or enter the space name instead.");
    }
    final Optional<OAuthConnectionDescriptor> connection =
        connectionsManager
            .resolve(project, connectionId.trim())
            .filter(SpacesLookupCredentials::isOctopusConnection);
    if (!connection.isPresent()) {
      throw new CredentialsUnavailableException("That Octopus connection could not be resolved.");
    }

    final Map<String, String> params = connection.get().getParameters();
    final String serverUrl = params.get(ConnectionPropertyNames.SERVER_URL);
    final String source =
        params.getOrDefault(
            ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_KEY);
    if (ConnectionPropertyNames.API_KEY_SOURCE_PARAMETER.equals(source)) {
      final SBuildType buildType = projectBuildType(buildTypeId, project);
      requireRunBuild(user, project, "an API key parameter");
      final String reference = apiKeyParameterReference(params);
      final String apiKey =
          resolveApiKeyParameter(reference, buildType == null ? project : buildType);
      Loggers.SERVER.info(
          "User "
              + user.getUsername()
              + " resolved the API key parameter "
              + reference
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
      throw new CredentialsUnavailableException(
          "Spaces cannot be listed for a connection that authenticates with OIDC. Enter the space"
              + " name instead.");
    }
    return new Credentials(
        serverUrl, OctopusAuth.apiKey(params.get(ConnectionPropertyNames.API_KEY)));
  }

  private static boolean isOctopusConnection(final OAuthConnectionDescriptor connection) {
    final OAuthProvider provider = connection.getOauthProvider();
    return provider != null && OctopusConnection.TYPE.equals(provider.getType());
  }

  /**
   * The step form's build configuration, or {@code null} on a form that has none. A build
   * configuration that does not exist and one in another project get the same message; the request
   * cannot reveal which build configurations exist.
   */
  @Nullable
  private SBuildType projectBuildType(@Nullable final String buildTypeId, final SProject project)
      throws CredentialsUnavailableException {
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

  private static void requireRunBuild(
      final SUser user, final SProject project, final String credentialKind)
      throws CredentialsUnavailableException {
    if (!user.isPermissionGrantedForProject(project.getProjectId(), Permission.RUN_BUILD)) {
      throw new CredentialsUnavailableException(
          "Spaces can only be listed with "
              + credentialKind
              + " by someone who can run builds here. Enter the space name instead.");
    }
  }

  /** The connection's {@code %parameter%} reference, trimmed as a build injects it. */
  private static String apiKeyParameterReference(final Map<String, String> params)
      throws CredentialsUnavailableException {
    final String reference = params.get(ConnectionPropertyNames.API_KEY_PARAMETER);
    if (reference == null || reference.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "This connection does not use an API key parameter.");
    }
    return reference.trim();
  }

  /** Resolves {@code reference} as a build of {@code scope} would. */
  private static String resolveApiKeyParameter(
      final String reference, final ParametersSupport scope)
      throws CredentialsUnavailableException {
    final ProcessingResult resolved = scope.getValueResolver().resolve(reference);
    final String apiKey = resolved.getResult();
    if (!resolved.isFullyResolved() || apiKey == null || apiKey.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "The API key parameter "
              + reference
              + " can't be resolved here - it may only be set when a build runs. Enter the space"
              + " name instead.");
    }
    return apiKey;
  }
}
