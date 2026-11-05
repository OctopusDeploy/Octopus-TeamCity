package octopus.teamcity.agent.cli;

import static jetbrains.buildServer.messages.DefaultMessagesInfo.BLOCK_TYPE_BUILD_STEP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildAgentSystemInfo;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

class CLIBuildProcessTest {

  @TempDir File buildTempDirectory;

  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);

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
  @ValueSource(strings = {"", "Preview", "1.0+", "2.0+", "3.0+"})
  void extractsTheBundledCliWhateverOctopusVersionTheStepCarries(final String octopusVersion)
      throws Exception {
    final ScriptedBuildProcess process = processWithExitCodes(0, 0);
    process.runnerParameters.put(OctopusConstants.Instance.getOctopusVersion(), octopusVersion);

    process.start();

    assertThat(process.commandsRun).containsExactly("login", "deploy");
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

  @Test
  void opensTheBuildLogBlockItCloses() throws Exception {
    final ScriptedBuildProcess process = processWithExitCodes(0, 0);

    process.start();
    process.waitFor();

    final InOrder inOrder = inOrder(logger);
    inOrder.verify(logger).activityStarted("Octopus Deploy", BLOCK_TYPE_BUILD_STEP);
    inOrder.verify(logger).activityFinished("Octopus Deploy", BLOCK_TYPE_BUILD_STEP);
  }

  private ScriptedBuildProcess processWithExitCodes(final Integer... exitCodes) {
    final AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    final BuildAgentConfiguration agentConfiguration = mock(BuildAgentConfiguration.class);
    final BuildParametersMap buildParameters = mock(BuildParametersMap.class);
    when(runningBuild.getBuildLogger()).thenReturn(logger);
    when(runningBuild.getBuildTempDirectory()).thenReturn(buildTempDirectory);
    when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
    when(runningBuild.getFailBuildOnExitCode()).thenReturn(true);
    when(agentConfiguration.getSystemInfo()).thenReturn(mock(BuildAgentSystemInfo.class));
    final Map<String, String> runnerParameters = new HashMap<>();
    when(context.getRunnerParameters()).thenReturn(runnerParameters);
    when(context.getBuildParameters()).thenReturn(buildParameters);
    when(buildParameters.getEnvironmentVariables()).thenReturn(new HashMap<>());

    return new ScriptedBuildProcess(runningBuild, context, runnerParameters, exitCodes);
  }

  private static final class ScriptedBuildProcess extends CLIBuildProcess {
    private final Deque<Integer> exitCodes;
    private final List<String> commandsRun = new ArrayList<>();
    private final Map<String, String> runnerParameters;

    ScriptedBuildProcess(
        final AgentRunningBuild runningBuild,
        final BuildRunnerContext context,
        final Map<String, String> runnerParameters,
        final Integer... exitCodes) {
      super(runningBuild, context);
      this.runnerParameters = runnerParameters;
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
