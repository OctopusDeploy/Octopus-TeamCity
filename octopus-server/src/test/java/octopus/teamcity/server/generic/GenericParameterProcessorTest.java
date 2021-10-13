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

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GenericParameterProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();
    result.put(CommonStepPropertyNames.STEP_TYPE, new BuildInformationStep().getName());
    result.put(CommonStepPropertyNames.SPACE_NAME, "MySpace");
    result.put(BuildInfoPropertyNames.PACKAGE_IDS, "Package1\nPackage2");
    result.put(BuildInfoPropertyNames.PACKAGE_VERSION, "1.2.3");
    result.put(BuildInfoPropertyNames.OVERWRITE_MODE, "FailIfExists");

    return result;
  }

  @Test
  public void validPropertyMapReturnsNoInvalidProperties() {
    final GenericParameterProcessor processor = new GenericParameterProcessor();
    final Map<String, String> properties = createValidPropertyMap();
    assertThat(processor.process(properties)).isEmpty();
  }

  @Test
  public void invalidPropertyReturnedIfSpaceNameIsMissing() {
    final Map<String, String> properties = createValidPropertyMap();
    properties.remove(CommonStepPropertyNames.SPACE_NAME);
    final GenericParameterProcessor processor = new GenericParameterProcessor();
    final List<InvalidProperty> result = processor.process(properties);
    assertThat(result)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(CommonStepPropertyNames.SPACE_NAME, "Space name must be specified, and cannot be whitespace.");
  }

  @ParameterizedTest
  @MethodSource("emptyStrings")
  public void invalidPropertyReturnedIfSpaceNameHasInvalidContent(final String invalidContent) {
    final Map<String, String> properties = createValidPropertyMap();
    properties.put(CommonStepPropertyNames.SPACE_NAME, invalidContent);
    final GenericParameterProcessor processor = new GenericParameterProcessor();
    final List<InvalidProperty> result = processor.process(properties);
    assertThat(result)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(CommonStepPropertyNames.SPACE_NAME, "Space name must be specified, and cannot be whitespace.");
  }

  static Stream<String> emptyStrings() {
    return Stream.of("", null);
  }

}
