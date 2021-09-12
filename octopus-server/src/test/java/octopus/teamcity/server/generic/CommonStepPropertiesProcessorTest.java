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

    result.put(CommonStepPropertyKeys.Keys.SERVER_URL.getKeyString(), "http://localhost:8065");
    result.put(
        CommonStepPropertyKeys.Keys.API_KEY.getKeyString(), "API-123456789012345678901234567890");
    result.put(CommonStepPropertyKeys.Keys.SPACE_NAME.getKeyString(), "My Space");
    result.put(CommonStepPropertyKeys.Keys.PROXY_REQUIRED.getKeyString(), "true");
    result.put(CommonStepPropertyKeys.Keys.PROXY_URL.getKeyString(), "http://proxy.url");
    result.put(CommonStepPropertyKeys.Keys.PROXY_USERNAME.getKeyString(), "ProxyUsername");
    result.put(CommonStepPropertyKeys.Keys.PROXY_PASSWORD.getKeyString(), "ProxyPassword");
    result.put(
        CommonStepPropertyKeys.Keys.STEP_TYPE.getKeyString(),
        new BuildInformationSubStepType().getName());
    result.put(CommonStepPropertyKeys.Keys.VERBOSE_LOGGING.getKeyString(), "false");

    result.put(BuildInfoKeys.Keys.PACKAGE_IDS.getKeyString(), "Package1\nPackage2");
    result.put(BuildInfoKeys.Keys.PACKAGE_VERSION.getKeyString(), "1.0");

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

    inputMap.remove(CommonStepPropertyKeys.Keys.STEP_TYPE.getKeyString());
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void stepTypeWhichDoesNotAlignWithAvailableBuildProcessesThrowsIllegalArgument() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyKeys.Keys.STEP_TYPE.getKeyString(), "invalid-step-type");
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void mandatoryFieldsMustBePopulated() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.Keys.SERVER_URL.getKeyString());
    inputMap.remove(CommonStepPropertyKeys.Keys.API_KEY.getKeyString());
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(2);
    final List<String> missingPropertyNames =
        result.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toList());
    assertThat(missingPropertyNames)
        .containsExactlyInAnyOrder(
            CommonStepPropertyKeys.Keys.SERVER_URL.getKeyString(),
            CommonStepPropertyKeys.Keys.API_KEY.getKeyString());
  }

  @Test
  public void illegallyFormattedServerUrlReturnsASingleInvalidProperty() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyKeys.Keys.SERVER_URL.getKeyString(), "badUrl");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(CommonStepPropertyKeys.Keys.SERVER_URL.getKeyString());
  }

  @Test
  public void illegallyFormattedApiKeyReturnsASingleInvalidProperty() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyKeys.Keys.API_KEY.getKeyString(), "API-1");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(CommonStepPropertyKeys.Keys.API_KEY.getKeyString());
  }

  @Test
  public void spaceNameCanBeNull() {
    // Implies the default space should be used
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.Keys.SPACE_NAME.getKeyString());
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void proxyUsernameAndPasswordCanBothBeNull() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.Keys.PROXY_PASSWORD.getKeyString());
    inputMap.remove(CommonStepPropertyKeys.Keys.PROXY_USERNAME.getKeyString());
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyPasswordIsSetWithoutUsername() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.Keys.PROXY_USERNAME.getKeyString());
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(CommonStepPropertyKeys.Keys.PROXY_USERNAME.getKeyString());
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyUsernameIsSetWithoutPassword() {
    final CommonStepPropertiesProcessor processor = new CommonStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyKeys.Keys.PROXY_PASSWORD.getKeyString());
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(CommonStepPropertyKeys.Keys.PROXY_PASSWORD.getKeyString());
  }
}
