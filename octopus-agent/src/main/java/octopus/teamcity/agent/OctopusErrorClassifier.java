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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Classifies CLI exit failures as transient (worth retrying) or permanent (fail immediately).
 *
 * <p>Both the legacy .NET CLI (octo.dll) and the Go CLI (octopus) produce different error messages
 * for the same failure modes, so patterns cover both formats: - .NET: "The remote server returned
 * an error: (502)" / "Unable to connect to the remote server" - Go: "server returned HTTP status
 * 502" / "connection refused" / "i/o timeout"
 *
 * <p>Classification priority: permanent patterns are checked first. If neither matches, the error
 * is treated as permanent (fail-safe: unknown errors are not retried).
 *
 * @see OctopusCliRetryExecutor
 */
public class OctopusErrorClassifier {

  public enum ErrorCategory {
    TRANSIENT,
    PERMANENT
  }

  private static final List<Pattern> TRANSIENT_PATTERNS =
      Arrays.asList(
          Pattern.compile("Unable to connect to the remote server", Pattern.CASE_INSENSITIVE),
          Pattern.compile(
              "The remote server returned an error: \\(50[234]\\)", Pattern.CASE_INSENSITIVE),
          Pattern.compile("The remote name could not be resolved", Pattern.CASE_INSENSITIVE),
          Pattern.compile("A connection attempt failed", Pattern.CASE_INSENSITIVE),
          Pattern.compile("connection refused", Pattern.CASE_INSENSITIVE),
          Pattern.compile("context deadline exceeded", Pattern.CASE_INSENSITIVE),
          Pattern.compile("i/o timeout", Pattern.CASE_INSENSITIVE),
          Pattern.compile("no such host", Pattern.CASE_INSENSITIVE),
          Pattern.compile("server returned HTTP status 50[234]", Pattern.CASE_INSENSITIVE),
          Pattern.compile("The Octopus server .* is not available", Pattern.CASE_INSENSITIVE));

  private static final List<Pattern> PERMANENT_PATTERNS =
      Arrays.asList(
          Pattern.compile("\\(40[134]\\)", Pattern.CASE_INSENSITIVE),
          Pattern.compile("HTTP status 40[134]", Pattern.CASE_INSENSITIVE),
          Pattern.compile("Could not find (project|environment|channel)", Pattern.CASE_INSENSITIVE),
          Pattern.compile("(API-Key|apikey).*invalid", Pattern.CASE_INSENSITIVE),
          Pattern.compile("space .* not found", Pattern.CASE_INSENSITIVE));

  public static ErrorCategory classify(int exitCode, String output) {
    if (exitCode == 0) {
      return ErrorCategory.PERMANENT;
    }

    if (output == null || output.isEmpty()) {
      return ErrorCategory.PERMANENT;
    }

    if (matchesAny(output, PERMANENT_PATTERNS)) {
      return ErrorCategory.PERMANENT;
    }

    if (matchesAny(output, TRANSIENT_PATTERNS)) {
      return ErrorCategory.TRANSIENT;
    }

    return ErrorCategory.PERMANENT;
  }

  private static boolean matchesAny(String output, List<Pattern> patterns) {
    for (Pattern pattern : patterns) {
      if (pattern.matcher(output).find()) {
        return true;
      }
    }
    return false;
  }
}
