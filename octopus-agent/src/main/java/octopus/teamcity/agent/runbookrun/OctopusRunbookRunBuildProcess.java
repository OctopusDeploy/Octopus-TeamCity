package octopus.teamcity.agent.runbookrun;

import com.octopus.sdk.model.commands.ExecuteRunbookCommandBody;
import com.octopus.sdk.model.task.TaskState;
import com.octopus.sdk.operation.executionapi.ExecuteRunbook;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.runbookrun.RunbookRunUserData;

public class OctopusRunbookRunBuildProcess extends InterruptableBuildProcess {

  private final BuildProgressLogger buildLogger;
  private final ExecuteRunbook executor;
  private final TaskWaiter waiter;

  public OctopusRunbookRunBuildProcess(
      final BuildRunnerContext context, final ExecuteRunbook executor, final TaskWaiter waiter) {
    super(context);
    this.buildLogger = context.getBuild().getBuildLogger();
    this.waiter = waiter;
    this.executor = executor;
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Collating data for Execute Runbook");
      final RunbookRunUserData userData = new RunbookRunUserData(context.getRunnerParameters());
      final CommonStepUserData commonStepUserData =
          new CommonStepUserData(context.getRunnerParameters());
      final String spaceName = commonStepUserData.getSpaceName();

      final ExecuteRunbookCommandBody body =
          new ExecuteRunbookCommandBody(
              spaceName,
              userData.getProjectName(),
              userData.getEnvironmentNames(),
              userData.getRunbookName());

      final String serverTaskId = executor.execute(body);

      buildLogger.message(
          "Server task has been started for runbook '" + userData.getRunbookName() + "'");

      final TaskState taskCompletionState = waiter.waitForCompletion(serverTaskId);

      if (taskCompletionState.equals(TaskState.SUCCESS)) {
        complete(BuildFinishedStatus.FINISHED_SUCCESS);
      } else {
        complete(BuildFinishedStatus.FINISHED_FAILED);
      }
    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }
}
