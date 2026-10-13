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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;

/**
 * Exchanges an OIDC ID token for an Octopus access token, through Octopus's token-exchange
 * endpoint, so spaces can be listed for a connection that authenticates with OIDC.
 */
public class OctopusTokenExchange {

  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  private static final int READ_TIMEOUT_MILLIS = 20_000;
  private static final Gson GSON = new GsonBuilder().create();

  /** Performs the POST. Separated out so tests need no server. */
  public interface Requester {
    String post(String url, String json) throws IOException;
  }

  private final Requester requester;

  public OctopusTokenExchange() {
    this(OctopusTokenExchange::httpPost);
  }

  OctopusTokenExchange(final Requester requester) {
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

    final String body =
        requester.post(OctopusSpacesFetcher.baseUrl(serverUrl) + "/token/v1", GSON.toJson(request));

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

  private static String httpPost(final String url, final String json) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    try {
      connection.setRequestMethod("POST");
      // A redirect would send the ID token somewhere other than the address just validated.
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
      connection.setReadTimeout(READ_TIMEOUT_MILLIS);
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");
      try (OutputStream out = connection.getOutputStream()) {
        out.write(json.getBytes(StandardCharsets.UTF_8));
      }

      final int status = connection.getResponseCode();
      if (status >= 400 && status <= 499) {
        throw new IOException(
            "Octopus did not accept the OIDC token (HTTP "
                + status
                + "). Check the service account has an OIDC identity whose subject matches this"
                + " build configuration.");
      }
      if (status < 200 || status > 299) {
        throw new IOException(
            "Octopus returned HTTP " + status + " when exchanging the OIDC token.");
      }
      try (InputStream in = connection.getInputStream()) {
        return IOUtils.toString(in, StandardCharsets.UTF_8);
      }
    } finally {
      connection.disconnect();
    }
  }

  /** Shape of the subset of the token-exchange response we read. Field names match the JSON. */
  private static final class TokenResponse {
    @SuppressWarnings("MemberName")
    private String access_token;
  }
}
