package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.junit.jupiter.api.Test;

class BuildInformationBuildProcessTest {

  @Test
  void failsTheStepWhenTheBuildInformationCannotBeGathered() {
    final AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    final BuildAgentConfiguration agentConfiguration = mock(BuildAgentConfiguration.class);
    when(runningBuild.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
    when(runningBuild.getCheckoutDirectory()).thenReturn(new File("checkout"));
    when(runningBuild.getSharedConfigParameters()).thenReturn(new HashMap<>());
    when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
    when(agentConfiguration.getServerUrl()).thenReturn("http://teamcity.invalid");
    when(context.getRunnerParameters()).thenReturn(new HashMap<>());
    when(context.getBuild()).thenThrow(new IllegalStateException("TeamCity is unreachable"));

    final BuildInformationBuildProcess process =
        new BuildInformationBuildProcess(runningBuild, context);

    assertThatThrownBy(process::createCommand)
        .isInstanceOf(RunBuildException.class)
        .hasRootCauseMessage("TeamCity is unreachable");
  }
}
