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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyKeys;

public class CommonStepPropertiesProcessor implements PropertiesProcessor {
  private static final CommonStepPropertyKeys KEYS = new CommonStepPropertyKeys();

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    if (properties == null) {
      throw new IllegalArgumentException("Supplied properties list was null");
    }

    final String stepType = properties.getOrDefault(KEYS.getStepTypeKey(), "");
    if (stepType.isEmpty()) {
      throw new IllegalArgumentException("No step-type was specified, contact Octopus support");
    }

    final List<InvalidProperty> result = Lists.newArrayList();

    validateServerUrl(properties, KEYS.getServerKey()).ifPresent(result::add);
    validateApiKey(properties, KEYS.getApiKey()).ifPresent(result::add);
    result.addAll(validateProxySettings(properties));

    final SubStepCollection subStepCollection = new SubStepCollection();
    result.addAll(
        subStepCollection.getSubSteps().stream()
            .filter(cmd -> cmd.getName().equals(stepType))
            .findFirst()
            .map(cmd -> cmd.validateProperties(properties))
            .orElseThrow(
                () -> new IllegalArgumentException("No matching validation for selected command")));

    // TODO:tmm Prevent usage of the Step Vnext until agent-side is complete (this can then be
    // removed
    result.add(
        new InvalidProperty(
            KEYS.getStepTypeKey(),
            "Octopus' generic build runner is not yet ready for use in a build process"));

    return result;
  }

  // TODO(tmm): This needs to be moved into the SDK, such that it can be reused across CI systems
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
    } else {
      if (!apiKey.startsWith("API-") || apiKey.length() < 29 || apiKey.length() > 36) {
        return Optional.of(
            new InvalidProperty(
                propertyId,
                "API does not conform to expected convention (API-XXXX), and between 29 and 36 characters in total."));
      }
    }
    return Optional.empty();
  }

  private List<InvalidProperty> validateProxySettings(final Map<String, String> properties) {
    final List<InvalidProperty> result = Lists.newArrayList();
    final String proxyRequired = properties.get(KEYS.getProxyRequired());
    if (proxyRequired.equals("false")) {
      return result;
    }
    validateProxyServerUrl(properties, KEYS.getProxyServerUrlKey()).ifPresent(result::add);
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
    final String proxyUsername = properties.get(KEYS.getProxyUsernameKey());
    final String proxyPassword = properties.get(KEYS.getProxyPasswordKey());

    if (proxyUsername == null && proxyPassword != null) {
      return Optional.of(
          new InvalidProperty(
              KEYS.getProxyUsernameKey(), "Proxy username must be set if password is provided"));
    }

    if (proxyUsername != null && proxyPassword == null) {
      return Optional.of(
          new InvalidProperty(
              KEYS.getProxyPasswordKey(), "Proxy password must be set if username is provided"));
    }

    return Optional.empty();
  }
}
