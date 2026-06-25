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

import java.util.LinkedHashMap;
import java.util.Map;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.jetbrains.annotations.NotNull;

public class OctopusConnection extends OAuthProvider {

  public static final String TYPE = "OctopusConnection";

  private final PluginDescriptor pluginDescriptor;

  public OctopusConnection(final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Octopus Deploy";
  }

  @Override
  public String getEditParametersUrl() {
    return pluginDescriptor.getPluginResourcesPath("editOctopusConnection.jsp");
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultProperties() {
    final Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put(ConnectionPropertyNames.VERSION, "3.0+");
    defaults.put(ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_KEY);
    return defaults;
  }

  @NotNull
  @Override
  public String describeConnection(@NotNull final OAuthConnectionDescriptor connection) {
    final Map<String, String> params = connection.getParameters();
    final String url = params.getOrDefault(ConnectionPropertyNames.SERVER_URL, "(no URL)");
    final String version = params.getOrDefault(ConnectionPropertyNames.VERSION, "");
    final String space = params.getOrDefault(ConnectionPropertyNames.SPACE_NAME, "");
    // TeamCity renders newlines in a connection description as line breaks on the Connections tab.
    final StringBuilder description = new StringBuilder("Octopus URL: ").append(url);
    if (!version.isEmpty() && !version.equals("3.0+")) {
      description.append("\nVersion: ").append(version);
    }
    if (!space.isEmpty()) {
      description.append("\nSpace Name: ").append(space);
    }
    return description.toString();
  }

  @Override
  public PropertiesProcessor getPropertiesProcessor() {
    return new OctopusConnectionPropertiesProcessor();
  }
}
