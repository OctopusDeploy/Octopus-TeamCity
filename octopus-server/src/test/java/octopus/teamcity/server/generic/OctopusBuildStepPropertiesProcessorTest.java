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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import org.junit.jupiter.api.Test;

class OctopusBuildStepPropertiesProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();

    result.put(CommonStepPropertyNames.STEP_TYPE, new BuildInformationStep().getName());
    result.put(CommonStepPropertyNames.SPACE_NAME, "My Space");
    result.put(CommonStepPropertyNames.VERBOSE_LOGGING, "false");
    result.put(BuildInfoPropertyNames.PACKAGE_IDS, "Package1\nPackage2");
    result.put(BuildInfoPropertyNames.PACKAGE_VERSION, "1.0");
    result.put(BuildInfoPropertyNames.OVERWRITE_MODE, "OverwriteExisting");

    return result;
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
}
