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

import octopus.teamcity.common.BaseUserData;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class ConnectionUserData  extends BaseUserData  {

  private static final ConnectionPropertyNames KEYS = new ConnectionPropertyNames();

  public ConnectionUserData(final Map<String, String> params) {
    super(params);
  }

  public URL getServerUrl() throws MalformedURLException {
    final String rawInput = fetchRaw(KEYS.getServerUrlPropertyName());
    return new URL(rawInput);
  }

  public String getApiKey() {
    return fetchRaw(KEYS.getApiKeyPropertyName());
  }

  public boolean getProxyRequired() {
    final String rawInput = fetchRaw(KEYS.getProxyRequiredPropertyName());
    return Boolean.getBoolean(rawInput);
  }

  public URL getProxyUrl() throws MalformedURLException {
    final String rawInput = fetchRaw(KEYS.getProxyUrlPropertyName());
    return new URL(rawInput);
  }

  public String getProxyUsername() {
    return fetchRaw(KEYS.getProxyUsernamePropertyName());
  }

  public String getProxyPassword() {
    return fetchRaw(KEYS.getProxyPasswordPropertyName());
  }

}
