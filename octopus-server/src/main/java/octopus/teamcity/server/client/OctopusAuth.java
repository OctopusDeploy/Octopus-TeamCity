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

/** The header a request to Octopus authenticates with: an API key, or an OIDC access token. */
public final class OctopusAuth {

  private final String header;
  private final String value;
  private final String description;

  private OctopusAuth(final String header, final String value, final String description) {
    this.header = header;
    this.value = value;
    this.description = description;
  }

  public static OctopusAuth apiKey(final String apiKey) {
    return new OctopusAuth("X-Octopus-ApiKey", trimmed(apiKey), "API key");
  }

  public static OctopusAuth accessToken(final String accessToken) {
    final String token = trimmed(accessToken);
    return new OctopusAuth(
        "Authorization", token.isEmpty() ? "" : "Bearer " + token, "access token");
  }

  public String header() {
    return header;
  }

  public String value() {
    return value;
  }

  /** What the credential is, for error messages: "API key" or "access token". */
  public String description() {
    return description;
  }

  public boolean isEmpty() {
    return value.isEmpty();
  }

  private static String trimmed(final String value) {
    return value == null ? "" : value.trim();
  }
}
