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

import java.net.MalformedURLException;
import java.util.Map;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.connection.ConnectionUserData;

public class OctopusConnection extends OAuthProvider {

  public static final String TYPE = "OctopusConnection";

  private final PluginDescriptor pluginDescriptor;

  public OctopusConnection(final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getDisplayName() {
    return "OctopusDeploy Server";
  }

  @Override
  public String getEditParametersUrl() {
    return pluginDescriptor.getPluginResourcesPath("editOctopusConnection.jsp");
  }

  @Override
  public String describeConnection(final OAuthConnectionDescriptor connection) {
    final Map<String, String> params = connection.getParameters();
    final ConnectionUserData userData = new ConnectionUserData(params);
    try {
      return String.format("Octopus Server URL: %s", userData.getServerUrl().toString());
    } catch (MalformedURLException e) {
      return "Unable to decode entered parameters.";
    }
  }

  @Override
  public PropertiesProcessor getPropertiesProcessor() {
    return new OctopusConnectionPropertiesProcessor();
  }
}
