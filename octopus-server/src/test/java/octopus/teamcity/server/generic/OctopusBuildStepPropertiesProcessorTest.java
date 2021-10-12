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

import octopus.teamcity.common.commonstep.StepTypeConstants;
import octopus.teamcity.server.connection.OctopusConnectionPropertiesProcessor;
import org.junit.jupiter.api.Test;

class OctopusBuildStepPropertiesProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();

    result.put(StepTypeConstants.STEP_TYPE, "build-information");

    return result;
  }

  @Test
  public void missingStepTypeFieldThrowsIllegalArgumentException() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(StepTypeConstants.STEP_TYPE);
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void stepTypeWhichDoesNotAlignWithAvailableBuildProcessesThrowsIllegalArgument() {
    final OctopusConnectionPropertiesProcessor processor =
        new OctopusConnectionPropertiesProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(StepTypeConstants.STEP_TYPE, "invalid-step-type");
    assertThatThrownBy(() -> processor.process(inputMap))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
