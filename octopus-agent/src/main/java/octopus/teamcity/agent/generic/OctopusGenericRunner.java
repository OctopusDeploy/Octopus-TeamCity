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

package octopus.teamcity.agent.generic;

import jetbrains.buildServer.agent.AgentBuildRunner;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class OctopusGenericRunner implements AgentBuildRunner {

  @NotNull
  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new AgentBuildRunnerInfo() {
      @NotNull
      @Override
      public String getType() {
        return OctopusConstants.GENERIC_RUNNER_TYPE;
      }

      @Override
      public boolean canRun(@NotNull final BuildAgentConfiguration buildAgentConfiguration) {
        return true;
      }
    };
  }

  @NotNull
  @Override
  public BuildProcess createBuildProcess(
      @NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) {

    final BuildProgressLogger logger = runningBuild.getBuildLogger();

    return new BuildProcess() {
      @Override
      public void start() {
        logger.buildFailureDescription(
            "Octopus Generic Runner is not yet ready for use, please use existing steps");
      }

      @Override
      public boolean isInterrupted() {
        return false;
      }

      @Override
      public boolean isFinished() {
        return true;
      }

      @Override
      public void interrupt() {}

      @NotNull
      @Override
      public BuildFinishedStatus waitFor() {
        return BuildFinishedStatus.FINISHED_FAILED;
      }
    };
  }
}
