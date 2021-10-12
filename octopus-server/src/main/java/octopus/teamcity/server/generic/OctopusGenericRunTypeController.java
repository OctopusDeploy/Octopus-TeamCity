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

package octopus.teamcity.server.generic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import octopus.teamcity.server.OctopusGenericRunType;
import octopus.teamcity.server.connection.ConnectionHelper;
import octopus.teamcity.server.connection.OctopusConnectionsBean;
import org.springframework.web.servlet.ModelAndView;

// This is responsible for handling the call http request for the OctopusBuildStep.
public class OctopusGenericRunTypeController extends BaseController {

  private final PluginDescriptor pluginDescriptor;
  private final OAuthConnectionsManager oauthConnectionManager;
  private final ProjectManager projectManager;
  private final RelativeWebLinks webLinks;

  public OctopusGenericRunTypeController(
      final WebControllerManager webControllerManager,
      final PluginDescriptor pluginDescriptor,
      final OctopusGenericRunType octopusGenericRunType,
      final OAuthConnectionsManager oauthConnectionManager,
      final ProjectManager projectManager, final RelativeWebLinks webLinks) {
    this.pluginDescriptor = pluginDescriptor;
    this.oauthConnectionManager = oauthConnectionManager;
    this.projectManager = projectManager;
    this.webLinks = webLinks;

    webControllerManager.registerController(
        octopusGenericRunType.getEditRunnerParamsJspFilePath(), this);
  }

  @Override
  protected ModelAndView doHandle(
      final HttpServletRequest request, final HttpServletResponse response) throws IOException {
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
        ConnectionHelper.getAvailableOctopusConnections(
            oauthConnectionManager, projectManager, user);

    modelAndView.addObject("octopusConnections", new OctopusConnectionsBean(availableConnections));
    modelAndView.addObject("user", user);
    modelAndView.addObject("rootUrl", WebUtil.getRootUrl(request));
    modelAndView.addObject("rootProject", projectManager.getRootProject());
    modelAndView.addObject(
        "editConnectionUrl", webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections");

    return modelAndView;
  }
}
