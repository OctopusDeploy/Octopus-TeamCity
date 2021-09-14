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
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import org.junit.jupiter.api.Test;

class BuildInformationStepTypeTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();
    result.put(BuildInfoPropertyNames.PACKAGE_IDS, "Package1\nPackage2");
    result.put(BuildInfoPropertyNames.PACKAGE_VERSION, "1.2.3");
    result.put(BuildInfoPropertyNames.OVERWRITE_MODE, "FailIfExists");

    return result;
  }

  @Test
  public void noInvalidPropertiesFromValidPropertyMap() {
    final BuildInformationStep buildInfoStepType = new BuildInformationStep();
    final Map<String, String> properties = createValidPropertyMap();

    assertThat(buildInfoStepType.validateProperties(properties)).hasSize(0);
  }

  @Test
  public void missingPackageIdsProducesSingleInvalidEntry() {
    final BuildInformationStep buildInfoStepType = new BuildInformationStep();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoPropertyNames.PACKAGE_IDS);
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(BuildInfoPropertyNames.PACKAGE_IDS);
  }

  @Test
  public void missingPackageVersionProducesSingleInvalidEntry() {
    final BuildInformationStep buildInfoStepType = new BuildInformationStep();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoPropertyNames.PACKAGE_VERSION);
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(BuildInfoPropertyNames.PACKAGE_VERSION);
  }

  @Test
  public void missingOverwriteModeResultsInInvalidEntry() {
    final BuildInformationStep buildInfoStepType = new BuildInformationStep();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoPropertyNames.OVERWRITE_MODE);
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(BuildInfoPropertyNames.OVERWRITE_MODE);
  }

  @Test
  public void missingBothVersionAndPackageIdsProducesTwoInvalidEntries() {
    final BuildInformationStep buildInfoStepType = new BuildInformationStep();
    final Map<String, String> properties = createValidPropertyMap();

    properties.remove(BuildInfoPropertyNames.PACKAGE_VERSION);
    properties.remove(BuildInfoPropertyNames.PACKAGE_IDS);
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(2);
    final List<String> failedPropertyNames =
        result.stream().map(InvalidProperty::getPropertyName).collect(Collectors.toList());

    assertThat(failedPropertyNames)
        .containsExactlyInAnyOrder(
            BuildInfoPropertyNames.PACKAGE_VERSION, BuildInfoPropertyNames.PACKAGE_IDS);
  }

  @Test
  public void invalidOverwriteModeTextProducesInvalidEntry() {
    final BuildInformationStep buildInfoStepType = new BuildInformationStep();
    final Map<String, String> properties = createValidPropertyMap();

    properties.put(BuildInfoPropertyNames.OVERWRITE_MODE, "Not Valid Overwrite Mode");
    final List<InvalidProperty> result = buildInfoStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(BuildInfoPropertyNames.OVERWRITE_MODE);
  }
}
