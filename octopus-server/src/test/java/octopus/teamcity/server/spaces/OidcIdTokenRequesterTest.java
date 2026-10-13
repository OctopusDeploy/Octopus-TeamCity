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
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentials;
import jetbrains.buildServer.serverSide.connections.credentials.ConnectionCredentialsException;
import jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

class OidcIdTokenRequesterTest {

  private final ApplicationContext context = mock(ApplicationContext.class);
  private final ProjectConnectionCredentialsManager credentialsManager =
      mock(ProjectConnectionCredentialsManager.class);
  private final SProject project = mock(SProject.class);

  private OidcIdTokenRequester requester() {
    when(context.getBean(ProjectConnectionCredentialsManager.class)).thenReturn(credentialsManager);
    return new OidcIdTokenRequester(context);
  }

  private static ConnectionCredentials credentialsWith(final Map<String, String> properties) {
    final ConnectionCredentials credentials = mock(ConnectionCredentials.class);
    when(credentials.getProperties()).thenReturn(properties);
    return credentials;
  }

  @Test
  void isSupportedWhenTheCredentialsApiIsOnTheClasspath() {
    assertThat(OidcIdTokenRequester.isSupported()).isTrue();
  }

  @Test
  void requestsATokenForTheBuildConfiguration() throws Exception {
    final ConnectionCredentials credentials =
        credentialsWith(Collections.singletonMap("id_token", "id.token.value"));
    when(credentialsManager.requestConnectionCredentials(eq(project), eq("oidc-1"), anyMap()))
        .thenReturn(credentials);

    final String token = requester().request(project, "oidc-1", "MyProject_Deploy");

    assertThat(token).isEqualTo("id.token.value");
    verify(credentialsManager)
        .requestConnectionCredentials(
            project,
            "oidc-1",
            Collections.singletonMap("oidc.edit.buildTypeId", "MyProject_Deploy"));
  }

  @Test
  void requestsAProjectScopedTokenWithoutABuildConfiguration() throws Exception {
    final ConnectionCredentials credentials =
        credentialsWith(Collections.singletonMap("id_token", "id.token.value"));
    when(credentialsManager.requestConnectionCredentials(eq(project), eq("oidc-1"), anyMap()))
        .thenReturn(credentials);

    requester().request(project, "oidc-1", null);

    verify(credentialsManager)
        .requestConnectionCredentials(project, "oidc-1", Collections.emptyMap());
  }

  @Test
  void passesTheOidcPluginsRefusalThrough() throws Exception {
    when(credentialsManager.requestConnectionCredentials(any(), any(), anyMap()))
        .thenThrow(new ConnectionCredentialsException("You do not have permission."));

    assertThatThrownBy(() -> requester().request(project, "oidc-1", null))
        .isInstanceOf(IOException.class)
        .hasMessage("You do not have permission.");
  }

  @Test
  void asksForANewerOidcPluginWhenItCannotIssueTokensOutsideABuild() throws Exception {
    when(credentialsManager.requestConnectionCredentials(any(), any(), anyMap()))
        .thenThrow(
            new ConnectionCredentialsException(
                "Unsupported Connection Provider type: oidc-identity-token"));

    assertThatThrownBy(() -> requester().request(project, "oidc-1", null))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Update the OIDC Identity Token plugin");
  }

  @Test
  void failsClearlyWhenNoTokenComesBack() throws Exception {
    final ConnectionCredentials credentials = credentialsWith(Collections.emptyMap());
    when(credentialsManager.requestConnectionCredentials(any(), any(), anyMap()))
        .thenReturn(credentials);

    assertThatThrownBy(() -> requester().request(project, "oidc-1", null))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("did not return a token");
  }

  @Test
  void failsClearlyWhenTheServerHasNoCredentialsManager() {
    when(context.getBean(ProjectConnectionCredentialsManager.class))
        .thenThrow(new NoSuchBeanDefinitionException(ProjectConnectionCredentialsManager.class));

    assertThatThrownBy(() -> new OidcIdTokenRequester(context).request(project, "oidc-1", null))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("cannot issue OIDC tokens outside a build");
  }
}
