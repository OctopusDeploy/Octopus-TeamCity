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
import java.util.Collections;
import java.util.Map;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Requests an ID token from the OIDC Identity Token plugin through TeamCity's connection
 * credentials API.
 *
 * <p>The credentials API exists from TeamCity 2023.05, and this plugin still loads on older
 * servers. This is the only class that references the credentials API, and loading this class loads
 * the API's classes; create an instance only after {@link ConnectionCredentialsApi#isPresent()}
 * confirms the API is there.
 */
final class OidcIdTokenRequester implements OidcIdTokens {

  private static final String BUILD_TYPE_ID_PARAM = "oidc.edit.buildTypeId";
  private static final String ID_TOKEN = "id_token";
  private static final String UNSUPPORTED_PROVIDER = "Unsupported Connection Provider type";

  private final ApplicationContext context;

  OidcIdTokenRequester(final ApplicationContext context) {
    this.context = context;
  }

  @Override
  public String request(
      final SProject project,
      final String oidcConnectionId,
      @Nullable final String buildTypeExternalId)
      throws IOException {
    final Map<String, String> params =
        buildTypeExternalId == null
            ? Collections.emptyMap()
            : Collections.singletonMap(BUILD_TYPE_ID_PARAM, buildTypeExternalId);

    final ConnectionCredentials credentials;
    try {
      credentials =
          credentialsManager().requestConnectionCredentials(project, oidcConnectionId, params);
    } catch (final ConnectionCredentialsException e) {
      final String message = e.getMessage() == null ? "" : e.getMessage();
      if (message.startsWith(UNSUPPORTED_PROVIDER)) {
        throw new IOException(
            "Update the OIDC Identity Token plugin to list spaces for OIDC connections.", e);
      }
      throw new IOException(message, e);
    }

    final String idToken = credentials.getProperties().get(ID_TOKEN);
    if (idToken == null || idToken.isEmpty()) {
      throw new IOException("The OIDC Identity Token plugin did not return a token.");
    }
    return idToken;
  }

  private ProjectConnectionCredentialsManager credentialsManager() throws IOException {
    try {
      return context.getBean(ProjectConnectionCredentialsManager.class);
    } catch (final BeansException e) {
      throw new IOException(ConnectionCredentialsApi.UNSUPPORTED_MESSAGE, e);
    }
  }
}
