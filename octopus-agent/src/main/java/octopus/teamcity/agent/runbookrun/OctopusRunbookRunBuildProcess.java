package octopus.teamcity.agent.runbookrun;

import com.google.common.collect.Lists;
import com.octopus.openapi.model.TaskState;
import com.octopus.sdk.Repository;
import com.octopus.sdk.api.TaskApi;
import com.octopus.sdk.domain.Space;
import com.octopus.sdk.domain.Task;
import com.octopus.sdk.http.OctopusClient;

import com.octopus.sdk.model.commands.ExecuteRunbookCommandBody;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.runbookrun.RunbookRunUserData;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

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

      if(!space.isPresent()) {
        buildLogger.buildFailureDescription("No space named '" + spaceName + "' existed on octopus server")
        complete(BuildFinishedStatus.FINISHED_FAILED);
        return;
      }

      final String serverTaskId = space.get().executionsApi().executeRunbook(body);
      buildLogger.message("Server task has been started for runbook '" + userData.getRunbookName() + ";");

      final BuildFinishedStatus result = waitForTask(serverTaskId, repo.tasks());
      complete(result);

    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }

  private BuildFinishedStatus waitForTask(final String serverTaskId, final TaskApi tasks) throws IOException {
    Optional<Task> task = tasks.getById(serverTaskId);

    if(!task.isPresent()) {
      buildLogger.buildFailureDescription("Unable to find task with id '" + serverTaskId + "' on Octopus server");
      complete(BuildFinishedStatus.FINISHED_FAILED);
      return BuildFinishedStatus.FINISHED_FAILED;
    }
    final TaskState state = task.get().getProperties().getState();
    buildLogger.message("Server task '" + serverTaskId + "' is currently ");

    final Collection<TaskState> completedStates = Lists.newArrayList(
        TaskState.CANCELED, TaskState.FAILED, TaskState.SUCCESS,  TaskState.TIMEDOUT);
    )

    while(!completedStates.contains(state)) {
      Thread.sleep(1000);
    }

  }

}
