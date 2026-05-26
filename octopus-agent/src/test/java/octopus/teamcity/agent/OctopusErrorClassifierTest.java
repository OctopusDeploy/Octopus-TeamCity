/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;

import octopus.teamcity.agent.OctopusErrorClassifier.ErrorCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OctopusErrorClassifierTest {

  @Test
  void exitCodeZeroIsPermanent() {
    assertThat(OctopusErrorClassifier.classify(0, "some output"))
        .isEqualTo(ErrorCategory.PERMANENT);
  }

  @Test
  void nullOutputIsPermanent() {
    assertThat(OctopusErrorClassifier.classify(1, null)).isEqualTo(ErrorCategory.PERMANENT);
  }

  @Test
  void emptyOutputIsPermanent() {
    assertThat(OctopusErrorClassifier.classify(1, "")).isEqualTo(ErrorCategory.PERMANENT);
  }

  @Test
  void unknownErrorIsPermanent() {
    assertThat(OctopusErrorClassifier.classify(1, "Something unexpected happened"))
        .isEqualTo(ErrorCategory.PERMANENT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Unable to connect to the remote server",
        "The remote server returned an error: (502) Bad Gateway",
        "The remote server returned an error: (503) Service Unavailable",
        "The remote server returned an error: (504) Gateway Timeout",
        "The remote name could not be resolved: 'octopus.example.com'",
        "A connection attempt failed because the connected party did not properly respond",
        "dial tcp 10.0.0.1:443: connection refused",
        "context deadline exceeded",
        "i/o timeout",
        "no such host",
        "server returned HTTP status 502",
        "server returned HTTP status 503",
        "server returned HTTP status 504",
        "The Octopus server https://octopus.example.com is not available",
        "Unable to connect to the Octopus Deploy server",
        "Octopus Cloud instance is Undergoing Maintenance",
        "Your Octopus Cloud instance is currently undergoing maintenance and will be back soon."
      })
  void transientPatternsAreClassifiedCorrectly(String output) {
    assertThat(OctopusErrorClassifier.classify(1, output)).isEqualTo(ErrorCategory.TRANSIENT);
  }

  @Test
  void octoDllMaintenancePageIsTransient() {
    String output =
        "System.Exception: Unable to connect to the Octopus Deploy server. "
            + "See the inner exception for details.\n"
            + " ---> Octopus.Client.Exceptions.OctopusServerException: <!DOCTYPE html>\n"
            + "<html>\n"
            + "  <head>\n"
            + "    <title>Octopus Cloud instance is Undergoing Maintenance</title>\n"
            + "  </head>\n"
            + "  <body>\n"
            + "    <h2>Octopus Cloud instance is Undergoing Maintenance</h2>\n"
            + "    <p>Your Octopus Cloud instance is currently undergoing maintenance "
            + "and will be back soon.</p>\n"
            + "  </body>\n"
            + "</html>\n"
            + "Exit code: -3";
    assertThat(OctopusErrorClassifier.classify(253, output)).isEqualTo(ErrorCategory.TRANSIENT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "The remote server returned an error: (401) Unauthorized",
        "The remote server returned an error: (403) Forbidden",
        "The remote server returned an error: (404) Not Found",
        "HTTP status 401",
        "HTTP status 403",
        "HTTP status 404",
        "Could not find project 'MyProject'",
        "Could not find environment 'Production'",
        "Could not find channel 'Default'",
        "API-Key is invalid",
        "The apikey provided is invalid",
        "space 'NonExistent' not found"
      })
  void permanentPatternsAreClassifiedCorrectly(String output) {
    assertThat(OctopusErrorClassifier.classify(1, output)).isEqualTo(ErrorCategory.PERMANENT);
  }

  @Test
  void transientPatternBuriedInMultiLineOutput() {
    String output =
        "Starting deployment...\n"
            + "Connecting to server...\n"
            + "Unable to connect to the remote server\n"
            + "at System.Net.HttpWebRequest.GetResponse()";
    assertThat(OctopusErrorClassifier.classify(1, output)).isEqualTo(ErrorCategory.TRANSIENT);
  }

  @Test
  void permanentPatternTakesPrecedenceOverTransient() {
    String output =
        "Unable to connect to the remote server\n"
            + "The remote server returned an error: (401) Unauthorized";
    assertThat(OctopusErrorClassifier.classify(1, output)).isEqualTo(ErrorCategory.PERMANENT);
  }

  @Test
  void caseInsensitiveMatching() {
    assertThat(OctopusErrorClassifier.classify(1, "CONNECTION REFUSED"))
        .isEqualTo(ErrorCategory.TRANSIENT);
  }
}
