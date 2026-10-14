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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class OctopusHttpClientTest {

  @Test
  void trimsWhitespaceAndTrailingSlashes() throws Exception {
    assertThat(OctopusHttpClient.baseUrl("  https://octopus.example.com///  "))
        .isEqualTo("https://octopus.example.com");
  }

  @Test
  void acceptsPlainHttpAndHttps() throws Exception {
    // On-prem Octopus installs are commonly plain http on an internal host, so http must work.
    assertThat(OctopusHttpClient.baseUrl("http://octopus.internal:8065"))
        .isEqualTo("http://octopus.internal:8065");
    assertThat(OctopusHttpClient.baseUrl("https://octopus.example.com"))
        .isEqualTo("https://octopus.example.com");
  }

  @Test
  void rejectsAMissingUrl() {
    for (final String url : new String[] {null, "", "   "}) {
      assertThatThrownBy(() -> OctopusHttpClient.baseUrl(url))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("Octopus URL is required");
    }
  }

  @Test
  void rejectsSchemesOtherThanHttpAndHttps() {
    // Without this, URL.openConnection() would happily read a file: or jar: URL.
    for (final String url :
        new String[] {
          "file:///etc/passwd", "jar:file:/tmp/x.jar!/y", "ftp://octopus.example.com", "gopher://x"
        }) {
      assertThatThrownBy(() -> OctopusHttpClient.baseUrl(url))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("must start with http:// or https://");
    }
  }

  @Test
  void rejectsAUrlWithNoHost() {
    // These parse as a URI with the right scheme but no authority, e.g. "http:example.com".
    for (final String url : new String[] {"http:example.com", "https:/onlypath"}) {
      assertThatThrownBy(() -> OctopusHttpClient.baseUrl(url))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("missing a host name");
    }
  }

  @Test
  void rejectsAMalformedUrl() {
    // "https://" and a URL containing a space are not parseable at all.
    for (final String url : new String[] {"https://", "http://exa mple.com"}) {
      assertThatThrownBy(() -> OctopusHttpClient.baseUrl(url))
          .isInstanceOf(IOException.class)
          .hasMessageContaining("is not a valid URL");
    }
  }
}
