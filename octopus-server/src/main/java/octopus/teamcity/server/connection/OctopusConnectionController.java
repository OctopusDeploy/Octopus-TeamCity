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

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import octopus.teamcity.server.OctopusGenericRunType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

// This is responsible for handling the call http request for the OctopusBuildStep.
// It adds all OctopusConnections to the request, along with metadata, such that the JSP
// can display information about the available connections.
public class OctopusConnectionController extends BaseController {

  private final OAuthConnectionsManager oauthConnectionManager;
  private final ProjectManager projectManager;
  private final PluginDescriptor pluginDescriptor;
  private final WebLinks webLinks;

  public OctopusConnectionController(
      final WebControllerManager webControllerManager,
      final OAuthConnectionsManager oauthConnectionManager,
      final ProjectManager projectManager,
      final PluginDescriptor pluginDescriptor,
      final OctopusGenericRunType octopusGenericRunType,
      final SBuildServer myServer,
      final WebLinks webLinks) {
    super(myServer);
    this.oauthConnectionManager = oauthConnectionManager;
    this.projectManager = projectManager;
    this.pluginDescriptor = pluginDescriptor;
    this.webLinks = webLinks;

    final String path = octopusGenericRunType.getEditRunnerParamsJspFilePath();
    webControllerManager.registerController(path, this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(
      @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response)
      throws Exception {
    final ModelAndView modelAndView =
        new ModelAndView(
            pluginDescriptor.getPluginResourcesPath(
                "v2" + File.separator + "editOctopusGeneric.jsp"));

    final User user = SessionUser.getUser(request.getSession());
    if (user == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthenticated");
      return null;
    }

    final List<OAuthConnectionDescriptor> availableConnections =
        projectManager.getProjects().stream()
            .filter(
                p -> user.isPermissionGrantedForProject(p.getProjectId(), Permission.VIEW_PROJECT))
            .map(
                p ->
                    oauthConnectionManager.getAvailableConnectionsOfType(p, OctopusConnection.TYPE))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    modelAndView.addObject("octopusConnections", new OctopusConnectionsBean(availableConnections));
    modelAndView.addObject("user", user);
    modelAndView.addObject("rootUrl", WebUtil.getRootUrl(request));
    modelAndView.addObject("rootProject", projectManager.getRootProject());
    modelAndView.addObject(
        "editConnectionUrl", webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections");

    return modelAndView;
  }
}
