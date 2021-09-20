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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.pushpackage.PushPackagePropertyNames;
import org.junit.jupiter.api.Test;

class PushPackageStepTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();
    result.put(PushPackagePropertyNames.PACKAGE_PATHS, "Package1\nPackage2");
    result.put(PushPackagePropertyNames.PUBLISH_ARTIFACTS, "false");
    result.put(PushPackagePropertyNames.OVERWRITE_MODE, "FailIfExists");
    result.put(PushPackagePropertyNames.USE_DELTA_COMPRESSION, "false");

    return result;
  }

  @Test
  public void describeParametersDisplaysViewText() {
    final PushPackageStep pushPackageStep = new PushPackageStep();
    final String parameterDescription =
        pushPackageStep.describeParameters(createValidPropertyMap());

    assertEquals("Packages: Package1, Package2", parameterDescription);
  }

  @Test
  public void validPropertySetProducesNoInvalidEntries() {
    final PushPackageStep pushPackageStep = new PushPackageStep();
    final Map<String, String> properties = createValidPropertyMap();

    assertThat(pushPackageStep.validateProperties(properties)).hasSize(0);
  }

  @Test
  public void invalidOverwriteModeTextProducesInvalidEntry() {
    final PushPackageStep pushPackageStep = new PushPackageStep();
    final Map<String, String> properties = createValidPropertyMap();

    properties.put(PushPackagePropertyNames.OVERWRITE_MODE, "Not Valid Overwrite Mode");
    final List<InvalidProperty> result = pushPackageStep.validateProperties(properties);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPropertyName()).isEqualTo(PushPackagePropertyNames.OVERWRITE_MODE);
  }
}
