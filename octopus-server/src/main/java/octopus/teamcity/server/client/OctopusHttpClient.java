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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

/** Makes requests from the TeamCity server to an Octopus server. */
public class OctopusHttpClient {

  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  private static final int READ_TIMEOUT_MILLIS = 20_000;

  /**
   * The Octopus server URL without trailing slashes, rejecting anything that is not a plain http(s)
   * address.
   *
   * <p>The URL comes from a connection's settings, so the scheme is restricted to http and https -
   * without that, a value such as {@code file:} or {@code jar:} would make {@link
   * URL#openConnection()} read from somewhere other than a web server. A host is required so a
   * scheme-only value cannot slip through.
   */
  public static String baseUrl(final String serverUrl) throws IOException {
    if (serverUrl == null || serverUrl.trim().isEmpty()) {
      throw new IOException("An Octopus URL is required.");
    }
    String base = serverUrl.trim();
    while (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }

    final URI parsed;
    try {
      parsed = new URI(base);
    } catch (final URISyntaxException e) {
      throw new IOException("'" + base + "' is not a valid URL.", e);
    }
    final String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase();
    if (!"http".equals(scheme) && !"https".equals(scheme)) {
      throw new IOException("The Octopus URL must start with http:// or https://.");
    }
    if (parsed.getHost() == null || parsed.getHost().isEmpty()) {
      throw new IOException("The Octopus URL is missing a host name.");
    }

    return base;
  }

  /**
   * The body of a successful GET.
   *
   * @throws IOException if the server cannot be reached, rejects the credential, redirects, or
   *     returns a non-2xx status.
   */
  public String get(final String url, final OctopusAuth auth) throws IOException {
    final HttpURLConnection connection = open(url);
    try {
      connection.setRequestMethod("GET");
      connection.setRequestProperty(auth.header(), auth.value());

      final int status = connection.getResponseCode();
      if (status == HttpURLConnection.HTTP_UNAUTHORIZED
          || status == HttpURLConnection.HTTP_FORBIDDEN) {
        throw new IOException(
            "Octopus rejected the "
                + auth.description()
                + " (HTTP "
                + status
                + "). Check its permissions.");
      }
      return readSuccessfulBody(connection, status);
    } finally {
      connection.disconnect();
    }
  }

  private static HttpURLConnection open(final String url) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    // A redirect would take the request somewhere other than the validated address, and would
    // also replay the credential there.
    connection.setInstanceFollowRedirects(false);
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    connection.setReadTimeout(READ_TIMEOUT_MILLIS);
    connection.setRequestProperty("Accept", "application/json");
    return connection;
  }

  private static String readSuccessfulBody(final HttpURLConnection connection, final int status)
      throws IOException {
    if (status >= 300 && status <= 399) {
      throw new IOException(
          "The Octopus URL redirected (HTTP "
              + status
              + "). Point it directly at the Octopus server.");
    }
    if (status < 200 || status > 299) {
      throw new IOException(
          "Octopus returned HTTP " + status + " for " + connection.getURL().getPath() + ".");
    }
    try (InputStream in = connection.getInputStream()) {
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
  }
}
