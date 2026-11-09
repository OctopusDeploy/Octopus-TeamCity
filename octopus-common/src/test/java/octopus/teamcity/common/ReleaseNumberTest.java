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

import org.junit.jupiter.api.Test;

class ReleaseNumberTest {

  @Test
  void latestInAnyCaseIsLatest() {
    assertThat(ReleaseNumber.isLatest("latest")).isTrue();
    assertThat(ReleaseNumber.isLatest("Latest")).isTrue();
    assertThat(ReleaseNumber.isLatest(" LATEST ")).isTrue();
  }

  @Test
  void anyOtherValueIsNotLatest() {
    assertThat(ReleaseNumber.isLatest(null)).isFalse();
    assertThat(ReleaseNumber.isLatest("")).isFalse();
    assertThat(ReleaseNumber.isLatest("1.0.0-latest")).isFalse();
    assertThat(ReleaseNumber.isLatest("%octo.releaseNumber%")).isFalse();
  }
}
