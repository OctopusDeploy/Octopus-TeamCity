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

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunner;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class OctopusPackPackageRunner implements AgentBuildRunner {

    protected final ExtensionHolder myExtensionHolder;

    public OctopusPackPackageRunner(@NotNull final ExtensionHolder extensionHolder) {
        myExtensionHolder = extensionHolder;
    }

    @Override
    @NotNull
    public BuildProcess createBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) throws RunBuildException {
        return new OctopusPackPackageBuildProcess(runningBuild, context, myExtensionHolder);
    }

    @Override
    @NotNull
    public AgentBuildRunnerInfo getRunnerInfo() {
        return new AgentBuildRunnerInfo() {
            @Override
            @NotNull
            public String getType() {
                return OctopusConstants.PACK_PACKAGE_RUNNER_TYPE;
            }

            @Override
            public boolean canRun(@NotNull BuildAgentConfiguration agentConfiguration) {
                return OctopusOsUtils.CanRunOcto(agentConfiguration);
            }
        };
    }
}
