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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.buildinfo.BuildInfoKeys;
import org.junit.jupiter.api.Test;

class BuildInformationSubStepTypeTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();
    result.put(BuildInfoKeys.Keys.PACKAGE_IDS.getKeyString(), "Package1\nPackage2");
    result.put(BuildInfoKeys.Keys.PACKAGE_VERSION.getKeyString(), "1.2.3");
    result.put(BuildInfoKeys.Keys.OVERWRITE_MODE.getKeyString(), "FailIfExists");

    return result;
  }

  @Test
  public void noInvalidPropertiesFromValidPropertyMap() {
    final BuildInformationSubStepType buildInfoStepType = new BuildInformationSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    assertThat(buildInfoStepType.validateProperties(properties)).hasSize(0);
  }

  @Test
  public void missingPackageIdsProducesSingleInvalidEntry() {
    final BuildInformationSubStepType buildInfoStepType = new BuildInformationSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoKeys.Keys.PACKAGE_IDS.getKeyString());
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(BuildInfoKeys.Keys.PACKAGE_IDS.getKeyString());
  }

  @Test
  public void missingPackageVersionProducesSingleInvalidEntry() {
    final BuildInformationSubStepType buildInfoStepType = new BuildInformationSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoKeys.Keys.PACKAGE_VERSION.getKeyString());
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(BuildInfoKeys.Keys.PACKAGE_VERSION.getKeyString());
  }

  @Test
  public void missingOverwriteModeResultsInInvalidEntry() {
    final BuildInformationSubStepType buildInfoStepType = new BuildInformationSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoKeys.Keys.OVERWRITE_MODE.getKeyString());
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(BuildInfoKeys.Keys.OVERWRITE_MODE.getKeyString());
  }

  @Test
  public void missingBothVersionAndPackageIdsProducesTwoInvalidEntries() {
    final BuildInformationSubStepType buildInfoStepType = new BuildInformationSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoKeys.Keys.PACKAGE_VERSION.getKeyString());
    properties.remove(BuildInfoKeys.Keys.PACKAGE_IDS.getKeyString());
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(2);
    final List<String> failedPropertyNames =
        result.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toList());

    assertThat(failedPropertyNames)
        .containsExactlyInAnyOrder(
            BuildInfoKeys.Keys.PACKAGE_VERSION.getKeyString(),
            BuildInfoKeys.Keys.PACKAGE_IDS.getKeyString());
  }

  @Test
  public void invalidOverwriteModeTextProducesInvalidEntry() {
    final BuildInformationSubStepType buildInfoStepType = new BuildInformationSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    properties.put(BuildInfoKeys.Keys.OVERWRITE_MODE.getKeyString(), "Not Valid Overwrite Mode");
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName())
        .isEqualTo(BuildInfoKeys.Keys.OVERWRITE_MODE.getKeyString());
  }
}
