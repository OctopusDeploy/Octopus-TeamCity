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

import java.util.ArrayList;
import java.util.List;

/**
 * Decorates an {@link Output.Writer} to capture all written lines for later inspection, while still
 * forwarding each line to the delegate (typically the TeamCity build log).
 *
 * <p>Used by the legacy CLI path ({@link OctopusBuildProcess}) to collect stdout/stderr so that the
 * combined output can be passed to {@link OctopusErrorClassifier} for transient-vs-permanent
 * failure classification after the CLI process exits.
 *
 * <p>The Go CLI path ({@link octopus.teamcity.agent.cli.CLIBuildProcess}) does not use this class
 * because it merges stdout/stderr via {@code ProcessBuilder.redirectErrorStream(true)} and captures
 * output inline with a StringBuilder.
 *
 * <p>Not thread-safe; callers must ensure writes are complete before calling {@link
 * #getCapturedOutput()}.
 */
public class CaptureWriter implements Output.Writer {
  private final Output.Writer delegate;
  private final List<String> lines = new ArrayList<String>();

  public CaptureWriter(Output.Writer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void write(String text) {
    delegate.write(text);
    lines.add(text);
  }

  public String getCapturedOutput() {
    StringBuilder sb = new StringBuilder();
    for (String line : lines) {
      sb.append(line).append('\n');
    }
    return sb.toString();
  }
}
