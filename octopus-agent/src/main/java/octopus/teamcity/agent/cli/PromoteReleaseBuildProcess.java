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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class PromoteReleaseBuildProcess extends CLIBuildProcess {
  public PromoteReleaseBuildProcess(
        @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
  }

  @Override
  protected List<OctopusCommandBuilder> createCommand() {
    final OctopusConstants constants = OctopusConstants.Instance;
    List<OctopusCommandBuilder> commands = new ArrayList<>();
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

    commands.add(CommandHelper.login(parameters));

    final PromoteReleaseCommand promote = new PromoteReleaseCommand(parameters);
    commands.add(promote);

    if (wait) {
      commands.add(new WaitForTaskCommand(parameters, promote::requireServerTaskId));
    }
    return commands;
  }

  @Override
  protected String getLogMessage() {
    return "Promoting Octopus Deploy release";
  }
}
