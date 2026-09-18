package octopus.teamcity.agent.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import octopus.teamcity.common.ReleaseSummary;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class CreateReleaseBuildProcess extends CLIBuildProcess {
  static final String RELEASE_URL_PARAMETER = "octopus.release.url";
  static final String RELEASE_NUMBER_PARAMETER = "octopus.release.number";

  private String autoCreatedReleaseNumber;
  private String spaceId;

  public CreateReleaseBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    super(runningBuild, context);
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
      commands.add(new SpaceViewCommand(parameters, id -> spaceId = id));
    }

    commands.add(new CreateReleaseCommand(parameters, this::releaseCreated));

    if (StringUtils.isNotBlank(deployTo)) {
      final DeployReleaseCommand deploy =
          new DeployReleaseCommand(parameters, () -> autoCreatedReleaseNumber);
      commands.add(deploy);

      if (wait) {
        commands.add(new WaitForTaskCommand(parameters, deploy::requireServerTaskId));
      }
    }
    return commands;
  }

  /** What the step makes of the release it has just created, as soon as the CLI answers. */
  private void releaseCreated(final CreateReleaseResponse response) {
    final OctopusConstants constants = OctopusConstants.Instance;
    final Map<String, String> parameters = getContext().getRunnerParameters();

    if (StringUtils.isNotBlank(parameters.get(constants.getDeployToKey()))) {
      // The deployment about to run has nothing to deploy without this, so say so plainly
      // rather than leaving the CLI to fail on an empty --version.
      autoCreatedReleaseNumber =
          response
              .version()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Could not read the created release's number from the CLI's response, "
                              + "so there is no release to deploy. The response was: "
                              + CommandUtils.loggableOutput(response.output())));
    }

    publishReleaseLink(response, parameters);
  }

  /**
   * Points at the release the step has just created, both in the log and as parameters later steps
   * can read. The release exists either way, so nothing here is allowed to fail the step: an
   * unreadable response costs the link and nothing else.
   */
  private void publishReleaseLink(
      final CreateReleaseResponse response, final Map<String, String> parameters) {
    final OctopusConstants constants = OctopusConstants.Instance;
    final Optional<String> link =
        ReleaseLink.of(
            parameters.get(constants.getServerKey()), spaceId, response.id().orElse(null));
    if (!link.isPresent()) {
      logger.warning(
          "Could not work out where the release lives in Octopus Deploy, "
              + "so this step will not link to it. The space was "
              + (StringUtils.isBlank(spaceId) ? "not known" : spaceId)
              + " and the release create response was: "
              + CommandUtils.loggableOutput(response.output()));
      return;
    }

    if (response.version().isPresent()) {
      logger.message(
          "Created release "
              + response.version().get()
              + projectDescription(parameters.get(constants.getProjectNameKey()))
              + " in space "
              + spaceId);
    }

    logger.message("View this release in Octopus Deploy: " + link.get());
    logger.message(setParameter(RELEASE_URL_PARAMETER, link.get()));
    if (response.version().isPresent()) {
      logger.message(setParameter(RELEASE_NUMBER_PARAMETER, response.version().get()));
    }

    publishSummaryForTheBuildOverview(
        new ReleaseSummary(link.get(), response.version().orElse(null)));
  }

  /**
   * Hands the release to the server as a hidden artifact, which is what the build overview reads to
   * link to it once the build is over - the log line above only helps while the log is being read.
   */
  private void publishSummaryForTheBuildOverview(final ReleaseSummary release) {
    final File summary =
        new File(
            getContext().getBuild().getBuildTempDirectory(),
            ReleaseSummary.artifactNameFor(getContext().getId()));
    try (OutputStream destination = new FileOutputStream(summary)) {
      release.writeTo(destination);
    } catch (final IOException e) {
      logger.warning(
          "Could not record the release for the build overview, "
              + "so only this log will link to it: "
              + e.getMessage());
      return;
    }

    logger.message(
        ServiceMessage.asString(
            "publishArtifacts",
            summary.getAbsolutePath() + " => " + ReleaseSummary.ARTIFACT_DIRECTORY));
  }

  private static String projectDescription(final String projectName) {
    return StringUtils.isBlank(projectName) ? "" : " of project " + projectName;
  }

  private static String setParameter(final String name, final String value) {
    final Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("name", name);
    attributes.put("value", value);
    return ServiceMessage.asString("setParameter", attributes);
  }

  @Override
  protected String getLogMessage() {
    return "Creating Octopus Deploy release";
  }
}
