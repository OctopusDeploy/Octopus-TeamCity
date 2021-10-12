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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import octopus.teamcity.server.OctopusGenericRunType;
import org.springframework.web.servlet.ModelAndView;

// This is responsible for handling the call http request for the OctopusBuildStep.
public class OctopusGenericRunTypeController extends BaseController {

  private final PluginDescriptor pluginDescriptor;

  public OctopusGenericRunTypeController(
      final WebControllerManager webControllerManager,
      final PluginDescriptor pluginDescriptor,
      final OctopusGenericRunType octopusGenericRunType) {
    this.pluginDescriptor = pluginDescriptor;

    webControllerManager.registerController(
        octopusGenericRunType.getEditRunnerParamsJspFilePath(), this);
  }

  @Override
  protected ModelAndView doHandle(
      final HttpServletRequest request, final HttpServletResponse response) {
    return new ModelAndView(
        pluginDescriptor.getPluginResourcesPath("v2" + File.separator + "editOctopusGeneric.jsp"));
  }
}
