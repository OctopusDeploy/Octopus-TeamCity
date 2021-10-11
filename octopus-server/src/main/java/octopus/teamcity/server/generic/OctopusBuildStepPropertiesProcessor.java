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

package octopus.teamcity.server.generic;

import com.octopus.sdk.utils.ApiKeyValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

public class OctopusBuildStepPropertiesProcessor implements PropertiesProcessor {
  private static final CommonStepPropertyNames KEYS = new CommonStepPropertyNames();
  private static final ConnectionPropertyNames CONNECTION_KEYS = new ConnectionPropertyNames();

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    if (properties == null) {
      throw new IllegalArgumentException("Supplied properties list was null");
    }

    final String stepType = properties.get(KEYS.getStepTypePropertyName());
    if (stepType == null) {
      throw new IllegalArgumentException("No step-type was specified, contact Octopus support");
    }

    final List<InvalidProperty> result = Lists.newArrayList();

    validateServerUrl(properties, CONNECTION_KEYS.getServerUrlPropertyName())
        .ifPresent(result::add);
    validateApiKey(properties, CONNECTION_KEYS.getApiKeyPropertyName()).ifPresent(result::add);
    result.addAll(validateProxySettings(properties));

    final BuildStepCollection buildStepCollection = new BuildStepCollection();
    result.addAll(
        buildStepCollection.getSubSteps().stream()
            .filter(cmd -> cmd.getName().equals(stepType))
            .findFirst()
            .map(cmd -> cmd.validateProperties(properties))
            .orElseThrow(
                () -> new IllegalArgumentException("No matching validation for selected command")));

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
    final String proxyRequired = properties.get(CONNECTION_KEYS.getProxyRequiredPropertyName());
    if (proxyRequired.equals("false")) {
      return result;
    }
    validateProxyServerUrl(properties, CONNECTION_KEYS.getProxyServerUrlPropertyName())
        .ifPresent(result::add);
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
    final String proxyUsername = properties.get(CONNECTION_KEYS.getProxyUsernamePropertyName());
    final String proxyPassword = properties.get(CONNECTION_KEYS.getProxyPasswordPropertyName());

    if (proxyUsername == null && proxyPassword != null) {
      return Optional.of(
          new InvalidProperty(
              CONNECTION_KEYS.getProxyUsernamePropertyName(),
              "Proxy username must be set if password is provided"));
    }

    if (proxyUsername != null && proxyPassword == null) {
      return Optional.of(
          new InvalidProperty(
              CONNECTION_KEYS.getProxyPasswordPropertyName(),
              "Proxy password must be set if username is provided"));
    }

    return Optional.empty();
  }
}
