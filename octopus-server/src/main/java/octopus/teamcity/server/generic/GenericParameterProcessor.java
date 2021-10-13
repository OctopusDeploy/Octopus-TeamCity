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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;

public class GenericParameterProcessor implements PropertiesProcessor {

  private static final CommonStepPropertyNames KEYS = new CommonStepPropertyNames();

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    final String stepType = properties.getOrDefault(KEYS.getStepTypePropertyName(), "");
    if (StringUtil.isEmpty(stepType)) {
      failedProperties.add(
          new InvalidProperty(
              CommonStepPropertyNames.STEP_TYPE,
              "StepType must be specified, and cannot be whitespace."));
    }

    final String spaceName = properties.getOrDefault(KEYS.getSpaceNamePropertyName(), "");
    if (StringUtil.isEmpty(spaceName)) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getSpaceNamePropertyName(),
              "Space name must be specified, and cannot be whitespace."));
    }

    final BuildStepCollection buildStepCollection = new BuildStepCollection();
    final Optional<OctopusBuildStep> buildStep = buildStepCollection.getStepTypeByName(stepType);

    if (!buildStep.isPresent()) {
      failedProperties.add(
          new InvalidProperty(
              CommonStepPropertyNames.STEP_TYPE,
              "Cannot find a build handler for defined StepType"));
    } else {
      failedProperties.addAll(buildStep.get().validateProperties(properties));
    }

    return failedProperties;
  }
}
