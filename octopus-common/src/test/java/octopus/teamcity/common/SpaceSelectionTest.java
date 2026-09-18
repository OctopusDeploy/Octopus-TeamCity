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

  private static final OctopusConstants CONSTANTS = OctopusConstants.Instance;

  private Map<String, String> params(final String spaceId, final String spaceName) {
    final Map<String, String> params = new HashMap<>();
    if (spaceId != null) {
      params.put(CONSTANTS.getSpaceId(), spaceId);
    }
    if (spaceName != null) {
      params.put(CONSTANTS.getSpaceName(), spaceName);
    }
    return params;
  }

  @Test
  void prefersTheSpaceIdOverTheSpaceName() {
    // The whole point: the id keeps working after the space is renamed in Octopus, so a stale
    // stored name must not win over it.
    assertThat(SpaceSelection.resolve(params("Spaces-1795", "Modern Deployments")))
        .isEqualTo("Spaces-1795");
  }

  @Test
  void fallsBackToTheSpaceNameWhenNoIdIsStored() {
    assertThat(SpaceSelection.resolve(params(null, "Modern Deployments")))
        .isEqualTo("Modern Deployments");
  }

  @Test
  void treatsABlankIdAsAbsent() {
    assertThat(SpaceSelection.resolve(params("   ", "Modern Deployments")))
        .isEqualTo("Modern Deployments");
  }

  @Test
  void trimsSurroundingWhitespace() {
    assertThat(SpaceSelection.resolve(params(null, "  Modern Deployments  ")))
        .isEqualTo("Modern Deployments");
  }

  @Test
  void resolvesToEmptyWhenNeitherIsSet() {
    assertThat(SpaceSelection.resolve(params(null, null))).isEmpty();
    assertThat(SpaceSelection.resolve(params("", "  "))).isEmpty();
  }

  @Test
  void resolveOrDefaultSubstitutesTheDefaultSpace() {
    // The Go CLI path has always sent "Default" rather than omitting --space.
    assertThat(SpaceSelection.resolveOrDefault(params(null, null))).isEqualTo("Default");
    assertThat(SpaceSelection.DEFAULT_SPACE).isEqualTo("Default");
  }

  @Test
  void resolveOrDefaultStillPrefersAnIdWhenPresent() {
    assertThat(SpaceSelection.resolveOrDefault(params("Spaces-2", "Whatever")))
        .isEqualTo("Spaces-2");
  }

  @Test
  void isSetReportsWhetherASpaceWasIdentified() {
    assertThat(SpaceSelection.isSet(params("Spaces-3", null))).isTrue();
    assertThat(SpaceSelection.isSet(params(null, "Named"))).isTrue();
    assertThat(SpaceSelection.isSet(params(null, null))).isFalse();
    assertThat(SpaceSelection.isSet(params(" ", " "))).isFalse();
  }
}
