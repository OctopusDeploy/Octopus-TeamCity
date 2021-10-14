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

package octopus.teamcity.agent.runbookrun;

import com.octopus.sdk.model.commands.CreateReleaseCommandBody;
import com.octopus.sdk.model.commands.ExecuteRunbookCommandBody;
import com.octopus.sdk.model.task.TaskState;
import com.octopus.sdk.operation.executionapi.ExecuteRunbook;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.createrelease.OctopusCreateReleaseBuildProcess;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.createrelease.CreateReleasePropertyNames;
import octopus.teamcity.common.runbookrun.RunbookRunPropertyNames;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OctopusRunbookRunBuildProcessTest {

  private final BuildRunnerContext context = mock(BuildRunnerContext.class);
  private final AgentRunningBuild mockBuild = mock(AgentRunningBuild.class);
  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);

  @Test
  public void createReleaseInvokesSdkFunctionalityWithCorrectParameters()
      throws RunBuildException, IOException, InterruptedException {
    final Map<String, String> parameters = new HashMap<>();
    parameters.put(CommonStepPropertyNames.SPACE_NAME, "TheSpace");
    parameters.put(RunbookRunPropertyNames.PROJECT_NAME, "My Project");
    parameters.put(RunbookRunPropertyNames.RUNBOOK_NAME, "TheRunbook");
    parameters.put(RunbookRunPropertyNames.ENVIRONMENT_NAMES, "Environment1\nEnvironment2");
    parameters.put(RunbookRunPropertyNames.SNAPSHOT_NAME, "SNAPSHOT_NAME");

    when(context.getRunnerParameters()).thenReturn(parameters);
    when(context.getBuild()).thenReturn(mockBuild);
    when(mockBuild.getBuildLogger()).thenReturn(logger);

    final String createdServerTaskId = "ThisIsATaskId";
    final ExecuteRunbook executeRunbook = mock(ExecuteRunbook.class);
    final TaskWaiter taskWaiter = mock(TaskWaiter.class);

    when(executeRunbook.execute(any())).thenReturn(createdServerTaskId);
    when(taskWaiter.waitForCompletion(createdServerTaskId)).thenReturn(TaskState.SUCCESS);

    final OctopusRunbookRunBuildProcess buildProcess =
        new OctopusRunbookRunBuildProcess(context, executeRunbook, taskWaiter);
    buildProcess.doStart();

    final ArgumentCaptor<ExecuteRunbookCommandBody> commandBodyArgumentCaptor =
        ArgumentCaptor.forClass(ExecuteRunbookCommandBody.class);
    verify(executeRunbook).execute(commandBodyArgumentCaptor.capture());
    final ExecuteRunbookCommandBody transmittedBody = commandBodyArgumentCaptor.getValue();
    assertThat(transmittedBody).isNotNull();
    assertThat(transmittedBody.getSpaceIdOrName())
        .isEqualTo(parameters.get(CommonStepPropertyNames.SPACE_NAME));
    assertThat(transmittedBody.getRunbookName())
        .isEqualTo(parameters.get(RunbookRunPropertyNames.RUNBOOK_NAME));
//    assertThat(transmittedBody.containsExactlyInAnyOrder())
//        .containsInAnyOrder("Environment1", "Environment2");
    assertThat(transmittedBody.getSnapshot())
        .isEqualTo(parameters.get(RunbookRunPropertyNames.SNAPSHOT_NAME));

    verify(taskWaiter).waitForCompletion(createdServerTaskId);

  }

}
