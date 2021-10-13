package octopus.teamcity.agent.runbookrun;

import com.octopus.sdk.Repository;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.commands.ExecuteRunbookCommandBody;
import com.octopus.sdk.model.task.TaskState;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.runbookrun.RunbookRunUserData;

public class OctopusRunbookRunBuildProcess extends InterruptableBuildProcess {

  private final BuildProgressLogger buildLogger;
  private final OctopusClient client;

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
      final CommonStepUserData commonStepUserData =
          new CommonStepUserData(context.getRunnerParameters());
      final String spaceName = commonStepUserData.getSpaceName().get();

      final ExecuteRunbookCommandBody body =
          new ExecuteRunbookCommandBody(
              spaceName,
              userData.getProjectName(),
              userData.getEnvironmentNames(),
              userData.getRunbookName());

      final Repository repo = new Repository(client);
      buildLogger.message("Executing Runbook");
      final Optional<Space> space = repo.spaces().getByName(spaceName);

      if (!space.isPresent()) {
        buildLogger.buildFailureDescription(
            "No space named '" + spaceName + "' existed on octopus server");
        complete(BuildFinishedStatus.FINISHED_FAILED);
        return;
      }

      final String serverTaskId = space.get().executionsApi().executeRunbook(body);
      buildLogger.message(
          "Server task has been started for runbook '" + userData.getRunbookName() + "'");

      final TaskStateQuery taskStateQuery = new TaskStateQuery(serverTaskId, repo.tasks());
      final BuildFinishedStatus result = waitForServerTaskToComplete(taskStateQuery);
      complete(result);

    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }

  private BuildFinishedStatus waitForServerTaskToComplete(final TaskStateQuery taskStateQuery)
      throws InterruptedException {
    final Timer timer = new Timer("WaitForRunbook");
    final CompletableFuture<TaskState> completionFuture = new CompletableFuture<>();
    final TimerTask taskStateChecker = new ServerTaskTimerTask(completionFuture, taskStateQuery);

    try {
      timer.scheduleAtFixedRate(taskStateChecker, 0, 1000);
      final TaskState result = completionFuture.get(50, TimeUnit.SECONDS);
      if (result.equals(TaskState.SUCCESS)) {
        return BuildFinishedStatus.FINISHED_SUCCESS;
      }
    } catch (final ExecutionException e) {
      buildLogger.error("Failure in communications during runbook execution " + e.getMessage());
    } catch (final TimeoutException e) {
      buildLogger.error("Runbook failed to complete in expected timeout.");
    } finally {
      timer.cancel();
    }
    return BuildFinishedStatus.FINISHED_FAILED;
  }
}
