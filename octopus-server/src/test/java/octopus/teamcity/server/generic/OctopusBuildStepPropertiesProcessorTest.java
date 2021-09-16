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
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import org.junit.jupiter.api.Test;

/**
 * It should be noted that when TeamCity constructs a Properties Map, it removes leading whitespace
 * thus, a Server URL of " " - will be reduced to an empty string, which is then reduced to a
 * null/missing entry
 */
class OctopusBuildStepPropertiesProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();

    result.put(CommonStepPropertyNames.SERVER_URL, "http://localhost:8065");
    result.put(CommonStepPropertyNames.API_KEY, "API-123456789012345678901234567890");
    result.put(CommonStepPropertyNames.SPACE_NAME, "My Space");
    result.put(CommonStepPropertyNames.PROXY_REQUIRED, "true");
    result.put(CommonStepPropertyNames.PROXY_URL, "http://proxy.url");
    result.put(CommonStepPropertyNames.PROXY_USERNAME, "ProxyUsername");
    result.put(CommonStepPropertyNames.PROXY_PASSWORD, "ProxyPassword");
    result.put(CommonStepPropertyNames.STEP_TYPE, new BuildInformationStep().getName());
    result.put(CommonStepPropertyNames.VERBOSE_LOGGING, "false");

    result.put(BuildInfoPropertyNames.PACKAGE_IDS, "Package1\nPackage2");
    result.put(BuildInfoPropertyNames.PACKAGE_VERSION, "1.0");
    result.put(BuildInfoPropertyNames.OVERWRITE_MODE, "OverwriteExisting");

    return result;
  }

  @Test
  public void aValidInputMapProducesNoInvalidEntries() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    assertThat(processor.process(inputMap)).hasSize(0);
  }

  @Test
  public void anEmptyListThrowsException() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    assertThatThrownBy(() -> processor.process(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void missingStepTypeFieldThrowsIllegalArgumentException() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.STEP_TYPE);
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void stepTypeWhichDoesNotAlignWithAvailableBuildProcessesThrowsIllegalArgument() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyNames.STEP_TYPE, "invalid-step-type");
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void mandatoryFieldsMustBePopulated() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.SERVER_URL);
    inputMap.remove(CommonStepPropertyNames.API_KEY);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(2);
    final List<String> missingPropertyNames =
        result.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toList());
    assertThat(missingPropertyNames)
        .containsExactlyInAnyOrder(
            CommonStepPropertyNames.SERVER_URL, CommonStepPropertyNames.API_KEY);
  }

  @Test
  public void illegallyFormattedServerUrlReturnsASingleInvalidProperty() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyNames.SERVER_URL, "badUrl");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyNames.SERVER_URL);
  }

  @Test
  public void illegallyFormattedApiKeyReturnsASingleInvalidProperty() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(CommonStepPropertyNames.API_KEY, "API-1");
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyNames.API_KEY);
  }

  @Test
  public void spaceNameCanBeNull() {
    // Implies the default space should be used
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.SPACE_NAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void proxyUsernameAndPasswordCanBothBeNull() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.PROXY_PASSWORD);
    inputMap.remove(CommonStepPropertyNames.PROXY_USERNAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(0);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyPasswordIsSetWithoutUsername() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.PROXY_USERNAME);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyNames.PROXY_USERNAME);
  }

  @Test
  public void invalidPropertyIsReturnedIfProxyUsernameIsSetWithoutPassword() {
    final OctopusBuildStepPropertiesProcessor processor = new OctopusBuildStepPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(CommonStepPropertyNames.PROXY_PASSWORD);
    final List<InvalidProperty> result = processor.process(inputMap);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(CommonStepPropertyNames.PROXY_PASSWORD);
  }
}
