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

import com.google.common.collect.Lists;
import com.octopus.sdk.utils.ApiKeyValidator;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.server.generic.BuildStepCollection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("availableConnections")
public class OctopusServerParamPropertyProcessor implements PropertiesProcessor {
  private static final CommonStepPropertyNames KEYS = new CommonStepPropertyNames();

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    if (properties == null) {
      throw new IllegalArgumentException("Supplied properties list was null");
    }
    final List<InvalidProperty> result = Lists.newArrayList();

    validateServerUrl(properties, KEYS.getServerUrlPropertyName()).ifPresent(result::add);
    validateApiKey(properties, KEYS.getApiKeyPropertyName()).ifPresent(result::add);
    result.addAll(validateProxySettings(properties));

    return result;
  }

  private Optional<InvalidProperty> validateServerUrl(
      final Map<String, String> properties, final String propertyId) {
    final String serverUrl = properties.get(propertyId);

    if (serverUrl == null) {
      return Optional.of(new InvalidProperty(propertyId, "Server URL must be specified"));
    } else {
      try {
        final URL octopusServerURL = new URL(serverUrl);
        if (!octopusServerURL.getProtocol().equals("http")
            && !octopusServerURL.getProtocol().equals("https")) {
          return Optional.of(
              new InvalidProperty(
                  propertyId, "Server URL must specify specify http or https protocol"));
        }
      } catch (final MalformedURLException e) {
        final String errorMsg = "Illegally formatted URL - " + e.getLocalizedMessage();
        return Optional.of(new InvalidProperty(propertyId, errorMsg));
      }
    }
    return Optional.empty();
  }

  private Optional<InvalidProperty> validateApiKey(
      final Map<String, String> properties, final String propertyId) {
    final String apiKey = properties.get(propertyId);
    if (apiKey == null) {
      return Optional.of(new InvalidProperty(propertyId, "API key must be specified"));
    }

    try {
      ApiKeyValidator.validate(apiKey);
    } catch (final IllegalArgumentException e) {
      return Optional.of(new InvalidProperty(propertyId, e.getMessage()));
    }
    return Optional.empty();
  }

  private List<InvalidProperty> validateProxySettings(final Map<String, String> properties) {
    final List<InvalidProperty> result = Lists.newArrayList();
    final String proxyRequired = properties.get(KEYS.getProxyRequiredPropertyName());
    if (proxyRequired.equals("false")) {
      return result;
    }
    validateProxyServerUrl(properties, KEYS.getProxyServerUrlPropertyName()).ifPresent(result::add);
    validateProxyCredentials(properties).ifPresent(result::add);

    return result;
  }

  private Optional<InvalidProperty> validateProxyServerUrl(
      final Map<String, String> properties, final String propertyId) {
    final String proxyUrl = properties.get(propertyId);
    if (proxyUrl == null) {
      return Optional.of(new InvalidProperty(propertyId, "Proxy Server URL must be specified"));
    } else {
      try {
        new URL(proxyUrl);
      } catch (final MalformedURLException e) {
        return Optional.of(new InvalidProperty(propertyId, e.getLocalizedMessage()));
      }
    }
    return Optional.empty();
  }

  private Optional<InvalidProperty> validateProxyCredentials(final Map<String, String> properties) {
    final String proxyUsername = properties.get(KEYS.getProxyUsernamePropertyName());
    final String proxyPassword = properties.get(KEYS.getProxyPasswordPropertyName());

    if (proxyUsername == null && proxyPassword != null) {
      return Optional.of(
          new InvalidProperty(
              KEYS.getProxyUsernamePropertyName(),
              "Proxy username must be set if password is provided"));
    }

    if (proxyUsername != null && proxyPassword == null) {
      return Optional.of(
          new InvalidProperty(
              KEYS.getProxyPasswordPropertyName(),
              "Proxy password must be set if username is provided"));
    }

    return Optional.empty();
  }
}
