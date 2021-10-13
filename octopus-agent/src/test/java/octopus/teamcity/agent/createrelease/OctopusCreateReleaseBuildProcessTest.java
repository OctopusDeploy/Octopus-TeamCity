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

import com.octopus.sdk.operation.buildinformation.BuildInformationUploader;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.buildinformation.BaseBuildVcsData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class OctopusCreateReleaseBuildProcessTest {
  private final BaseBuildVcsData vcsData = mock(BaseBuildVcsData.class);
  private final BuildInformationUploader uploader = mock(BuildInformationUploader.class);
  private final BuildRunnerContext context = mock(BuildRunnerContext.class);
  private final AgentRunningBuild mockBuild = mock(AgentRunningBuild.class);
  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);
  private final BuildAgentConfiguration agentConfig = mock(BuildAgentConfiguration.class);

  @Test
  public void unknownSpaceThrowsExceptionWithMessage() {
    final OctopusCreateReleaseBuildProcess buildProcess = new OctopusCreateReleaseBuildProcess(context, );

  }

}
