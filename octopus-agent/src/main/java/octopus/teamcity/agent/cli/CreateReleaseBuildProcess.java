package octopus.teamcity.agent.cli;

import static octopus.teamcity.agent.cli.CommandUtils.getServerTaskId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class CreateReleaseBuildProcess extends CLIBuildProcess {
  static final String RELEASE_URL_PARAMETER = "octopus.release.url";
  static final String RELEASE_NUMBER_PARAMETER = "octopus.release.number";

  private String autoCreatedReleaseNumber;
  private String serverTaskId;
  private String spaceId;

  public CreateReleaseBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
  }

  @Override
  public void processOutput(String output, int exitCode) {
    logger.message("Exit code: " + exitCode);
    if (exitCode == 0) {
      final OctopusConstants constants = OctopusConstants.Instance;
      final Map<String, String> parameters = getContext().getRunnerParameters();
      final String deployTo = parameters.get(constants.getDeployToKey());
      final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

      if (CommandUtils.isSpaceViewCommand(output)) {
        spaceId = CommandUtils.getSpaceId(output);
      } else if (CommandUtils.isCreateReleaseCommand(output)) {
        if (StringUtils.isNotBlank(deployTo)) {
          autoCreatedReleaseNumber = CommandUtils.getReleaseVersion(output);
        }
        publishReleaseLink(output, parameters.get(constants.getServerKey()));
      } else if (wait && CommandUtils.isDeployReleaseCommand(output)) {
        serverTaskId = getServerTaskId(output);
      }
    }
  }

  /**
   * Points at the release the step has just created, both in the log and as parameters later steps
   * can read. The release exists either way, so nothing here is allowed to fail the step: an
   * unreadable response costs the link and nothing else.
   */
  private void publishReleaseLink(final String createReleaseOutput, final String serverUrl) {
    try {
      final Optional<String> link =
          ReleaseLink.of(serverUrl, spaceId, CommandUtils.getReleaseId(createReleaseOutput));
      if (!link.isPresent()) {
        logger.warning(
            "Could not work out where the release lives in Octopus Deploy, "
                + "so this step will not link to it.");
        return;
      }

      logger.message("View this release in Octopus Deploy: " + link.get());
      logger.message(setParameter(RELEASE_URL_PARAMETER, link.get()));
      logger.message(
          setParameter(
              RELEASE_NUMBER_PARAMETER, CommandUtils.getReleaseVersion(createReleaseOutput)));
    } catch (final RuntimeException e) {
      logger.warning(
          "Could not read the created release from the CLI's response, "
              + "so this step will not link to it: "
              + e.getMessage());
    }
  }

  private static String setParameter(final String name, final String value) {
    final Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("name", name);
    attributes.put("value", value);
    return ServiceMessage.asString("setParameter", attributes);
  }

  @Override
  protected List<OctopusCommandBuilder> createCommand() {
    final OctopusConstants constants = OctopusConstants.Instance;
    List<OctopusCommandBuilder> commands = new ArrayList<>();
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final String deployTo = parameters.get(constants.getDeployToKey());
    final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));
    final String space = parameters.get(constants.getSpaceName());

    commands.add(CommandHelper.login(parameters));

    // A step configured with a space id already knows what the link to the release needs.
    if (CommandUtils.isSpaceId(space)) {
      spaceId = space.trim();
    } else {
      commands.add(CommandHelper.spaceView(parameters));
    }

    commands.add(CommandHelper.createRelease(parameters));

    if (StringUtils.isNotBlank(deployTo)) {
      commands.add(
          new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
              return CommandHelper.deployRelease(parameters, autoCreatedReleaseNumber);
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
    }
    return commands;
  }

  @Override
  protected String getLogMessage() {
    return "Creating Octopus Deploy release";
  }
}
