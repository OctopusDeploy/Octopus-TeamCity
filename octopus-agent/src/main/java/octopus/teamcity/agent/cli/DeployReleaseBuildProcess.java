package octopus.teamcity.agent.cli;

import static octopus.teamcity.agent.cli.CommandUtils.getServerTaskId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class DeployReleaseBuildProcess extends CLIBuildProcess {
  private String serverTaskId;

  public DeployReleaseBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
  }

  @Override
  public void processOutput(String output, int exitCode) {
    logger.message("Exit code: " + exitCode);
    if (exitCode == 0) {
      final OctopusConstants constants = OctopusConstants.Instance;
      final Map<String, String> parameters = getContext().getRunnerParameters();
      final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));
      if (wait && CommandUtils.isDeployReleaseCommand(output)) {
        serverTaskId = getServerTaskId(output);
      }
    }
  }

  @Override
  protected List<OctopusCommandBuilder> createCommand() {
    final OctopusConstants constants = OctopusConstants.Instance;
    List<OctopusCommandBuilder> commands = new ArrayList<>();
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

    commands.add(CommandHelper.login(parameters));
    commands.add(
        new OctopusCommandBuilder() {
          @Override
          protected String[] buildCommand(boolean masked) {
            return CommandHelper.deployRelease(parameters, null);
          }
        });

    if (wait) {
      commands.add(
          new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
              return CommandHelper.wait(parameters, serverTaskId);
            }
          });
    }
    return commands;
  }

  @Override
  protected String getLogMessage() {
    return "Deploying Octopus Deploy release";
  }
}
