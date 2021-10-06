package octopus.teamcity.agent.runbookrun;

import com.octopus.sdk.Repository;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.commands.CreateReleaseCommandBody;
import com.octopus.sdk.model.commands.ExecuteRunbookCommandBody;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.createrelease.CreateReleaseUserData;
import octopus.teamcity.common.runbookrun.RunbookRunUserData;

public class OctopusRunbookRunBuildProcess extends InterruptableBuildProcess {

  private final BuildProgressLogger buildLogger;
  private OctopusClient client;

  public OctopusRunbookRunBuildProcess(BuildRunnerContext context, final OctopusClient client) {
    super(context);
    this.buildLogger = context.getBuild().getBuildLogger();
    this.client = client;
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Collating data for Execute Runbook");
      final RunbookRunUserData userData = new RunbookRunUserData(context.getRunnerParameters());
      final CommonStepUserData commonStepUserData = new CommonStepUserData(context.getRunnerParameters());
      final String spaceName = commonStepUserData.getSpaceName().get();

      final ExecuteRunbookCommandBody body = new ExecuteRunbookCommandBody(
          spaceName,
          userData.getProjectName(),
          userData.getEnvironmentNames(),
          userData.getRunbookName());

      final Repository repo = new Repository(client);
      buildLogger.message("Executing Runbook");
      final String serverTaskId = repo.spaces().getByName(spaceName).get().executionsApi().executeRunbook(body);

      buildLogger.message("Runbook execution has been queued in task: " + serverTaskId);


    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }
}
