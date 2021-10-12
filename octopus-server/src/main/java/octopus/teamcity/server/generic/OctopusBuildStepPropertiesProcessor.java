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

import com.octopus.sdk.utils.ApiKeyValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.commonstep.StepTypeConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

public class OctopusBuildStepPropertiesProcessor implements PropertiesProcessor {

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    if (properties == null) {
      throw new IllegalArgumentException("Supplied properties list was null");
    }

    final String stepType = properties.get(StepTypeConstants.STEP_TYPE);
    if (stepType == null) {
      throw new IllegalArgumentException("No step-type was specified, contact Octopus support");
    }

    final BuildStepCollection buildStepCollection = new BuildStepCollection();
    final Optional<OctopusBuildStep> buildStep = buildStepCollection.getStepTypeByName(stepType);

    if (!buildStep.isPresent()) {
      return Collections.singletonList(
          new InvalidProperty(
              StepTypeConstants.STEP_TYPE, "Cannot find a build handler for defined steptype"));
    }

    return buildStep.get().validateProperties(properties);
  }
}
