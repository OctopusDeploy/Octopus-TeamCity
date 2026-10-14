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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import octopus.teamcity.server.client.OctopusAuth;
import octopus.teamcity.server.client.OctopusHttpClient;

/**
 * Lists the spaces on an Octopus server, so the UI can offer them as a dropdown rather than asking
 * an admin to type a space name by hand.
 */
public class OctopusSpacesFetcher {

  /** How many spaces we ask for. Far above any realistic instance. */
  static final int TAKE = 1000;

  private static final Gson GSON = new GsonBuilder().create();

  /** Performs the GET, so tests need no server. */
  public interface Requester {
    String get(String url, OctopusAuth auth) throws IOException;
  }

  private final Requester requester;

  public OctopusSpacesFetcher() {
    this(new OctopusHttpClient()::get);
  }

  OctopusSpacesFetcher(final Requester requester) {
    this.requester = requester;
  }

  /** The spaces visible to the supplied API key, ordered by name. */
  public List<OctopusSpace> fetch(final String serverUrl, final String apiKey) throws IOException {
    return fetch(serverUrl, OctopusAuth.apiKey(apiKey));
  }

  /**
   * The spaces visible to the supplied credential, ordered by name.
   *
   * @throws IOException if the server cannot be reached, rejects the credential, or returns
   *     something that is not a spaces collection.
   */
  public List<OctopusSpace> fetch(final String serverUrl, final OctopusAuth auth)
      throws IOException {
    final String url = spacesUrl(serverUrl);
    if (auth.isEmpty()) {
      throw new IOException("An Octopus " + auth.description() + " is required to list spaces.");
    }

    final String body = requester.get(url, auth);

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

  static String spacesUrl(final String serverUrl) throws IOException {
    return OctopusHttpClient.baseUrl(serverUrl) + "/api/spaces?skip=0&take=" + TAKE;
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
