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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class OctopusOsUtils {

  private static final Logger LOGGER = Loggers.AGENT;

  public static Boolean CanRunOcto(@NotNull BuildAgentConfiguration agentConfiguration) {
    if (agentConfiguration.getSystemInfo().isUnix() || agentConfiguration.getSystemInfo().isMac()) {
      String os = agentConfiguration.getSystemInfo().isUnix() ? "Unix" : "Mac";
      if (HasDotNet(agentConfiguration)) {
        LOGGER.info(String.format("Octopus can run on agent with %s and DotNot", os));
        return true;
      } else {
        if (HasOcto(agentConfiguration)) {
          LOGGER.info(String.format("Octopus can run on agent with %s and octo", os));
          return true;
        } else {
          LOGGER.info(String.format("Octopus can not run on agent with %s and without octo or DotNET", os));
          return false;
        }
      }
    } else if (agentConfiguration.getSystemInfo().isWindows()) {
      LOGGER.info("Octopus can run on agent with Windows");
      return true;
    }

    LOGGER.info("Octopus cannot run on agent without Windows, Unix or Mac");
    return false;
  }

  public static Boolean HasDotNet(@NotNull BuildAgentConfiguration agentConfiguration) {
    String result = executeCommand("dotnet");
    // v1 of dotnet outputs Microsoft, v2 outputs Usage
    return result.contains("Microsoft") || result.contains("Usage");
  }

  public static Boolean HasOcto(@NotNull BuildAgentConfiguration agentConfiguration) {
    String result = executeCommand("octo");
    return result.contains("Octopus");
  }

  private static String executeCommand(final String command) {

    final StringBuilder output = new StringBuilder();

    Process p;
    try {
      p = Runtime.getRuntime().exec(command);
      p.waitFor();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(),
          StandardCharsets.UTF_8));

      String line = "";
      while ((line = reader.readLine()) != null) {
        output.append(line + "\n");
      }

    } catch (Exception e) {
      LOGGER.error("Failed to execute command " + command);
    }

    return output.toString();
  }
}
