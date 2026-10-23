package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DeployReleaseBuildProcessTest {

  private static List<OctopusCommandBuilder> commandsFor(Map<String, String> params)
      throws RunBuildException {
    final AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(runningBuild.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));

    return new DeployReleaseBuildProcess(runningBuild, context).createCommand();
  }

  @Test
  void waitsForTheTaskTheDeploymentStarted() throws RunBuildException {
    final Map<String, String> params = withReleaseNumber("1.2.3");
    params.put(OctopusConstants.Instance.getWaitForDeployments(), "true");
    final List<OctopusCommandBuilder> commands = commandsFor(params);

    Commands.of(commands, DeployReleaseCommand.class)
        .readResponse("[{\"ServerTaskId\": \"task-xyz\"}]");

    assertThat(Commands.of(commands, WaitForTaskCommand.class).buildCommand()).contains("task-xyz");
  }

  @Test
  void doesNotWaitUnlessTheStepAsksTo() throws RunBuildException {
    final List<OctopusCommandBuilder> commands = commandsFor(withReleaseNumber("1.2.3"));

    assertThat(Commands.ranA(commands, DeployReleaseCommand.class)).isTrue();
    assertThat(Commands.ranA(commands, WaitForTaskCommand.class)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"latest", "Latest", " LATEST "})
  void failsBeforeRunningAnyCommandWhenTheReleaseNumberIsLatest(String releaseNumber) {
    assertThatThrownBy(() -> commandsFor(withReleaseNumber(releaseNumber)))
        .isInstanceOf(RunBuildException.class)
        .hasMessageContaining("'latest'");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void failsBeforeRunningAnyCommandWhenTheReleaseNumberIsEmpty(String releaseNumber) {
    assertThatThrownBy(() -> commandsFor(withReleaseNumber(releaseNumber)))
        .isInstanceOf(RunBuildException.class)
        .hasMessageContaining("release number is empty");
  }

  @Test
  void failsBeforeRunningAnyCommandWhenTheReleaseNumberIsMissing() {
    assertThatThrownBy(() -> commandsFor(new HashMap<>()))
        .isInstanceOf(RunBuildException.class)
        .hasMessageContaining("release number is empty");
  }

  @Test
  void deploysAReleaseNumberThatMerelyContainsLatest() throws RunBuildException {
    final List<OctopusCommandBuilder> commands = commandsFor(withReleaseNumber("1.0.0-latest"));

    assertThat(Commands.of(commands, DeployReleaseCommand.class).buildCommand())
        .containsSequence("--version", "1.0.0-latest");
  }

  private static Map<String, String> withReleaseNumber(String releaseNumber) {
    final Map<String, String> params = new HashMap<>();
    params.put(OctopusConstants.Instance.getReleaseNumberKey(), releaseNumber);
    return params;
  }
}
