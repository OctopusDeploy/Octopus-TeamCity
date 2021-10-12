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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

/**
 * It should be noted that when TeamCity constructs a Properties Map, it removes leading whitespace
 * thus, a Server URL of " " - will be reduced to an empty string, which is then reduced to a
 * null/missing entry
 */
class OctopusConnectionPropertiesProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();

    result.put(ConnectionPropertyNames.SERVER_URL, "http://localhost:8065");
    result.put(ConnectionPropertyNames.API_KEY, "API-123456789012345678901234567890");
    result.put(ConnectionPropertyNames.PROXY_REQUIRED, "true");
    result.put(ConnectionPropertyNames.PROXY_URL, "http://proxy.url");
    result.put(ConnectionPropertyNames.PROXY_USERNAME, "ProxyUsername");
    result.put(ConnectionPropertyNames.PROXY_PASSWORD, "ProxyPassword");
    return result;
  }

  @Test
  public void aValidInputMapProducesNoInvalidEntries() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    assertThat(processor.process(inputMap)).hasSize(0);
  }

  @Test
  public void anEmptyListThrowsException() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    assertThatThrownBy(() -> processor.process(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void mandatoryFieldsMustBePopulated() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(ConnectionPropertyNames.SERVER_URL);
    inputMap.remove(ConnectionPropertyNames.API_KEY);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(2);
    final List<String> missingPropertyNames =
        result.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toList());
    assertThat(missingPropertyNames)
        .containsExactlyInAnyOrder(
            ConnectionPropertyNames.SERVER_URL, ConnectionPropertyNames.API_KEY);
  }

  @Test
  public void illegallyFormattedServerUrlReturnsASingleInvalidProperty() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(ConnectionPropertyNames.SERVER_URL, "badUrl");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  public void illegallyFormattedApiKeyReturnsASingleInvalidProperty() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(ConnectionPropertyNames.API_KEY, "API-1");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(ConnectionPropertyNames.API_KEY);
  }

  @Test
  public void spaceNameCanBeNull() {
    // Implies the default space should be used
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.SPACE_NAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void proxyUsernameAndPasswordCanBothBeNull() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(ConnectionPropertyNames.PROXY_PASSWORD);
    inputMap.remove(ConnectionPropertyNames.PROXY_USERNAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyPasswordIsSetWithoutUsername() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(ConnectionPropertyNames.PROXY_USERNAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(ConnectionPropertyNames.PROXY_USERNAME);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyUsernameIsSetWithoutPassword() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(ConnectionPropertyNames.PROXY_PASSWORD);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(ConnectionPropertyNames.PROXY_PASSWORD);
  }
}
