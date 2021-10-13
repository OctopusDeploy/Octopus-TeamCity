/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.agent.createrelease;

import com.google.common.collect.Lists;
import com.octopus.sdk.Repository;
import com.octopus.sdk.operation.buildinformation.BuildInformationUploader;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.buildinformation.BaseBuildVcsData;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.createrelease.CreateReleasePropertyNames;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OctopusCreateReleaseBuildProcessTest {

  private final Repository mockRepository = mock(Repository.class);
  private final BuildRunnerContext context = mock(BuildRunnerContext.class);
  private final AgentRunningBuild mockBuild = mock(AgentRunningBuild.class);
  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);
  private final BuildAgentConfiguration agentConfig = mock(BuildAgentConfiguration.class);

  @Test
  public void unknownSpaceThrowsExceptionWithMessage() throws RunBuildException {

    final Map<String, String> parameters = new HashMap<>();
    parameters.put(CommonStepPropertyNames.SPACE_NAME, "TheSpace");
    parameters.put(CreateReleasePropertyNames.PROJECT_NAME, "My Project");
    parameters.put(CreateReleasePropertyNames.PACKAGE_VERSION, "PackageVersion");
    parameters.put(CreateReleasePropertyNames.RELEASE_VERSION, "ReleaseVersion");
    parameters.put(CreateReleasePropertyNames.CHANNEL_NAME, "TheChannel");
    parameters.put(CreateReleasePropertyNames.PACKAGES, "Package1\nPackage2");

    when(context.getRunnerParameters()).thenReturn(parameters);
    when(context.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getBuildLogger()).thenReturn(logger);

    final OctopusCreateReleaseBuildProcess buildProcess = new OctopusCreateReleaseBuildProcess(context, mockRepository);
    buildProcess.doStart();

  }

}
