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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.commonstep.StepTypeConstants;
import org.junit.jupiter.api.Test;

class GenericParameterProcessorTest {

  private Map<String, String> createValidPropertyMap() {
    final Map<String, String> result = new HashMap<>();

    result.put(StepTypeConstants.STEP_TYPE, "build-information");
    result.put(CommonStepPropertyNames.SPACE_NAME, "MySpace");
    result.put(CommonStepPropertyNames.VERBOSE_LOGGING, "false");
    result.put(CommonStepPropertyNames.CONNECTION_NAME, "connectionName");

    result.put(BuildInfoPropertyNames.PACKAGE_IDS, "Package1\nPackage2");
    result.put(BuildInfoPropertyNames.PACKAGE_VERSION, "1.0");
    result.put(BuildInfoPropertyNames.OVERWRITE_MODE, "OverwriteExisting");

    return result;
  }

  @Test
  public void missingStepTypeFieldThrowsIllegalArgumentException() {
    final GenericParameterProcessor processor = new GenericParameterProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.remove(StepTypeConstants.STEP_TYPE);
    final Collection<InvalidProperty> result = processor.process(inputMap);
    assertThat(result)
        .containsExactly(new InvalidProperty(StepTypeConstants.STEP_TYPE, "No StepType specified"));
  }

  @Test
  public void stepTypeWhichDoesNotAlignWithAvailableBuildProcessesThrowsIllegalArgument() {
    final GenericParameterProcessor processor = new GenericParameterProcessor();
    final Map<String, String> inputMap = createValidPropertyMap();

    inputMap.put(StepTypeConstants.STEP_TYPE, "invalid-step-type");
    final Collection<InvalidProperty> result = processor.process(inputMap);
    assertThat(result)
        .containsExactly(
            new InvalidProperty(
                StepTypeConstants.STEP_TYPE,
                "Cannot find a build handler " + "for defined steptype"));
  }
}
