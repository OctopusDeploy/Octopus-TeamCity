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
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.client.OctopusTokenExchange;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

/**
 * An Octopus access token for a connection that authenticates with OIDC: an ID token from the
 * connection's OIDC connector, exchanged at Octopus for an access token.
 */
class OidcAccessTokens {

  private final OctopusConnectionsManager connectionsManager;
  private final OidcIdTokens idTokens;
  private final OctopusTokenExchange tokenExchange;

  OidcAccessTokens(
      final OctopusConnectionsManager connectionsManager,
      final ApplicationContext applicationContext) {
    this(connectionsManager, idTokensFor(applicationContext), new OctopusTokenExchange());
  }

  OidcAccessTokens(
      final OctopusConnectionsManager connectionsManager,
      final OidcIdTokens idTokens,
      final OctopusTokenExchange tokenExchange) {
    this.connectionsManager = connectionsManager;
    this.idTokens = idTokens;
    this.tokenExchange = tokenExchange;
  }

  /**
   * An access token for {@code serverUrl}, from an ID token issued with the subject a build of
   * {@code buildType} would get, or scoped to {@code project} when that is {@code null}.
   */
  String accessToken(
      final Map<String, String> connectionParams,
      final SProject project,
      @Nullable final SBuildType buildType,
      final String serverUrl)
      throws CredentialsUnavailableException {
    final String oidcConnectionId = oidcConnectionId(connectionParams);
    final Optional<OAuthConnectionDescriptor> oidcConnector =
        connectionsManager.resolve(project, oidcConnectionId);
    if (!oidcConnector.isPresent()) {
      throw new CredentialsUnavailableException(
          "The OIDC connection '" + oidcConnectionId + "' could not be resolved.");
    }
    // The connector's audience is the Octopus service account id, as at build time.
    final String audience =
        oidcConnector.get().getParameters().get(ConnectionPropertyNames.OIDC_CONNECTOR_AUDIENCE);
    if (audience == null || audience.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "The OIDC connection '" + oidcConnectionId + "' has no audience.");
    }
    try {
      final String idToken =
          idTokens.request(
              project, oidcConnectionId, buildType == null ? null : buildType.getExternalId());
      return tokenExchange.exchange(serverUrl, audience.trim(), idToken);
    } catch (final IOException e) {
      Loggers.SERVER.warn(
          "Could not get an Octopus access token from "
              + serverUrl
              + " through OIDC connection "
              + oidcConnectionId
              + " for project "
              + project.getExternalId(),
          e);
      throw new CredentialsUnavailableException(e.getMessage());
    }
  }

  private static String oidcConnectionId(final Map<String, String> connectionParams)
      throws CredentialsUnavailableException {
    final String oidcConnectionId =
        connectionParams.get(ConnectionPropertyNames.OIDC_CONNECTION_ID);
    if (oidcConnectionId == null || oidcConnectionId.trim().isEmpty()) {
      throw new CredentialsUnavailableException(
          "This connection does not name an OIDC connection.");
    }
    return oidcConnectionId.trim();
  }

  private static OidcIdTokens idTokensFor(final ApplicationContext applicationContext) {
    if (OidcIdTokenRequester.isSupported()) {
      return new OidcIdTokenRequester(applicationContext);
    }
    return (project, oidcConnectionId, buildTypeExternalId) -> {
      throw new IOException(OidcIdTokenRequester.UNSUPPORTED_MESSAGE);
    };
  }
}
