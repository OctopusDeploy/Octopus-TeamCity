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

import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

/**
 * Decides whether a step runs on the new (Go) Octopus CLI rather than the legacy {@code octo} CLI.
 * True when the {@code OCTOPUS_NEW_CLI} environment variable is truthy, or when a connection has
 * injected an OIDC api-key source (OIDC requires the new CLI; the legacy CLI cannot use it).
 */
public final class OctopusCliSelector {
  private OctopusCliSelector() {}

  public static boolean shouldUseNewCli(
      final AgentRunningBuild runningBuild, final BuildRunnerContext context) {
    final boolean envFlag =
        Boolean.parseBoolean(
            runningBuild
                .getSharedBuildParameters()
                .getEnvironmentVariables()
                .get("OCTOPUS_NEW_CLI"));
    final Map<String, String> runnerParameters = context.getRunnerParameters();
    final boolean oidc =
        ConnectionPropertyNames.API_KEY_SOURCE_OIDC.equals(
            runnerParameters.get(OctopusConstants.Instance.getApiKeySourceKey()));
    return envFlag || oidc;
  }
}
