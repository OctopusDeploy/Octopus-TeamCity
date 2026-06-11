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
}
