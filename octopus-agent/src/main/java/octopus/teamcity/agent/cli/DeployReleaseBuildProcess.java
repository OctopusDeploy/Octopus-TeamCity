package octopus.teamcity.agent.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class DeployReleaseBuildProcess extends CLIBuildProcess {

  public DeployReleaseBuildProcess(
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

    final DeployReleaseCommand deploy = new DeployReleaseCommand(parameters);
    commands.add(deploy);

    if (wait) {
      commands.add(new WaitForTaskCommand(parameters, deploy::requireServerTaskId));
    }
    return commands;
  }

  @Override
  protected String getLogMessage() {
    return "Deploying Octopus Deploy release";
  }
}
