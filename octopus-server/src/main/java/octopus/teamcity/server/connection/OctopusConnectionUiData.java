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
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.jetbrains.annotations.NotNull;

/**
 * Exposes the available Octopus connections to the step edit JSPs via a static accessor.
 *
 * <p>TeamCity renders a runner's parameter form by server-side including the JSP returned from
 * {@code getEditRunnerParamsJspFilePath()}. A controller registered at that path is not reliably
 * invoked for that include, so — like the teamcity-oidc-plugin — the JSP itself pulls the data it
 * needs through this static accessor. A Spring bean of this type captures the collaborators into
 * static fields at startup so the JSP (which has no Spring context) can reach them.
 */
public class OctopusConnectionUiData {
  private static final ConnectionPropertyNames CONNECTION_KEYS = new ConnectionPropertyNames();

  private static volatile OctopusConnectionsManager connectionsManager;
  private static volatile ProjectManager projectManager;

  @SuppressWarnings(
      "StaticAssignmentInConstructor") // JSPs have no Spring context; capture statically.
  public OctopusConnectionUiData(
      final OctopusConnectionsManager connectionsManager, final ProjectManager projectManager) {
    OctopusConnectionUiData.connectionsManager = connectionsManager;
    OctopusConnectionUiData.projectManager = projectManager;
  }

  /**
   * Connections available to the current user ({@code id}, {@code displayName}, {@code url}, {@code
   * version}, {@code space}) for the JSP. API key is deliberately not included.
   */
  @NotNull
  public static List<Map<String, String>> availableConnections(final HttpServletRequest request) {
    return availableConnections(SessionUser.getUser(request));
  }

  /** As {@link #availableConnections(HttpServletRequest)} but for an already-resolved user. */
  static List<Map<String, String>> availableConnections(final SUser user) {
    final List<Map<String, String>> result = new ArrayList<>();
    if (user == null || connectionsManager == null) {
      return result;
    }
    for (final OAuthConnectionDescriptor descriptor :
        connectionsManager.listAvailableConnections(user)) {
      final Map<String, String> params = descriptor.getParameters();
      final Map<String, String> view = new HashMap<>();
      view.put("id", descriptor.getId());
      view.put("displayName", descriptor.getConnectionDisplayName());
      view.put("url", params.getOrDefault(CONNECTION_KEYS.getServerUrlPropertyName(), ""));
      view.put("version", params.getOrDefault(CONNECTION_KEYS.getVersionPropertyName(), ""));
      view.put("space", params.getOrDefault(CONNECTION_KEYS.getSpaceNamePropertyName(), ""));
      result.add(view);
    }
    return result;
  }

  /**
   * URL of the current project's Connections tab (where connections are added). Built relative to
   * the request's context path so the browser resolves it against the host it is actually viewing —
   * avoiding a stale/absolute server root URL (wrong port, {@code init=1}). Falls back to the
   * Connections tab without a project id if the current project cannot be resolved.
   */
  public static String editConnectionUrl(final HttpServletRequest request) {
    final String base = request.getContextPath() + "/admin/editProject.html";
    final String projectExternalId = currentProjectExternalId(request);
    return projectExternalId == null
        ? base + "?tab=oauthConnections"
        : base + "?projectId=" + projectExternalId + "&tab=oauthConnections";
  }

  private static String currentProjectExternalId(final HttpServletRequest request) {
    if (projectManager == null) {
      return null;
    }
    final String idParam = request.getParameter("id");
    if (idParam == null || !idParam.startsWith("buildType:")) {
      return null;
    }
    final SBuildType buildType =
        projectManager.findBuildTypeByExternalId(idParam.substring("buildType:".length()));
    return buildType == null ? null : buildType.getProject().getExternalId();
  }
}
