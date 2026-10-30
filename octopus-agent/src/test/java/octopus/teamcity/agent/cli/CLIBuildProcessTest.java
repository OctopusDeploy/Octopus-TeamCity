package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildAgentSystemInfo;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CLIBuildProcessTest {

  @TempDir File buildTempDirectory;

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 137})
  void stopsAndFailsTheStepWhenACommandFails(final int failingExitCode) throws Exception {
    final ScriptedBuildProcess process = processWithExitCodes(failingExitCode, 0);

    process.start();

    assertThat(process.commandsRun).containsExactly("login");
    assertThat(process.waitFor()).isEqualTo(BuildFinishedStatus.FINISHED_FAILED);
  }

  @Test
  void runsEveryCommandAndSucceedsWhenAllCommandsSucceed() throws Exception {
    final ScriptedBuildProcess process = processWithExitCodes(0, 0);

    process.start();

    assertThat(process.commandsRun).containsExactly("login", "deploy");
    assertThat(process.waitFor()).isEqualTo(BuildFinishedStatus.FINISHED_SUCCESS);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Error: unknown flag: --progress",
        "Error: unknown shorthand flag: 'x' in -x",
      })
  void explainsAnUnknownFlagWhenTheStepHasAdditionalArguments(final String output) {
    assertThat(CLIBuildProcess.unknownArgumentHint("--progress", output))
        .contains("additional command line arguments");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void saysNothingExtraWhenTheStepHasNoAdditionalArguments(final String additionalArguments) {
    assertThat(
            CLIBuildProcess.unknownArgumentHint(
                additionalArguments, "Error: unknown flag: --progress"))
        .isNull();
  }

  @Test
  void saysNothingExtraWhenTheFailureIsNotAnUnknownFlag() {
    assertThat(CLIBuildProcess.unknownArgumentHint("--progress", "server returned HTTP status 401"))
        .isNull();
  }

  private ScriptedBuildProcess processWithExitCodes(final Integer... exitCodes) {
    final AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    final BuildAgentConfiguration agentConfiguration = mock(BuildAgentConfiguration.class);
    final BuildParametersMap buildParameters = mock(BuildParametersMap.class);
    when(runningBuild.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
    when(runningBuild.getBuildTempDirectory()).thenReturn(buildTempDirectory);
    when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
    when(runningBuild.getFailBuildOnExitCode()).thenReturn(true);
    when(agentConfiguration.getSystemInfo()).thenReturn(mock(BuildAgentSystemInfo.class));
    when(context.getRunnerParameters()).thenReturn(new HashMap<>());
    when(context.getBuildParameters()).thenReturn(buildParameters);
    when(buildParameters.getEnvironmentVariables()).thenReturn(new HashMap<>());

    return new ScriptedBuildProcess(runningBuild, context, exitCodes);
  }

  private static final class ScriptedBuildProcess extends CLIBuildProcess {
    private final Deque<Integer> exitCodes;
    private final List<String> commandsRun = new ArrayList<>();

    ScriptedBuildProcess(
        final AgentRunningBuild runningBuild,
        final BuildRunnerContext context,
        final Integer... exitCodes) {
      super(runningBuild, context);
      this.exitCodes = new ArrayDeque<>(Arrays.asList(exitCodes));
    }

    @Override
    protected List<OctopusCommandBuilder> createCommand() {
      return Arrays.asList(named("login"), named("deploy"));
    }

    @Override
    int runCommand(final OctopusCommandBuilder command) throws RunBuildException {
      commandsRun.add(command.buildCommand()[0]);
      return exitCodes.pop();
    }

    @Override
    protected String getLogMessage() {
      return "Running scripted commands";
    }

    private static OctopusCommandBuilder named(final String name) {
      return new OctopusCommandBuilder() {
        @Override
        protected String[] buildCommand(final boolean masked) {
          return new String[] {name};
        }
      };
    }
  }
}
