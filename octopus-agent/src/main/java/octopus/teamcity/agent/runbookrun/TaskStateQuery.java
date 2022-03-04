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

import com.octopus.sdk.api.TaskApi;
import com.octopus.sdk.domain.Task;
import com.octopus.sdk.model.task.TaskState;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TaskStateQuery {
  public static final Collection<TaskState> COMPLETED_STATES =
      Lists.newArrayList(
          TaskState.CANCELED, TaskState.FAILED, TaskState.SUCCESS, TaskState.TIMEDOUT);

  private static final Logger LOG = LogManager.getLogger();
  private final String serverTaskId;
  private final TaskApi tasks;

  public TaskStateQuery(final String serverTaskId, final TaskApi tasks) {
    this.serverTaskId = serverTaskId;
    this.tasks = tasks;
  }

  public String getServerTaskId() {
    return serverTaskId;
  }

  public TaskState getState() throws IOException {
    final Optional<Task> task = tasks.getById(serverTaskId);

    if (!task.isPresent()) {
      throw new IllegalStateException(
          "Unable to find task with id '" + serverTaskId + "' on Octopus server");
    }

    final TaskState taskState = task.get().getProperties().getState();
    LOG.debug("Task {} is now {}", serverTaskId, taskState);
    return taskState;
  }
}
