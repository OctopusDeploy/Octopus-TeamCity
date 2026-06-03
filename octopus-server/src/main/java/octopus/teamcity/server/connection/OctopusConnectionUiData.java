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

import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

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
  private static final ConnectionPropertyNames CONN = new ConnectionPropertyNames();

  private static volatile OctopusConnectionsManager connectionsManager;
  private static volatile WebLinks webLinks;

  @SuppressWarnings(
      "StaticAssignmentInConstructor") // JSPs have no Spring context; capture statically.
  public OctopusConnectionUiData(
      final OctopusConnectionsManager connectionsManager, final WebLinks webLinks) {
    OctopusConnectionUiData.connectionsManager = connectionsManager;
    OctopusConnectionUiData.webLinks = webLinks;
  }

  /**
   * Connections available to the current user, as simple maps ({@code id}, {@code displayName},
   * {@code url}, {@code version}, {@code space}) for the JSP. Never returns null. The API key is
   * deliberately not included.
   */
  public static List<Map<String, String>> availableConnections(final HttpServletRequest request) {
    return availableConnections(SessionUser.getUser(request));
  }

  /** As {@link #availableConnections(HttpServletRequest)} but for an already-resolved user. */
  static List<Map<String, String>> availableConnections(final SUser user) {
    final List<Map<String, String>> result = new ArrayList<>();
    if (user == null || connectionsManager == null) {
      return result;
    }
    for (final OAuthConnectionDescriptor d : connectionsManager.listAvailableConnections(user)) {
      final Map<String, String> params = d.getParameters();
      final Map<String, String> view = new HashMap<>();
      view.put("id", d.getId());
      view.put("displayName", d.getConnectionDisplayName());
      view.put("url", params.getOrDefault(CONN.getServerUrlPropertyName(), ""));
      view.put("version", params.getOrDefault(CONN.getVersionPropertyName(), ""));
      view.put("space", params.getOrDefault(CONN.getSpaceNamePropertyName(), ""));
      result.add(view);
    }
    return result;
  }

  /** URL of the Root project's Connections tab (where connections are added). */
  public static String editConnectionUrl() {
    return webLinks == null
        ? "#"
        : webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections";
  }
}
