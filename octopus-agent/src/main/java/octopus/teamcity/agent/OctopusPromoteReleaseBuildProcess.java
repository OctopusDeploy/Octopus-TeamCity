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
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class OctopusPromoteReleaseBuildProcess extends OctopusBuildProcess {
  public OctopusPromoteReleaseBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
  }

  @Override
  protected String getLogMessage() {
    return "Promoting Octopus Deploy release";
  }

  @Override
  protected OctopusCommandBuilder createCommand() {
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final OctopusConstants constants = OctopusConstants.Instance;

    return new OctopusCommandBuilder() {
      @Override
      protected String[] buildCommand(boolean masked) {
        final ArrayList<String> commands = new ArrayList<String>();
        final String serverUrl = parameters.get(constants.getServerKey());
        final String apiKey = parameters.get(constants.getApiKey());
        final String spaceName = parameters.get(constants.getSpaceName());
        final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());
        final String promoteFrom = parameters.get(constants.getPromoteFromKey());
        final String deployTo = parameters.get(constants.getDeployToKey());
        final String projectName = parameters.get(constants.getProjectNameKey());
        final String tenants = parameters.get(constants.getTenantsKey());
        final String tenanttags = parameters.get(constants.getTenantTagsKey());
        final boolean wait =
            Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));
        final String deploymentTimeout = parameters.get(constants.getDeploymentTimeout());
        final boolean cancelOnTimeout =
            Boolean.parseBoolean(parameters.get(constants.getCancelDeploymentOnTimeout()));

        commands.add("promote-release");
        commands.add("--server");
        commands.add(serverUrl);
        commands.add("--apikey");
        commands.add(masked ? "SECRET" : apiKey);

        if (spaceName != null && !spaceName.isEmpty()) {
          commands.add("--space");
          commands.add(spaceName);
        }
        commands.add("--project");
        commands.add(projectName);
        commands.add("--enableservicemessages");

        if (promoteFrom != null && !promoteFrom.isEmpty()) {
          commands.add("--from");
          commands.add(promoteFrom);
        }

        for (String env : splitCommaSeparatedValues(deployTo)) {
          commands.add("--deployto");
          commands.add(env);
        }

        if (wait && deployTo != null && !deployTo.isEmpty()) {
          commands.add("--progress");

          if (deploymentTimeout != null && !deploymentTimeout.isEmpty()) {
            commands.add("--deploymenttimeout");
            commands.add(deploymentTimeout);
          }

          if (cancelOnTimeout) {
            commands.add("--cancelontimeout");
          }
        }

        for (String tenant : splitCommaSeparatedValues(tenants)) {
          commands.add("--tenant");
          commands.add(tenant);
        }

        for (String tenanttag : splitCommaSeparatedValues(tenanttags)) {
          commands.add("--tenanttag");
          commands.add(tenanttag);
        }

        if (commandLineArguments != null && !commandLineArguments.isEmpty()) {
          commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
        }

        return commands.toArray(new String[commands.size()]);
      }
    };
  }
}
