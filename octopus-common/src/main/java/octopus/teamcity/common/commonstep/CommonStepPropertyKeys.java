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

import jetbrains.buildServer.agent.Constants;

public class CommonStepPropertyKeys {

  public enum Keys {
    STEP_TYPE("octopus_step_type"),
    SERVER_URL("octopus_host"),
    API_KEY(Constants.SECURE_PROPERTY_PREFIX + "octopus_apikey"),
    SPACE_NAME("octopus_spacename"),
    PROXY_REQUIRED("octopus_proxyrequired"),
    PROXY_URL("octopus_proxyurl"),
    PROXY_USERNAME("octopus_proxyusername"),
    PROXY_PASSWORD(Constants.SECURE_PROPERTY_PREFIX + "octopus_proxypassword"),
    VERBOSE_LOGGING("octopus_verboselogging");

    private final String keyString;

    Keys(final String keyString) {
      this.keyString = keyString;
    }

    public String getKeyString() {
      return keyString;
    }
  }

  public CommonStepPropertyKeys() {}

  public String getStepTypeKey() {
    return Keys.STEP_TYPE.keyString;
  }

  public String getServerKey() {
    return Keys.SERVER_URL.keyString;
  }

  public String getApiKey() {
    return Keys.API_KEY.keyString;
  }

  public String getSpaceNameKey() {
    return Keys.SPACE_NAME.keyString;
  }

  public String getProxyRequired() {
    return Keys.PROXY_REQUIRED.keyString;
  }

  public String getProxyServerUrlKey() {
    return Keys.PROXY_URL.keyString;
  }

  public String getProxyUsernameKey() {
    return Keys.PROXY_USERNAME.keyString;
  }

  public String getProxyPasswordKey() {
    return Keys.PROXY_PASSWORD.keyString;
  }

  public String getVerboseLoggingKey() {
    return Keys.VERBOSE_LOGGING.keyString;
  }
}
