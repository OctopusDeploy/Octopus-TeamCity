package octopus.teamcity.agent.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.ReleaseNumber;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class DeployReleaseBuildProcess extends CLIBuildProcess {

  public DeployReleaseBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
  }

  @Override
  protected List<OctopusCommandBuilder> createCommand() throws RunBuildException {
    final OctopusConstants constants = OctopusConstants.Instance;
    List<OctopusCommandBuilder> commands = new ArrayList<>();
    final Map<String, String> parameters = getContext().getRunnerParameters();
    requireExactReleaseNumber(parameters.get(constants.getReleaseNumberKey()));
    final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

    commands.add(CommandHelper.login(parameters));

    final DeployReleaseCommand deploy = new DeployReleaseCommand(parameters);
    commands.add(deploy);

    if (wait) {
      commands.add(new WaitForTaskCommand(parameters, deploy::requireServerTaskId));
    }
    return commands;
  }

  /** The Go CLI deploys the release with exactly this version; it has no "latest". */
  private static void requireExactReleaseNumber(String releaseNumber) throws RunBuildException {
    if (StringUtils.isBlank(releaseNumber)) {
      throw new RunBuildException(
          "The release number is empty. Enter the release number to deploy, e.g. %octo.releaseNumber%"
              + " or %build.number%.");
    }
    if (ReleaseNumber.isLatest(releaseNumber)) {
      throw new RunBuildException(ReleaseNumber.LATEST_NOT_SUPPORTED);
    }
  }

  @Override
  protected String getLogMessage() {
    return "Deploying Octopus Deploy release";
  }
}
