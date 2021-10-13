package octopus.teamcity.agent.createrelease;

import com.octopus.sdk.model.commands.CreateReleaseCommandBody;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.common.createrelease.CreateReleaseUserData;

public class OctopusCreateReleaseBuildProcess extends InterruptableBuildProcess {

  private final BuildProgressLogger buildLogger;
  private final OtherExecutionApi executionApi;

  public OctopusCreateReleaseBuildProcess(
      final BuildRunnerContext context, final OtherExecutionApi executionApi) {
    super(context);
    this.buildLogger = context.getBuild().getBuildLogger();
    this.executionApi = executionApi;
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Collating data for Create Release");
      final CreateReleaseUserData userData =
          new CreateReleaseUserData(context.getRunnerParameters());

      final CreateReleaseCommandBody body =
          new CreateReleaseCommandBody(
              userData.getSpaceName().get(),
              userData.getProjectName(),
              userData.getPackageVersion());
      userData.getReleaseVersion().ifPresent(body::setReleaseVersion);
      userData.getChannelName().ifPresent(body::setChannelIdOrName);

      buildLogger.message("Creating release");
      final String response = executionApi.createRelease(body);
      buildLogger.message("Release has been created: " + response);

      complete(BuildFinishedStatus.FINISHED_SUCCESS);
    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }
}
