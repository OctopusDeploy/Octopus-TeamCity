/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.agent.runbookrun;

import com.octopus.sdk.Repository;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.task.TaskState;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskWaiter {
  private static final Logger LOG = LogManager.getLogger();

  private final OctopusClient client;
  final Timer timer = new Timer("WaitForTask-");

  public TaskWaiter(final OctopusClient client) {
    this.client = client;
  }

  public TaskState waitForCompletion(final String serverTaskId) throws InterruptedException {
    final Repository repo = new Repository(client);
    final TaskStateQuery taskStateQuery = new TaskStateQuery(serverTaskId, repo.tasks());

    return waitForServerTaskToComplete(taskStateQuery);
  }

  public void cancel() {
    timer.cancel();
  }

  private TaskState waitForServerTaskToComplete(final TaskStateQuery taskStateQuery)
      throws InterruptedException {
    final CompletableFuture<TaskState> completionFuture = new CompletableFuture<>();
    final TimerTask taskStateChecker = new ServerTaskTimerTask(completionFuture, taskStateQuery);

    try {
      timer.scheduleAtFixedRate(taskStateChecker, 0, 1000);
      return completionFuture.get(50, TimeUnit.SECONDS);
    } catch (final ExecutionException e) {
      LOG.error("Task was terminated - " + e.getMessage());
      return TaskState.CANCELED;
    } catch (final TimeoutException e) {
      return TaskState.TIMEDOUT;
    } finally {
      timer.cancel();
    }
  }
}
