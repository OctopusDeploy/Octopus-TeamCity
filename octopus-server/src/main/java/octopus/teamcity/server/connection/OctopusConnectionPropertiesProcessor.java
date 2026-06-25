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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

public class OctopusConnectionPropertiesProcessor implements PropertiesProcessor {
  private static final ConnectionPropertyNames KEYS = new ConnectionPropertyNames();
  private static final Set<String> KNOWN_VERSIONS =
      new HashSet<>(Arrays.asList(OctopusConstants.Instance.getOctopusVersions()));

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    final List<InvalidProperty> result = new ArrayList<>();
    if (properties == null) {
      return result;
    }

    validateServerUrl(properties.get(KEYS.getServerUrlPropertyName()), result);
    validateVersion(properties.get(KEYS.getVersionPropertyName()), result);

    final String source =
        properties.getOrDefault(
            KEYS.getApiKeySourcePropertyName(), ConnectionPropertyNames.API_KEY_SOURCE_KEY);

    if (ConnectionPropertyNames.API_KEY_SOURCE_PARAMETER.equals(source)) {
      validateParameterReference(properties.get(KEYS.getApiKeyParameterPropertyName()), result);
    } else if (ConnectionPropertyNames.API_KEY_SOURCE_OIDC.equals(source)) {
      if (StringUtil.isEmptyOrSpaces(properties.get(KEYS.getOidcConnectionIdPropertyName()))) {
        result.add(
            new InvalidProperty(
                KEYS.getOidcConnectionIdPropertyName(), "An OIDC connector must be selected"));
      }
    } else { // key (default / unknown)
      if (StringUtil.isEmptyOrSpaces(properties.get(KEYS.getApiKeyPropertyName()))) {
        result.add(new InvalidProperty(KEYS.getApiKeyPropertyName(), "API key must be specified"));
      }
    }

    return result;
  }

  private void validateParameterReference(final String value, final List<InvalidProperty> result) {
    final String id = KEYS.getApiKeyParameterPropertyName();
    if (StringUtil.isEmptyOrSpaces(value)) {
      result.add(new InvalidProperty(id, "A parameter reference must be specified"));
      return;
    }
    final String trimmed = value.trim();
    final boolean singleReference =
        trimmed.length() > 2
            && trimmed.startsWith("%")
            && trimmed.endsWith("%")
            && trimmed.indexOf('%', 1) == trimmed.length() - 1;
    if (!singleReference) {
      result.add(
          new InvalidProperty(id, "Must be a single parameter reference, e.g. %octopus.apikey%"));
    }
  }

  private void validateServerUrl(final String serverUrl, final List<InvalidProperty> result) {
    final String id = KEYS.getServerUrlPropertyName();
    if (StringUtil.isEmptyOrSpaces(serverUrl)) {
      result.add(new InvalidProperty(id, "Server URL must be specified"));
      return;
    }
    try {
      final URL url = new URL(serverUrl);
      if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
        result.add(new InvalidProperty(id, "Server URL must use the http or https protocol"));
      }
    } catch (final MalformedURLException e) {
      result.add(new InvalidProperty(id, "Malformed URL - " + e.getLocalizedMessage()));
    }
  }

  private void validateVersion(final String version, final List<InvalidProperty> result) {
    final String id = KEYS.getVersionPropertyName();
    if (StringUtil.isEmptyOrSpaces(version)) {
      result.add(new InvalidProperty(id, "Octopus version must be specified"));
      return;
    }
    final boolean known = KNOWN_VERSIONS.contains(version);
    if (!known) {
      result.add(new InvalidProperty(id, "Unknown Octopus version: " + version));
    }
  }
}
