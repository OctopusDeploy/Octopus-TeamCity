package octopus.teamcity.agent.createrelease;

import com.octopus.sdk.Repository;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.commands.CreateReleaseCommandBody;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.createrelease.CreateReleaseUserData;

import java.util.Optional;

public class OctopusCreateReleaseBuildProcess extends InterruptableBuildProcess {

  private final BuildProgressLogger buildLogger;
  private final Repository respository

  public OctopusCreateReleaseBuildProcess(
      final BuildRunnerContext context, final Repository respository) {
    super(context);
    this.buildLogger = context.getBuild().getBuildLogger();
    this.respository = respository;
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Collating data for Create Release");
      final CreateReleaseUserData userData =
          new CreateReleaseUserData(context.getRunnerParameters());
      final CommonStepUserData commonStepUserData =
          new CommonStepUserData(context.getRunnerParameters());
      final String spaceName = commonStepUserData.getSpaceName().get();
      final CreateReleaseCommandBody body =
          new CreateReleaseCommandBody(
              spaceName, userData.getProjectName(), userData.getPackageVersion());

      userData.getReleaseVersion().ifPresent(body::setReleaseVersion);
      userData.getChannelName().ifPresent(body::setChannelIdOrName);

      buildLogger.message("Creating release");
      final Optional<Space> space = repo.spaces().getByName(spaceName);

      if (!space.isPresent()) {
        buildLogger.buildFailureDescription(
            "No space named '" + spaceName + "' existed on octopus server");
        complete(BuildFinishedStatus.FINISHED_FAILED);
        return;
      }

      final String response = space.get().executionsApi().createRelease(body);

      buildLogger.message("Release has been created: " + response);

      complete(BuildFinishedStatus.FINISHED_SUCCESS);
    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }
}
