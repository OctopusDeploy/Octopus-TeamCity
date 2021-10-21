package octopus.teamcity.agent.runbookrun;

import static java.util.Collections.singletonList;

import com.octopus.sdk.model.commands.ExecuteRunbookCommandBody;
import com.octopus.sdk.model.task.TaskState;
import com.octopus.sdk.operation.executionapi.ExecuteRunbook;

import java.util.Collection;

import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.GenericUploadingBuildProcess;
import octopus.teamcity.common.runbookrun.RunbookRunUserData;

public class OctopusRunbookRunBuildProcess
    extends GenericUploadingBuildProcess<ExecuteRunbookCommandBody> {

  private final ExecuteRunbook executor;
  private final TaskWaiter waiter;

  public OctopusRunbookRunBuildProcess(
      final BuildRunnerContext context, final ExecuteRunbook executor, final TaskWaiter waiter) {
    super(context, executor);
    this.executor = executor;
    this.waiter = waiter;
  }

  @Override
  protected boolean uploadItem(final ExecuteRunbookCommandBody parameters) {
    try {
      final String serverTaskId = executor.execute(parameters);
      buildLogger.message(getIdentifier(parameters) + " -- Executing");

      buildLogger.message(
          "Server task with id '"
              + serverTaskId
              + "' has been started for runbook '"
              + getIdentifier(parameters));

      final TaskState taskCompletionState = waiter.waitForCompletion(serverTaskId);

      return taskCompletionState.equals(TaskState.SUCCESS);
    } catch (final Throwable t) {
      buildLogger.error(getIdentifier(parameters) + " -- FAILED");
      buildLogger.buildFailureDescription(t.getMessage());
      logStackTrace(t);
      return false;
    }
  }

  @Override
  protected Collection<ExecuteRunbookCommandBody> collateParameters() {
    final RunbookRunUserData userData = new RunbookRunUserData(context.getRunnerParameters());
    final String spaceName = userData.getSpaceName();

    final ExecuteRunbookCommandBody body =
        new ExecuteRunbookCommandBody(
            spaceName,
            userData.getProjectName(),
            userData.getEnvironmentNames(),
            userData.getRunbookName());
    userData.getSnapshotName().ifPresent(body::setSnapshot);

    return singletonList(body);
  }

  @Override
  protected String getIdentifier(final ExecuteRunbookCommandBody parameters) {
    return parameters.getRunbookName();
  }

  @Override
  public void interrupt() {
    super.interrupt();
    waiter.cancel();
  }
}
