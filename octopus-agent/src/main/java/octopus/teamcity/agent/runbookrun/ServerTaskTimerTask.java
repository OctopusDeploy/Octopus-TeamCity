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

import com.octopus.sdk.model.task.TaskState;

import java.io.IOException;
import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class ServerTaskTimerTask extends TimerTask {
  private static final Logger LOG = LogManager.getLogger();
  private final CompletableFuture<TaskState> completionFuture;
  private final TaskStateQuery taskStateQuery;
  private final Collection<TaskState> completedStates =
      Lists.newArrayList(
          TaskState.CANCELED, TaskState.FAILED, TaskState.SUCCESS, TaskState.TIMEDOUT);

  public ServerTaskTimerTask(
      final CompletableFuture<TaskState> completionFuture, final TaskStateQuery taskStateQuery) {
    this.completionFuture = completionFuture;
    this.taskStateQuery = taskStateQuery;
  }

  @Override
  public void run() {
    final TaskState taskState;
    try {
      taskState = taskStateQuery.getState();
      if (completedStates.contains(taskState)) {
        completionFuture.complete(taskState);
      }
    } catch (final IOException e) {
      LOG.error("Failed to connect to Ocotpus Server, will continue trying until timeout");
    } catch (IllegalStateException e) {
      LOG.error(e.getMessage());
    }
  }
}
