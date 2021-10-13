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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;

public class GenericParameterProcessor implements PropertiesProcessor {

  @Override
  public Collection<InvalidProperty> process(final Map<String, String> properties) {
    final String stepType = properties.get(CommonStepPropertyNames.STEP_TYPE);

    if (stepType == null) {
      return Collections.singletonList(
          new InvalidProperty(CommonStepPropertyNames.STEP_TYPE, "No StepType specified"));
    }

    final BuildStepCollection buildStepCollection = new BuildStepCollection();
    final Optional<OctopusBuildStep> buildStep = buildStepCollection.getStepTypeByName(stepType);

    if (!buildStep.isPresent()) {
      return Collections.singletonList(
          new InvalidProperty(
              CommonStepPropertyNames.STEP_TYPE,
              "Cannot find a build handler for defined steptype"));
    }

    return buildStep.get().validateProperties(properties);
  }
}
