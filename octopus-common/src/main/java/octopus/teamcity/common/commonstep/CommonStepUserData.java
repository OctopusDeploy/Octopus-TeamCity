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

package octopus.teamcity.common.commonstep;

import java.util.Map;
import java.util.Optional;

/**
 * Assumes that the params passed in are correctly formatted and can be immediate converted to the
 * appropriate types (URL/Boolean), as the map was verified as part of the TeamCity
 * PropertiesValidator
 */
public class CommonStepUserData {
  private static final CommonStepPropertyKeys KEYS = new CommonStepPropertyKeys();
  private final Map<String, String> params;

  public CommonStepUserData(final Map<String, String> params) {
    this.params = params;
  }

  public String getStepType() {
    return params.getOrDefault(KEYS.getStepTypeKey(), "");
  }

  public String getServerUrl() {
    return params.getOrDefault(KEYS.getServerKey(), "");
  }

  public String getApiKey() {
    return params.getOrDefault(KEYS.getApiKey(), "");
  }

  public Optional<String> getSpaceName() {
    return Optional.ofNullable(params.get(KEYS.getSpaceNameKey()));
  }

  public boolean getProxyRequired() {
    return Boolean.getBoolean(params.get(KEYS.getProxyRequired()));
  }

  public String getProxyServerUrl() {
    return params.get(KEYS.getProxyServerUrlKey());
  }

  public String getProxyUsername() {
    return params.get(KEYS.getProxyUsernameKey());
  }

  public String getProxyPassword() {
    return params.get(KEYS.getProxyPasswordKey());
  }

  public boolean getVerboseLogging() {
    return Boolean.getBoolean(params.get(KEYS.getVerboseLoggingKey()));
  }
}
