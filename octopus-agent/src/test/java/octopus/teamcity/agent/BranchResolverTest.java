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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class BranchResolverTest {

  private static final OctopusConstants CONSTANTS = OctopusConstants.Instance;

  @Test
  void overrideDisabledReturnsDerivedBranch() {
    final Map<String, String> parameters = new HashMap<>();
    assertThat(BranchResolver.resolve(parameters, CONSTANTS, "main")).isEqualTo("main");
  }

  @Test
  void overrideEnabledWithBlankValueReturnsDerivedBranch() {
    final Map<String, String> parameters = new HashMap<>();
    parameters.put(CONSTANTS.getBranchOverrideEnabledKey(), "true");
    parameters.put(CONSTANTS.getBranchOverrideValueKey(), "   ");
    assertThat(BranchResolver.resolve(parameters, CONSTANTS, "main")).isEqualTo("main");
  }

  @Test
  void overrideEnabledWithLiteralValueReturnsLiteralValue() {
    final Map<String, String> parameters = new HashMap<>();
    parameters.put(CONSTANTS.getBranchOverrideEnabledKey(), "true");
    parameters.put(CONSTANTS.getBranchOverrideValueKey(), "release/1.2");
    assertThat(BranchResolver.resolve(parameters, CONSTANTS, "main")).isEqualTo("release/1.2");
  }

  @Test
  void overrideEnabledWithParameterReferenceReturnsItVerbatim() {
    final Map<String, String> parameters = new HashMap<>();
    parameters.put(CONSTANTS.getBranchOverrideEnabledKey(), "true");
    parameters.put(CONSTANTS.getBranchOverrideValueKey(), "%env.BRANCH%");
    assertThat(BranchResolver.resolve(parameters, CONSTANTS, "main")).isEqualTo("%env.BRANCH%");
  }
}
