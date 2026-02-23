package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class CreateReleaseBuildProcessTest {

  private Object getPrivateField(Object instance, String fieldName) throws Exception {
    Field f = instance.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    return f.get(instance);
  }

  @Test
  void processOutput_setsAutoCreatedReleaseNumber_whenCreateReleaseOutput() throws Exception {
    final OctopusConstants constants = OctopusConstants.Instance;
    Map<String, String> params = new HashMap<>();
    params.put(constants.getDeployToKey(), "test");
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(runningBuild.getBuildLogger()).thenReturn(logger);

    CreateReleaseBuildProcess proc = new CreateReleaseBuildProcess(runningBuild, context);

    String createJson = "{\"Version\": \"1.2.3\"}";
    proc.processOutput(createJson, 0);

    Object autoCreated = getPrivateField(proc, "autoCreatedReleaseNumber");
    assertThat(autoCreated).isEqualTo("1.2.3");
  }

  @Test
  void processOutput_setsServerTaskId_whenDeployOutput() throws Exception {
    final OctopusConstants constants = OctopusConstants.Instance;
    Map<String, String> params = new HashMap<>();
    params.put(constants.getWaitForDeployments(), "true");
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    CreateReleaseBuildProcess proc = new CreateReleaseBuildProcess(runningBuild, context);

    String deployJson = "[{\"ServerTaskId\": \"task-xyz\"}]";
    proc.processOutput(deployJson, 0);

    Object serverTaskId = getPrivateField(proc, "serverTaskId");
    assertThat(serverTaskId).isEqualTo("task-xyz");
  }
}
