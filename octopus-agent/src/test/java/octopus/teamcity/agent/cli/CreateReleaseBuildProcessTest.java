package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CreateReleaseBuildProcessTest {

  private Object getPrivateField(Object instance, String fieldName) throws Exception {
    Field f = instance.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    return f.get(instance);
  }

  @Test
  void processOutput_setsAutoCreatedReleaseNumber_whenCreateReleaseOutput() throws Exception {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    CreateReleaseBuildProcess proc = new CreateReleaseBuildProcess(runningBuild, context);

    String createJson = "{\"Version\": \"1.2.3\"}";
    proc.processOutput(createJson, 0);

    Object autoCreated = getPrivateField(proc, "autoCreatedReleaseNumber");
    assertThat(autoCreated).isEqualTo("1.2.3");
  }

  @Test
  void processOutput_setsServerTaskId_whenDeployOutput() throws Exception {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    CreateReleaseBuildProcess proc = new CreateReleaseBuildProcess(runningBuild, context);

    String deployJson = "[{\"ServerTaskId\": \"task-xyz\"}]";
    proc.processOutput(deployJson, 0);

    Object serverTaskId = getPrivateField(proc, "serverTaskId");
    assertThat(serverTaskId).isEqualTo("task-xyz");
  }

  @Test
  void createCommand_includesLoginAndCreateWhenNoDeploy() {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getReleaseNumberKey(), "1.2.3");
    params.put(constants.getCommandLineArgumentsKey(), "--flag1 --flag2=value");

    when(context.getRunnerParameters()).thenReturn(params);

    CreateReleaseBuildProcess proc = new CreateReleaseBuildProcess(runningBuild, context);
    List<OctopusCommandBuilder> commands = proc.createCommand();

    // Expect login + create
    assertThat(commands).hasSize(2);

    String[] login = commands.get(0).buildCommand();
    assertThat(login).containsExactly(
            "login",
            "--server",
            "https://octo.example",
            "--api-key",
            "API-KEY-123",
            "--no-prompt"
    );

    String[] create = commands.get(1).buildCommand();
    assertThat(create).contains(
            "release",
            "create",
            "--project",
            "MyProject",
            "--version",
            "1.2.3",
            "--output-format",
            "json",
            "--flag1",
            "--flag2",
            "value",
            "--no-prompt"
    );
  }

  @Test
  void createCommand_includesDeployAndWaitWhenDeployToAndWaitEnabled() {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getReleaseNumberKey(), "1.2.3");
    params.put(constants.getDeployToKey(), "EnvA");
    params.put(constants.getWaitForDeployments(), "true");

    when(context.getRunnerParameters()).thenReturn(params);

    CreateReleaseBuildProcess proc = new CreateReleaseBuildProcess(runningBuild, context);
    List<OctopusCommandBuilder> commands = proc.createCommand();

    // Expect login + create + deploy + wait
    assertThat(commands).hasSize(4);

    String[] deploy = commands.get(2).buildCommand();
    assertThat(deploy).contains(
            "release",
            "deploy",
            "--project",
            "MyProject",
            "--environment",
            "EnvA",
            "--output-format",
            "json",
            "--no-prompt"
    );

    String[] wait = commands.get(3).buildCommand();
    assertThat(wait).contains(
            "task",
            "wait",
            "--output-format",
            "json",
            "--no-prompt"
    );
  }
}
