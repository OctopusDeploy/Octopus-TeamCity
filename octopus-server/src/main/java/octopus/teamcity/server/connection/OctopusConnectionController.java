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
import javax.servlet.http.HttpServletResponse;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

public class OctopusConnectionController extends BaseController {
  private static final Logger LOG = Loggers.SERVER;
  private static final ConnectionPropertyNames CONN = new ConnectionPropertyNames();

  // Maps the registered controller path (the run type's edit path) -> physical form JSP.
  private static final Map<String, String> PATH_TO_FORM = new HashMap<>();

  static {
    PATH_TO_FORM.put("editOctopusCreateRelease.jsp", "forms/editOctopusCreateReleaseForm.jsp");
    PATH_TO_FORM.put("editOctopusDeployRelease.jsp", "forms/editOctopusDeployReleaseForm.jsp");
    PATH_TO_FORM.put("editOctopusPromoteRelease.jsp", "forms/editOctopusPromoteReleaseForm.jsp");
    PATH_TO_FORM.put("editOctopusPushPackage.jsp", "forms/editOctopusPushPackageForm.jsp");
    PATH_TO_FORM.put("editOctopusBuildInformation.jsp", "forms/editOctopusBuildInformationForm.jsp");
  }

  private final OctopusConnectionsManager connectionsManager;
  private final PluginDescriptor pluginDescriptor;
  private final WebLinks webLinks;

  public OctopusConnectionController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final PluginDescriptor pluginDescriptor,
      final SBuildServer server,
      final WebLinks webLinks) {
    super(server);
    this.connectionsManager = connectionsManager;
    this.pluginDescriptor = pluginDescriptor;
    this.webLinks = webLinks;

    for (final String path : PATH_TO_FORM.keySet()) {
      webControllerManager.registerController(
          pluginDescriptor.getPluginResourcesPath(path), this);
    }
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(
      @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response)
      throws Exception {
    final String pathInfo = request.getPathInfo();
    final String lastSegment =
        pathInfo == null ? null : pathInfo.substring(pathInfo.lastIndexOf('/') + 1);
    final String formJsp = lastSegment == null ? null : PATH_TO_FORM.get(lastSegment);
    if (formJsp == null) {
      // This controller is only registered against the five known edit-JSP paths, so an
      // unmatched path means TeamCity routed here unexpectedly. Log and let TC continue
      // rather than silently serving the wrong form.
      LOG.warn(
          "OctopusConnectionController invoked for unmapped path: " + pathInfo);
      return null;
    }

    final ModelAndView modelAndView =
        new ModelAndView(pluginDescriptor.getPluginResourcesPath(formJsp));

    final SUser user = SessionUser.getUser(request);
    final List<Map<String, String>> connections = new ArrayList<>();
    if (user != null) {
      for (final OAuthConnectionDescriptor d : connectionsManager.listAvailableConnections(user)) {
        final Map<String, String> params = d.getParameters();
        final Map<String, String> view = new HashMap<>();
        view.put("id", d.getId());
        view.put("displayName", d.getConnectionDisplayName());
        view.put("url", params.getOrDefault(CONN.getServerUrlPropertyName(), ""));
        view.put("version", params.getOrDefault(CONN.getVersionPropertyName(), ""));
        view.put("space", params.getOrDefault(CONN.getSpaceNamePropertyName(), ""));
        connections.add(view);
      }
    }

    modelAndView.addObject("octopusConnections", connections);
    // Link to the Root project's Connections tab; connections defined there are inherited by
    // all projects. (Connections on sub-projects are still listed in the dropdown.)
    modelAndView.addObject(
        "editConnectionUrl",
        webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections");
    return modelAndView;
  }
}
