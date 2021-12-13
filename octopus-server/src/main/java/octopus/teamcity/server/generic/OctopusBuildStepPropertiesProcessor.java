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

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;

public class OctopusBuildStepPropertiesProcessor implements PropertiesProcessor {

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    final List<InvalidProperty> result = Lists.newArrayList();
    if (properties == null) {
      throw new IllegalArgumentException("Supplied properties list was null");
    }

    final String spaceName = properties.get(CommonStepPropertyNames.SPACE_NAME);
    if (spaceName == null) {
      result.add(
          new InvalidProperty(CommonStepPropertyNames.SPACE_NAME, "Space name must be specified"));
    }

    final String connectionId = properties.get(CommonStepPropertyNames.CONNECTION_ID);
    if (connectionId == null) {
      result.add(
          new InvalidProperty(
              CommonStepPropertyNames.CONNECTION_ID, "Connection must be selected"));
    }

    final String stepType = properties.get(CommonStepPropertyNames.STEP_TYPE);
    if (stepType == null) {
      throw new IllegalArgumentException("No step-type was specified, contact Octopus support");
    }

    final BuildStepCollection buildStepCollection = new BuildStepCollection();

    result.addAll(
        buildStepCollection
            .getStepTypeByName(stepType)
            .map(buildStep -> buildStep.validateProperties(properties))
            .orElseThrow(
                () -> new IllegalArgumentException("No matching validation for selected command")));

    return result;
  }
}
