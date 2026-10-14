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

import octopus.teamcity.server.client.OctopusAuth;
import org.junit.jupiter.api.Test;

class OctopusSpacesFetcherTest {

  private final List<String> requestedUrls = new ArrayList<>();
  private final List<OctopusAuth> sentAuth = new ArrayList<>();

  private OctopusSpacesFetcher fetcherReturning(final String body) {
    return new OctopusSpacesFetcher(
        (url, auth) -> {
          requestedUrls.add(url);
          sentAuth.add(auth);
          return body;
        });
  }

  @Test
  void readsIdAndNameFromTheSpacesCollection() throws Exception {
    final OctopusSpacesFetcher fetcher =
        fetcherReturning(
            "{\"TotalResults\":2,\"Items\":["
                + "{\"Id\":\"Spaces-1\",\"Name\":\"Default\",\"Slug\":\"default\"},"
                + "{\"Id\":\"Spaces-123\",\"Name\":\"Example Space\"}]}");

    final List<OctopusSpace> spaces = fetcher.fetch("https://octopus.example.com", "API-KEY");

    assertThat(spaces).extracting(OctopusSpace::getId).containsExactly("Spaces-1", "Spaces-123");
    assertThat(spaces)
        .extracting(OctopusSpace::getName)
        .containsExactly("Default", "Example Space");
  }

  @Test
  void ordersSpacesByNameCaseInsensitively() throws Exception {
    final OctopusSpacesFetcher fetcher =
        fetcherReturning(
            "{\"Items\":[{\"Id\":\"Spaces-3\",\"Name\":\"zebra\"},"
                + "{\"Id\":\"Spaces-1\",\"Name\":\"Apple\"},"
                + "{\"Id\":\"Spaces-2\",\"Name\":\"banana\"}]}");

    assertThat(fetcher.fetch("https://octopus.example.com", "API-KEY"))
        .extracting(OctopusSpace::getName)
        .containsExactly("Apple", "banana", "zebra");
  }

  @Test
  void skipsItemsWithNoIdAndFallsBackToTheIdWhenUnnamed() throws Exception {
    final OctopusSpacesFetcher fetcher =
        fetcherReturning("{\"Items\":[{\"Name\":\"no id at all\"},{\"Id\":\"Spaces-9\"}]}");

    final List<OctopusSpace> spaces = fetcher.fetch("https://octopus.example.com", "API-KEY");

    assertThat(spaces).extracting(OctopusSpace::getId).containsExactly("Spaces-9");
    assertThat(spaces).extracting(OctopusSpace::getName).containsExactly("Spaces-9");
  }

  @Test
  void toleratesAnEmptyCollection() throws Exception {
    assertThat(fetcherReturning("{\"TotalResults\":0,\"Items\":[]}").fetch("https://o", "API-KEY"))
        .isEmpty();
  }

  @Test
  void buildsTheSpacesUrlAndTrimsTrailingSlashes() throws Exception {
    final OctopusSpacesFetcher fetcher = fetcherReturning("{\"Items\":[]}");

    fetcher.fetch("https://octopus.example.com///  ", "API-KEY");

    assertThat(requestedUrls)
        .containsExactly("https://octopus.example.com/api/spaces?skip=0&take=" + Integer.MAX_VALUE);
  }

  @Test
  void sendsTheApiKey() throws Exception {
    fetcherReturning("{\"Items\":[]}").fetch("https://octopus.example.com", "API-KEY");

    assertThat(sentAuth).extracting(OctopusAuth::header).containsExactly("X-Octopus-ApiKey");
    assertThat(sentAuth).extracting(OctopusAuth::value).containsExactly("API-KEY");
  }

  @Test
  void rejectsAMissingAccessTokenWithoutCallingOut() {
    assertThatThrownBy(
            () -> fetcherReturning("{}").fetch("https://o", OctopusAuth.accessToken(" ")))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("access token");
    assertThat(requestedUrls).isEmpty();
  }

  @Test
  void rejectsAnInvalidUrlOrMissingKeyWithoutCallingOut() {
    final OctopusSpacesFetcher fetcher = fetcherReturning("{\"Items\":[]}");

    assertThatThrownBy(() -> fetcher.fetch("  ", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Octopus URL is required");
    assertThatThrownBy(() -> fetcher.fetch("file:///etc/passwd", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("must start with http:// or https://");
    assertThatThrownBy(() -> fetcher.fetch("https://octopus.example.com", " "))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("API key is required");
    assertThat(requestedUrls).isEmpty();
  }

  @Test
  void acceptsPlainHttpAndHttps() throws Exception {
    // On-prem Octopus installs are commonly plain http on an internal host, so http must work.
    fetcherReturning("{\"Items\":[]}").fetch("http://octopus.internal:8065", "API-KEY");
    fetcherReturning("{\"Items\":[]}").fetch("https://octopus.example.com", "API-KEY");

    assertThat(requestedUrls)
        .containsExactly(
            "http://octopus.internal:8065/api/spaces?skip=0&take=" + Integer.MAX_VALUE,
            "https://octopus.example.com/api/spaces?skip=0&take=" + Integer.MAX_VALUE);
  }

  @Test
  void failsClearlyWhenTheResponseIsNotASpacesCollection() {
    // e.g. the URL points at something that is not an Octopus server and returns HTML.
    assertThatThrownBy(
            () -> fetcherReturning("<html>not octopus</html>").fetch("https://x", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unable to retrieve spaces from the Octopus Server.");
  }

  @Test
  void failsClearlyWhenTheResponseHasNoItems() {
    assertThatThrownBy(() -> fetcherReturning("{\"Links\":{}}").fetch("https://x", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unable to retrieve spaces from the Octopus Server.");
  }

  @Test
  void propagatesTransportFailures() {
    final OctopusSpacesFetcher fetcher =
        new OctopusSpacesFetcher(
            (url, auth) -> {
              throw new IOException("connection refused");
            });

    assertThatThrownBy(() -> fetcher.fetch("https://octopus.example.com", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("connection refused");
  }
}
