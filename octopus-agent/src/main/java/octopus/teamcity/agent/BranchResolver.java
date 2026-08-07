/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import java.util.Map;

import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;

/** Resolves the branch to record in build information, honoring the user's override if set. */
public final class BranchResolver {

  private BranchResolver() {}

  public static String resolve(
      final Map<String, String> parameters,
      final OctopusConstants constants,
      final String derivedBranch) {
    final boolean overrideEnabled =
        Boolean.parseBoolean(parameters.get(constants.getBranchOverrideEnabledKey()));
    final String overrideValue = parameters.get(constants.getBranchOverrideValueKey());
    if (overrideEnabled && !StringUtil.isEmptyOrSpaces(overrideValue)) {
      return overrideValue;
    }
    return derivedBranch;
  }
}
