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

package octopus.teamcity.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SpaceSelectionTest {

  private Map<String, String> params(final String spaceName) {
    final Map<String, String> params = new HashMap<>();
    if (spaceName != null) {
      params.put(OctopusConstants.Instance.getSpaceName(), spaceName);
    }
    return params;
  }

  @Test
  void resolvesTheSpaceName() {
    assertThat(SpaceSelection.resolve(params("Modern Deployments")))
        .isEqualTo("Modern Deployments");
  }

  @Test
  void trimsSurroundingWhitespace() {
    assertThat(SpaceSelection.resolve(params("  Modern Deployments  ")))
        .isEqualTo("Modern Deployments");
  }

  @Test
  void resolvesToEmptyWhenNoSpaceIsSet() {
    assertThat(SpaceSelection.resolve(params(null))).isEmpty();
    assertThat(SpaceSelection.resolve(params("  "))).isEmpty();
  }

  @Test
  void resolveOrDefaultSubstitutesTheDefaultSpace() {
    assertThat(SpaceSelection.resolveOrDefault(params(null))).isEqualTo("Default");
    assertThat(SpaceSelection.resolveOrDefault(params("  "))).isEqualTo("Default");
  }

  @Test
  void resolveOrDefaultKeepsASpaceThatIsSet() {
    assertThat(SpaceSelection.resolveOrDefault(params("Modern Deployments")))
        .isEqualTo("Modern Deployments");
  }
}
