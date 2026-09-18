package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class DeployReleaseBuildProcessTest {

  private static List<OctopusCommandBuilder> commandsFor(Map<String, String> params) {
    final AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(runningBuild.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));

    return new DeployReleaseBuildProcess(runningBuild, context).createCommand();
  }

  @Test
  void waitsForTheTaskTheDeploymentStarted() {
    final Map<String, String> params = new HashMap<>();
    params.put(OctopusConstants.Instance.getWaitForDeployments(), "true");
    final List<OctopusCommandBuilder> commands = commandsFor(params);

    Commands.of(commands, DeployReleaseCommand.class)
        .readResponse("[{\"ServerTaskId\": \"task-xyz\"}]");

    assertThat(Commands.of(commands, WaitForTaskCommand.class).buildCommand()).contains("task-xyz");
  }

  @Test
  void doesNotWaitUnlessTheStepAsksTo() {
    final List<OctopusCommandBuilder> commands = commandsFor(new HashMap<>());

    assertThat(Commands.ranA(commands, DeployReleaseCommand.class)).isTrue();
    assertThat(Commands.ranA(commands, WaitForTaskCommand.class)).isFalse();
  }
}
