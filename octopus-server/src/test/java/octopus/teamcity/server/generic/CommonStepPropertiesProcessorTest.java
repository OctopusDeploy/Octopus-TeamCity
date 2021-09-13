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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.buildinfo.BuildInfoKeys;
import octopus.teamcity.common.commonstep.CommonStepPropertyKeys;
import org.junit.jupiter.api.Test;

/**
 * It should be noted that when TeamCity constructs a Properties Map, it removes leading whitespace
 * thus, a Server URL of " " - will be reduced to an empty string, which is then reduced to a
 * null/missing entry
 */
class CommonStepPropertiesProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();

    result.put(CommonStepPropertyKeys.SERVER_URL, "http://localhost:8065");
    result.put(CommonStepPropertyKeys.API_KEY, "API-123456789012345678901234567890");
    result.put(CommonStepPropertyKeys.SPACE_NAME, "My Space");
    result.put(CommonStepPropertyKeys.PROXY_REQUIRED, "true");
    result.put(CommonStepPropertyKeys.PROXY_URL, "http://proxy.url");
    result.put(CommonStepPropertyKeys.PROXY_USERNAME, "ProxyUsername");
    result.put(CommonStepPropertyKeys.PROXY_PASSWORD, "ProxyPassword");
    result.put(CommonStepPropertyKeys.STEP_TYPE, new BuildInformationSubStepType().getName());
    result.put(CommonStepPropertyKeys.VERBOSE_LOGGING, "false");

    result.put(BuildInfoKeys.PACKAGE_IDS, "Package1\nPackage2");
    result.put(BuildInfoKeys.PACKAGE_VERSION, "1.0");

    return result;
  }

  @Test
  public void aValidInputMapProducesNoInvalidEntries() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    assertThat(processor.process(inputMap)).hasSize(0);
  }

  @Test
  public void anEmptyListThrowsException() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    assertThatThrownBy(() -> processor.process(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void missingStepTypeFieldThrowsIllegalArgumentException() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.STEP_TYPE);
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void stepTypeWhichDoesNotAlignWithAvailableBuildProcessesThrowsIllegalArgument() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyKeys.STEP_TYPE, "invalid-step-type");
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void mandatoryFieldsMustBePopulated() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.SERVER_URL);
    inputMap.remove(CommonStepPropertyKeys.API_KEY);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(2);
    final List<String> missingPropertyNames =
        result.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toList());
    assertThat(missingPropertyNames)
        .containsExactlyInAnyOrder(
            CommonStepPropertyKeys.SERVER_URL, CommonStepPropertyKeys.API_KEY);
  }

  @Test
  public void illegallyFormattedServerUrlReturnsASingleInvalidProperty() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyKeys.SERVER_URL, "badUrl");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyKeys.SERVER_URL);
  }

  @Test
  public void illegallyFormattedApiKeyReturnsASingleInvalidProperty() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyKeys.API_KEY, "API-1");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyKeys.API_KEY);
  }

  @Test
  public void spaceNameCanBeNull() {
    // Implies the default space should be used
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.SPACE_NAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void proxyUsernameAndPasswordCanBothBeNull() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.PROXY_PASSWORD);
    inputMap.remove(CommonStepPropertyKeys.PROXY_USERNAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyPasswordIsSetWithoutUsername() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.PROXY_USERNAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyKeys.PROXY_USERNAME);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyUsernameIsSetWithoutPassword() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.PROXY_PASSWORD);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyKeys.PROXY_PASSWORD);
  }
}
