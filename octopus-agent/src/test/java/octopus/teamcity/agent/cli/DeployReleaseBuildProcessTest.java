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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DeployReleaseBuildProcessTest {

  @Test
  void createCommand_includesLoginAndDeployWhenNoWait() {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getDeployToKey(), "EnvA,EnvB");

    when(context.getRunnerParameters()).thenReturn(params);

    DeployReleaseBuildProcess proc = new DeployReleaseBuildProcess(runningBuild, context);
    List<OctopusCommandBuilder> commands = proc.createCommand();

    // Expect login + deploy
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

    String[] deploy = commands.get(1).buildCommand();
    assertThat(deploy).contains(
        "release",
        "deploy",
        "--project",
        "MyProject",
        "--environment",
        "EnvA",
        "--environment",
        "EnvB",
        "--output-format",
        "json",
        "--no-prompt"
    );
  }

  @Test
  void createCommand_includesDeployAndWaitWhenWaitEnabled() {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getDeployToKey(), "EnvA");
    params.put(constants.getWaitForDeployments(), "true");

    when(context.getRunnerParameters()).thenReturn(params);

    DeployReleaseBuildProcess proc = new DeployReleaseBuildProcess(runningBuild, context);
    List<OctopusCommandBuilder> commands = proc.createCommand();

    // Expect login + deploy + wait
    assertThat(commands).hasSize(3);

    String[] deploy = commands.get(1).buildCommand();
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

    String[] wait = commands.get(2).buildCommand();
    assertThat(wait).contains(
        "task",
        "wait",
        "--output-format",
        "json",
        "--no-prompt"
    );
  }
}
