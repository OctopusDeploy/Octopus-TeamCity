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

package octopus.teamcity.agent.buildinformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.octopus.sdk.operations.buildinformation.BuildInformationUploader;
import com.octopus.sdk.operations.buildinformation.BuildInformationUploaderContext;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OverwriteMode;
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OctopusBuildInformationBuildProcessTest {

  private final BaseBuildVcsData vcsData = mock(BaseBuildVcsData.class);
  private final BuildInformationUploader uploader = mock(BuildInformationUploader.class);
  private final BuildRunnerContext context = mock(BuildRunnerContext.class);
  private final AgentRunningBuild mockBuild = mock(AgentRunningBuild.class);
  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);
  private final BuildAgentConfiguration agentConfig = mock(BuildAgentConfiguration.class);

  @Test
  public void pushBuildInfoToOctopusServer() throws IOException, RunBuildException {
    final Map<String, String> userEnteredData = new HashMap<>();
    userEnteredData.put(
        BuildInfoPropertyNames.OVERWRITE_MODE, OverwriteMode.OverwriteExisting.name());
    userEnteredData.put(BuildInfoPropertyNames.PACKAGE_VERSION, "1.0");
    userEnteredData.put(BuildInfoPropertyNames.PACKAGE_IDS, "mypackage.first\nmypackage.second");

    final Map<String, String> sharedConfigParameters = new HashMap<>();
    sharedConfigParameters.put("octopus_vcstype", "git");
    sharedConfigParameters.put("vcsroot.url", "git://git.git/git.git");
    sharedConfigParameters.put("build.vcs.number", "COMMIT_HASH");

    when(mockBuild.getBuildNumber()).thenReturn("BuildNumber");
    when(mockBuild.getSharedConfigParameters()).thenReturn(sharedConfigParameters);
    when(mockBuild.getBuildLogger()).thenReturn(logger);
    when(mockBuild.getAgentConfiguration()).thenReturn(agentConfig);
    when(agentConfig.getServerUrl()).thenReturn("http://teamcityServer.com");

    when(vcsData.getBranchName()).thenReturn("BranchName");
    when(vcsData.getCommits()).thenReturn(Collections.emptyList());
    when(context.getRunnerParameters()).thenReturn(userEnteredData);
    when(context.getBuild()).thenReturn(mockBuild);
    when(uploader.upload(any())).thenReturn("12345");

    final OctopusBuildInformationBuildProcess buildProcess =
        new OctopusBuildInformationBuildProcess(uploader, vcsData, context);

    // run the build Process
    buildProcess.start();

    final ArgumentCaptor<BuildInformationUploaderContext> contextCaptor =
        ArgumentCaptor.forClass(BuildInformationUploaderContext.class);
    verify(uploader, times(2)).upload(contextCaptor.capture());

    assertThat(contextCaptor.getAllValues().get(0).getBranch().get())
        .isEqualTo(vcsData.getBranchName());
  }
}
