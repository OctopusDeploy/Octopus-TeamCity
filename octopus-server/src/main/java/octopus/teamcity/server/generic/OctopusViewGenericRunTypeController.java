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

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.controllers.admin.projects.RunnerPropertiesBean;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.RelativeWebLinks;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import jetbrains.buildServer.web.util.WebUtil;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.server.OctopusGenericRunType;
import octopus.teamcity.server.connection.ConnectionHelper;
import octopus.teamcity.server.connection.OctopusConnectionsBean;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

// This is responsible for handling the call http request for the OctopusBuildStep.
public class OctopusViewGenericRunTypeController extends BaseController {

  private final PluginDescriptor pluginDescriptor;
  private final OAuthConnectionsManager oauthConnectionManager;
  private final ProjectManager projectManager;

  public OctopusViewGenericRunTypeController(
      final WebControllerManager webControllerManager,
      final PluginDescriptor pluginDescriptor,
      final OctopusGenericRunType octopusGenericRunType,
      final OAuthConnectionsManager oauthConnectionManager,
      final ProjectManager projectManager) {
    this.pluginDescriptor = pluginDescriptor;
    this.oauthConnectionManager = oauthConnectionManager;
    this.projectManager = projectManager;

    webControllerManager.registerController(
        octopusGenericRunType.getViewRunnerParamsJspFilePath(), this);
  }

  @Override
  protected ModelAndView doHandle(
      final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    final ModelAndView modelAndView =
        new ModelAndView(
            pluginDescriptor.getPluginResourcesPath(
                "v2" + File.separator + "viewOctopusGeneric.jsp"));

    final User user = SessionUser.getUser(request.getSession());
    if (user == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthenticated");
      return null;
    }

    final List<OAuthConnectionDescriptor> availableConnections =
        ConnectionHelper.getAvailableOctopusConnections(
            oauthConnectionManager, projectManager, user);

    final RunnerPropertiesBean propertiesBean = (RunnerPropertiesBean)request.getAttribute("propertiesBean");
    final String connectionId = propertiesBean.getProperties().get(CommonStepPropertyNames.CONNECTION_ID);
    final Optional<OAuthConnectionDescriptor> connection =
        availableConnections.stream().filter(conn -> conn.getId().equals(connectionId)).findFirst();

    if(connection.isPresent()) {
      propertiesBean.getProperties().putAll(connection.get().getParameters());
    }

    return modelAndView;
  }
}
