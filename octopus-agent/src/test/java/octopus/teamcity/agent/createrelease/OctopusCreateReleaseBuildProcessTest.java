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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.octopus.sdk.model.commands.CreateReleaseCommandBody;
import com.octopus.sdk.operation.executionapi.CreateRelease;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.createrelease.CreateReleasePropertyNames;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OctopusCreateReleaseBuildProcessTest {

  private final BuildRunnerContext context = mock(BuildRunnerContext.class);
  private final AgentRunningBuild mockBuild = mock(AgentRunningBuild.class);
  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);

  @Test
  public void createReleaseInvokesSdkFunctionalityWithCorrectParameters() throws RunBuildException, IOException {

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

    final CreateRelease createRelease = mock(CreateRelease.class);

    final OctopusCreateReleaseBuildProcess buildProcess =
        new OctopusCreateReleaseBuildProcess(context, createRelease);
    buildProcess.doStart();

    final ArgumentCaptor<CreateReleaseCommandBody> commandBodyArgumentCaptor =
        ArgumentCaptor.forClass(CreateReleaseCommandBody.class);
    verify(createRelease).execute(commandBodyArgumentCaptor.capture());

    final CreateReleaseCommandBody transmittedBody = commandBodyArgumentCaptor.getValue();
    assertThat(transmittedBody).isNotNull();
    assertThat(transmittedBody.getSpaceIdOrName())
        .isEqualTo(parameters.get(CommonStepPropertyNames.SPACE_NAME));
    assertThat(transmittedBody.getProjectIdOrName())
        .isEqualTo(parameters.get(CreateReleasePropertyNames.PROJECT_NAME));
    assertThat(transmittedBody.getPackageVersion())
        .isEqualTo(parameters.get(CreateReleasePropertyNames.PACKAGE_VERSION));
    assertThat(transmittedBody.getReleaseVersion())
        .isEqualTo(parameters.get(CreateReleasePropertyNames.RELEASE_VERSION));
    assertThat(transmittedBody.getChannelIdOrName())
        .isEqualTo(parameters.get(CreateReleasePropertyNames.CHANNEL_NAME));
    assertThat(transmittedBody.getPackages()).containsExactlyInAnyOrder("Package1", "Package2");
    assertThat(transmittedBody.getGitCommit()).isNull();
    assertThat(transmittedBody.getGitRef()).isNull();
    assertThat(transmittedBody.getPackageFolder()).isNull();
    assertThat(transmittedBody.getPackagePrerelease()).isNull();
    assertThat(transmittedBody.getReleaseNotes()).isNull();
    assertThat(transmittedBody.getReleaseNotes()).isNull();
    assertThat(transmittedBody.isIgnoreChannelRules()).isFalse();
    assertThat(transmittedBody.isIgnoreExisting()).isFalse();
  }
}
