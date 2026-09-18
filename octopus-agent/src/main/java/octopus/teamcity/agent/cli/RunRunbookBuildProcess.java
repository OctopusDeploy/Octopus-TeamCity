package octopus.teamcity.agent.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class RunRunbookBuildProcess extends CLIBuildProcess {

  public RunRunbookBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
  }

  @Override
  protected List<OctopusCommandBuilder> createCommand() {
    final OctopusConstants constants = OctopusConstants.Instance;
    List<OctopusCommandBuilder> commands = new ArrayList<>();
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

    commands.add(CommandHelper.login(parameters));

    final RunbookRunCommand runbookRun = new RunbookRunCommand(parameters);
    commands.add(runbookRun);

    if (wait) {
      commands.add(new WaitForTaskCommand(parameters, runbookRun::requireServerTaskId));
    }
    return commands;
  }

  @Override
  protected String getLogMessage() {
    return "Running Octopus Deploy runbook";
  }
}
