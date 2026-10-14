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

package octopus.teamcity.server.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OctopusAuthTest {

  @Test
  void sendsAnApiKeyInTheOctopusApiKeyHeader() {
    final OctopusAuth auth = OctopusAuth.apiKey("API-KEY");

    assertThat(auth.header()).isEqualTo("X-Octopus-ApiKey");
    assertThat(auth.value()).isEqualTo("API-KEY");
  }

  @Test
  void sendsAnAccessTokenAsABearerToken() {
    final OctopusAuth auth = OctopusAuth.accessToken("access-token");

    assertThat(auth.header()).isEqualTo("Authorization");
    assertThat(auth.value()).isEqualTo("Bearer access-token");
  }

  @Test
  void trimsTheCredential() {
    assertThat(OctopusAuth.apiKey("  API-KEY  ").value()).isEqualTo("API-KEY");
    assertThat(OctopusAuth.accessToken("  access-token  ").value())
        .isEqualTo("Bearer access-token");
  }

  @Test
  void treatsABlankOrMissingCredentialAsEmpty() {
    assertThat(OctopusAuth.apiKey(" ").isEmpty()).isTrue();
    assertThat(OctopusAuth.apiKey(null).isEmpty()).isTrue();
    assertThat(OctopusAuth.accessToken(" ").isEmpty()).isTrue();
  }
}
