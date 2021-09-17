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

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.pushpackage.PushPackageKeys;
import org.junit.jupiter.api.Test;

class PushPackageSubStepTypeTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();
    result.put(PushPackageKeys.PACKAGE_PATHS, "Package1\nPackage2");
    result.put(PushPackageKeys.PUBLISH_ARTIFACTS, "false");
    result.put(PushPackageKeys.OVERWRITE_MODE, "FailIfExists");
    result.put(PushPackageKeys.USE_DELTA_COMPRESSION, "false");

    return result;
  }

  @Test
  public void validPropertySetProducesNoInvalidEntries() {
    final PushPackageSubStepType pushPackageSubStepType = new PushPackageSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    assertThat(pushPackageSubStepType.validateProperties(properties)).hasSize(0);
  }

  @Test
  public void invalidOverwriteModeTextProducesInvalidEntry() {
    final PushPackageSubStepType pushPackageSubStepType = new PushPackageSubStepType();
    final Map<String, String> properties = createValidPropertyMap();

    properties.put(PushPackageKeys.OVERWRITE_MODE, "Not Valid Overwrite Mode");
    final List<InvalidProperty> result = pushPackageSubStepType.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(PushPackageKeys.OVERWRITE_MODE);
  }
}
