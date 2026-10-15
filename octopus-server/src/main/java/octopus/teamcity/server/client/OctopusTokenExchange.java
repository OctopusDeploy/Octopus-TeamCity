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

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Exchanges an OIDC ID token for an Octopus access token, through Octopus's token-exchange
 * endpoint, so spaces can be listed for a connection that authenticates with OIDC.
 */
public class OctopusTokenExchange {

  private static final Gson GSON = new GsonBuilder().create();

  /** Performs the POST, so tests need no server. */
  public interface Requester {
    String post(String url, String json) throws IOException;
  }

  private final Requester requester;

  public OctopusTokenExchange() {
    this(new OctopusHttpClient()::postJson);
  }

  public OctopusTokenExchange(final Requester requester) {
    this.requester = requester;
  }

  /**
   * The access token Octopus issues for the ID token.
   *
   * @param audience the Octopus service account id the OIDC connection's tokens are issued for
   * @throws IOException if Octopus cannot be reached or does not accept the token
   */
  public String exchange(final String serverUrl, final String audience, final String idToken)
      throws IOException {
    final JsonObject request = new JsonObject();
    request.addProperty("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
    request.addProperty("audience", audience);
    request.addProperty("subject_token", idToken);
    request.addProperty("subject_token_type", "urn:ietf:params:oauth:token-type:jwt");

    final String url = OctopusHttpClient.baseUrl(serverUrl) + "/token/v1";
    final String body;
    try {
      body = requester.post(url, GSON.toJson(request));
    } catch (final OctopusHttpStatusException e) {
      if (e.getStatus() >= 400 && e.getStatus() <= 499) {
        throw new IOException(
            "Octopus did not accept the OIDC token (HTTP "
                + e.getStatus()
                + "). Check the service account has an OIDC identity whose issuer and subject"
                + " match the tokens TeamCity issues.",
            e);
      }
      throw e;
    }

    final TokenResponse response;
    try {
      response = GSON.fromJson(body, TokenResponse.class);
    } catch (final JsonSyntaxException e) {
      throw noAccessToken();
    }
    if (response == null || response.access_token == null || response.access_token.isEmpty()) {
      throw noAccessToken();
    }
    return response.access_token;
  }

  private static IOException noAccessToken() {
    return new IOException("Octopus did not return an access token for the OIDC token.");
  }

  /** Shape of the subset of the token-exchange response we read. Field names match the JSON. */
  private static final class TokenResponse {
    @SuppressWarnings("MemberName")
    private String access_token;
  }
}
