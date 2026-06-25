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

package octopus.teamcity.server.connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes the OIDC Identity Token connectors (type {@value
 * ConnectionPropertyNames#OIDC_CONNECTOR_TYPE}, provided by the teamcity-oidc-plugin) available to
 * the current project, for the connection edit form's OIDC dropdown. Follows the {@link
 * OctopusConnectionUiData} static-accessor pattern: a Spring bean captures collaborators into
 * static fields at startup so the JSP (which has no Spring context) can reach them. An empty list
 * means the OIDC option is not offered.
 */
public class OctopusOidcConnectorsUiData {
  private static volatile OAuthConnectionsManager oauthConnectionsManager;
  private static volatile ProjectManager projectManager;

  @SuppressWarnings(
      "StaticAssignmentInConstructor") // JSPs have no Spring context; capture statically.
  public OctopusOidcConnectorsUiData(
      final OAuthConnectionsManager oauthConnectionsManager, final ProjectManager projectManager) {
    OctopusOidcConnectorsUiData.oauthConnectionsManager = oauthConnectionsManager;
    OctopusOidcConnectorsUiData.projectManager = projectManager;
  }

  /** OIDC connectors ({@code id}, {@code displayName}) visible to the request's project. */
  @NotNull
  public static List<Map<String, String>> availableConnectors(final HttpServletRequest request) {
    final List<Map<String, String>> result = new ArrayList<>();
    if (oauthConnectionsManager == null || projectManager == null) {
      return result;
    }
    final SProject project = currentProject(request);
    if (project == null) {
      return result;
    }
    for (final OAuthConnectionDescriptor descriptor :
        oauthConnectionsManager.getAvailableConnectionsOfType(
            project, ConnectionPropertyNames.OIDC_CONNECTOR_TYPE)) {
      final Map<String, String> view = new HashMap<>();
      view.put("id", descriptor.getId());
      view.put("displayName", descriptor.getConnectionDisplayName());
      result.add(view);
    }
    return result;
  }

  private static SProject currentProject(final HttpServletRequest request) {
    final String projectExternalId = request.getParameter("projectId");
    if (projectExternalId == null) {
      return null;
    }
    return projectManager.findProjectByExternalId(projectExternalId);
  }
}
