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

package octopus.teamcity.agent.cli;

import java.util.Locale;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;

public class OctopusOsUtils {

  public static String getBinaryOsFolder(AgentRunningBuild runningBuild) throws RunBuildException {
    return getBinaryOsFolder(runningBuild, System.getProperty("os.arch"));
  }

  static String getBinaryOsFolder(AgentRunningBuild runningBuild, String jvmArchitecture)
      throws RunBuildException {
    final String os;
    if (runningBuild.getAgentConfiguration().getSystemInfo().isWindows()) {
      os = "windows-os";
    } else if (runningBuild.getAgentConfiguration().getSystemInfo().isMac()) {
      os = "mac-os";
    } else {
      os = "unix-os";
    }
    return os + "-" + getBinaryArchitecture(jvmArchitecture);
  }

  private static String getBinaryArchitecture(String jvmArchitecture) throws RunBuildException {
    switch (jvmArchitecture == null ? "" : jvmArchitecture.toLowerCase(Locale.ROOT)) {
      case "amd64":
      case "x86_64":
        return "amd64";
      case "aarch64":
      case "arm64":
        return "arm64";
      default:
        throw new RunBuildException(
            "The Octopus CLI is not available for the agent's architecture '"
                + jvmArchitecture
                + "'. Supported architectures are amd64 and arm64.");
    }
  }
}
