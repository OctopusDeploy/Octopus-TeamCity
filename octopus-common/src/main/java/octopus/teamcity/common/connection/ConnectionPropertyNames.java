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

package octopus.teamcity.common.connection;

import jetbrains.buildServer.agent.Constants;

public class ConnectionPropertyNames {
  // DisplayName is _required_ by TeamCity and must be called "displayName".
  public static final String DISPLAY_NAME = "displayName";
  public static final String SERVER_URL = "octopus_host";
  public static final String API_KEY = Constants.SECURE_PROPERTY_PREFIX + "octopus_apikey";
  public static final String VERSION = "octopus_version";
  public static final String SPACE_NAME = "octopus_space_name";

  // API key source — selects how the connection supplies credentials.
  public static final String API_KEY_SOURCE = "octopus_apikey_source";
  public static final String API_KEY_PARAMETER = "octopus_apikey_parameter";
  public static final String OIDC_CONNECTION_ID = "octopus_oidc_connection_id";

  // Source values (shared between the server build-start processor and the agent).
  public static final String API_KEY_SOURCE_KEY = "key";
  public static final String API_KEY_SOURCE_PARAMETER = "parameter";
  public static final String API_KEY_SOURCE_OIDC = "oidc";

  // The teamcity-oidc-plugin's OIDC connector, referenced by stable strings so this plugin
  // needs no compile-time dependency on it.
  public static final String OIDC_CONNECTOR_TYPE = "oidc-identity-token";
  public static final String OIDC_CONNECTOR_AUDIENCE = "audience";
  public static final String OIDC_CONNECTOR_TOKEN_VARIABLE_NAME = "token_variable_name";
  public static final String OIDC_DEFAULT_TOKEN_VARIABLE = "jwt.token";

  public ConnectionPropertyNames() {}

  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  public String getServerUrlPropertyName() {
    return SERVER_URL;
  }

  public String getApiKeyPropertyName() {
    return API_KEY;
  }

  public String getVersionPropertyName() {
    return VERSION;
  }

  public String getSpaceNamePropertyName() {
    return SPACE_NAME;
  }

  public String getApiKeySourcePropertyName() {
    return API_KEY_SOURCE;
  }

  public String getApiKeyParameterPropertyName() {
    return API_KEY_PARAMETER;
  }

  public String getOidcConnectionIdPropertyName() {
    return OIDC_CONNECTION_ID;
  }
}
