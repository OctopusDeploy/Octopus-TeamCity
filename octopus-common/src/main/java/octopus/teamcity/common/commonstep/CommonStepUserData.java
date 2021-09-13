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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import octopus.teamcity.common.BaseUserData;

/**
 * Assumes that the params passed in are correctly formatted and can be immediate converted to the
 * appropriate types (URL/Boolean), as the map was verified as part of the TeamCity
 * PropertiesValidator
 */
public class CommonStepUserData extends BaseUserData {

  private static final CommonStepPropertyKeys KEYS = new CommonStepPropertyKeys();

  public CommonStepUserData(final Map<String, String> params) {
    super(params);
  }

  public String getStepType() {
    return fetchRaw(KEYS.getStepTypeKey());
  }

  public URL getServerUrl() throws MalformedURLException {
    final String rawInput = fetchRaw(KEYS.getServerUrlKey());
    return new URL(rawInput);
  }

  public String getApiKey() {
    return fetchRaw(KEYS.getApiKeyKey());
  }

  public Optional<String> getSpaceName() {
    return Optional.ofNullable(params.get(KEYS.getSpaceNameKey()));
  }

  public boolean getProxyRequired() {
    final String rawInput = fetchRaw(KEYS.getProxyRequiredKey());
    return Boolean.getBoolean(rawInput);
  }

  public URL getProxyServerUrl() throws MalformedURLException {
    final String rawInput = fetchRaw(KEYS.getProxyServerUrlKey());
    return new URL(rawInput);
  }

  public String getProxyUsername() {
    return fetchRaw(KEYS.getProxyUsernameKey());
  }

  public String getProxyPassword() {
    return fetchRaw(KEYS.getProxyPasswordKey());
  }

  public boolean getVerboseLogging() {
    final String rawInput = fetchRaw(KEYS.getVerboseLoggingKey());
    return Boolean.getBoolean(rawInput);
  }
}
