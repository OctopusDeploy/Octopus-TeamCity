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

import org.junit.jupiter.api.Test;

class OctopusSpacesFetcherTest {

  private final List<String> requestedUrls = new ArrayList<>();
  private final List<String> sentApiKeys = new ArrayList<>();

  private OctopusSpacesFetcher fetcherReturning(final String body) {
    return new OctopusSpacesFetcher(
        (url, apiKey) -> {
          requestedUrls.add(url);
          sentApiKeys.add(apiKey);
          return body;
        });
  }

  @Test
  void readsIdAndNameFromTheSpacesCollection() throws Exception {
    final OctopusSpacesFetcher fetcher =
        fetcherReturning(
            "{\"TotalResults\":2,\"Items\":["
                + "{\"Id\":\"Spaces-1\",\"Name\":\"Default\",\"Slug\":\"default\"},"
                + "{\"Id\":\"Spaces-1795\",\"Name\":\"Swordfish\"}]}");

    final List<OctopusSpace> spaces = fetcher.fetch("https://octopus.example.com", "API-KEY");

    assertThat(spaces).extracting(OctopusSpace::getId).containsExactly("Spaces-1", "Spaces-1795");
    assertThat(spaces).extracting(OctopusSpace::getName).containsExactly("Default", "Swordfish");
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
        .containsExactly("https://octopus.example.com/api/spaces?skip=0&take=1000");
  }

  @Test
  void trimsTheApiKeyBeforeSendingIt() throws Exception {
    fetcherReturning("{\"Items\":[]}").fetch("https://octopus.example.com", "  API-KEY  ");

    assertThat(sentApiKeys).containsExactly("API-KEY");
  }

  @Test
  void rejectsAMissingUrlOrKeyWithoutCallingOut() {
    final OctopusSpacesFetcher fetcher = fetcherReturning("{\"Items\":[]}");

    assertThatThrownBy(() -> fetcher.fetch("  ", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Octopus URL is required");
    assertThatThrownBy(() -> fetcher.fetch("https://octopus.example.com", " "))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("API key is required");
    assertThat(requestedUrls).isEmpty();
  }

  @Test
  void failsClearlyWhenTheResponseIsNotASpacesCollection() {
    // e.g. the URL points at something that is not an Octopus server and returns HTML.
    assertThatThrownBy(
            () -> fetcherReturning("<html>not octopus</html>").fetch("https://x", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("did not return a spaces list");
  }

  @Test
  void failsClearlyWhenTheResponseHasNoItems() {
    assertThatThrownBy(() -> fetcherReturning("{\"Links\":{}}").fetch("https://x", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("did not return a spaces list");
  }

  @Test
  void propagatesTransportFailures() {
    final OctopusSpacesFetcher fetcher =
        new OctopusSpacesFetcher(
            (url, apiKey) -> {
              throw new IOException("connection refused");
            });

    assertThatThrownBy(() -> fetcher.fetch("https://octopus.example.com", "API-KEY"))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("connection refused");
  }
}
