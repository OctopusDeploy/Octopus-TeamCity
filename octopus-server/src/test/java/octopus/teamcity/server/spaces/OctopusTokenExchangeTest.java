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

package octopus.teamcity.server.spaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class OctopusTokenExchangeTest {

  private final List<String> postedUrls = new ArrayList<>();
  private final List<String> postedBodies = new ArrayList<>();

  private OctopusTokenExchange exchangeReturning(final String body) {
    return new OctopusTokenExchange(
        (url, json) -> {
          postedUrls.add(url);
          postedBodies.add(json);
          return body;
        });
  }

  @Test
  void exchangesTheIdTokenForAnAccessToken() throws Exception {
    final String accessToken =
        exchangeReturning("{\"access_token\":\"octopus-access\",\"token_type\":\"Bearer\"}")
            .exchange("https://octopus.example.com/", "service-account-guid", "id.token.value");

    assertThat(accessToken).isEqualTo("octopus-access");
    assertThat(postedUrls).containsExactly("https://octopus.example.com/token/v1");
    final JsonObject posted = new JsonParser().parse(postedBodies.get(0)).getAsJsonObject();
    assertThat(posted.get("grant_type").getAsString())
        .isEqualTo("urn:ietf:params:oauth:grant-type:token-exchange");
    assertThat(posted.get("audience").getAsString()).isEqualTo("service-account-guid");
    assertThat(posted.get("subject_token").getAsString()).isEqualTo("id.token.value");
    assertThat(posted.get("subject_token_type").getAsString())
        .isEqualTo("urn:ietf:params:oauth:token-type:jwt");
  }

  @Test
  void failsClearlyWhenNoAccessTokenComesBack() {
    assertThatThrownBy(
            () ->
                exchangeReturning("{\"error\":\"invalid_grant\"}")
                    .exchange("https://octopus.example.com", "aud", "id.token"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("did not return an access token");
  }

  @Test
  void failsClearlyWhenTheResponseIsNotJson() {
    assertThatThrownBy(
            () -> exchangeReturning("<html/>").exchange("https://octopus.example.com", "a", "t"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("did not return an access token");
  }

  @Test
  void validatesTheServerUrlBeforeCallingOut() {
    assertThatThrownBy(() -> exchangeReturning("{}").exchange("file:///etc/passwd", "a", "t"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("http:// or https://");
    assertThat(postedUrls).isEmpty();
  }
}
