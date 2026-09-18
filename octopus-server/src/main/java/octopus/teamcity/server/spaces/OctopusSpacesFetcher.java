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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;

/**
 * Lists the spaces on an Octopus server, so the UI can offer them as a dropdown rather than asking
 * an admin to type a space name by hand.
 *
 * <p>Follows {@code CommitHistoryFetcher}'s shape: the HTTP call sits behind {@link Requester} so
 * the parsing and URL construction can be tested without a server.
 */
public class OctopusSpacesFetcher {

  /** How many spaces we ask for. Far above any realistic instance. */
  static final int TAKE = 1000;

  private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
  private static final int READ_TIMEOUT_MILLIS = 20_000;
  private static final Gson GSON = new GsonBuilder().create();

  /** Performs the GET. Separated out so tests need no server. */
  public interface Requester {
    String get(String url, String apiKey) throws IOException;
  }

  private final Requester requester;

  public OctopusSpacesFetcher() {
    this(OctopusSpacesFetcher::httpGet);
  }

  OctopusSpacesFetcher(final Requester requester) {
    this.requester = requester;
  }

  /**
   * The spaces visible to the supplied API key, ordered by name.
   *
   * @throws IOException if the server cannot be reached, rejects the key, or returns something that
   *     is not a spaces collection.
   */
  public List<OctopusSpace> fetch(final String serverUrl, final String apiKey) throws IOException {
    if (serverUrl == null || serverUrl.trim().isEmpty()) {
      throw new IOException("An Octopus URL is required to list spaces.");
    }
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IOException("An Octopus API key is required to list spaces.");
    }

    final String body = requester.get(spacesUrl(serverUrl), apiKey.trim());

    final SpacesCollection collection;
    try {
      collection = GSON.fromJson(body, SpacesCollection.class);
    } catch (final JsonSyntaxException e) {
      throw new IOException(
          "The Octopus server did not return a spaces list. Check the URL points at an Octopus"
              + " server.",
          e);
    }
    if (collection == null || collection.Items == null) {
      throw new IOException(
          "The Octopus server did not return a spaces list. Check the URL points at an Octopus"
              + " server.");
    }

    final List<OctopusSpace> spaces = new ArrayList<>();
    for (final SpaceItem item : collection.Items) {
      if (item != null && item.Id != null && !item.Id.isEmpty()) {
        spaces.add(new OctopusSpace(item.Id, item.Name == null ? item.Id : item.Name));
      }
    }
    Collections.sort(spaces, Comparator.comparing(space -> space.getName().toLowerCase()));
    return spaces;
  }

  /**
   * Builds the spaces URL, rejecting anything that is not a plain http(s) address.
   *
   * <p>The caller supplies this URL when configuring a connection, so the scheme is restricted to
   * http and https - without that, a value such as {@code file:} or {@code jar:} would make {@link
   * URL#openConnection()} read from somewhere other than a web server. A host is required so a
   * scheme-only value cannot slip through.
   */
  static String spacesUrl(final String serverUrl) throws IOException {
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

    return base + "/api/spaces?skip=0&take=" + TAKE;
  }

  private static String httpGet(final String url, final String apiKey) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    try {
      connection.setRequestMethod("GET");
      // A redirect would take the request somewhere other than the address just validated, and
      // would also replay the API key there.
      connection.setInstanceFollowRedirects(false);
      connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
      connection.setReadTimeout(READ_TIMEOUT_MILLIS);
      connection.setRequestProperty("X-Octopus-ApiKey", apiKey);
      connection.setRequestProperty("Accept", "application/json");

      final int status = connection.getResponseCode();
      if (status == HttpURLConnection.HTTP_UNAUTHORIZED
          || status == HttpURLConnection.HTTP_FORBIDDEN) {
        throw new IOException(
            "Octopus rejected the API key (HTTP "
                + status
                + "). Check the key and its permissions.");
      }
      if (status >= 300 && status <= 399) {
        throw new IOException(
            "The Octopus URL redirected (HTTP "
                + status
                + "). Point it directly at the Octopus server.");
      }
      if (status < 200 || status > 299) {
        throw new IOException("Octopus returned HTTP " + status + " when listing spaces.");
      }
      try (InputStream in = connection.getInputStream()) {
        return IOUtils.toString(in, StandardCharsets.UTF_8);
      }
    } finally {
      connection.disconnect();
    }
  }

  /** Shape of the subset of {@code /api/spaces} we read. Field names match the JSON. */
  private static final class SpacesCollection {
    @SuppressWarnings("MemberName")
    private List<SpaceItem> Items;
  }

  private static final class SpaceItem {
    @SuppressWarnings("MemberName")
    private String Id;

    @SuppressWarnings("MemberName")
    private String Name;
  }
}
