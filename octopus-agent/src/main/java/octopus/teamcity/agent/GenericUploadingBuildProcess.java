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

package octopus.teamcity.agent;

import com.octopus.sdk.model.commands.CommandBody;
import com.octopus.sdk.operation.SpaceScopedOperation;

import java.util.Collection;
import java.util.Iterator;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildRunnerContext;

public abstract class GenericUploadingBuildProcess<T extends CommandBody>
    extends InterruptableBuildProcess {

  protected final SpaceScopedOperation<T, ?> operation;

  public GenericUploadingBuildProcess(
      final BuildRunnerContext context, final SpaceScopedOperation<T, ?> operation) {
    super(context);
    this.operation = operation;
  }

  @Override
  public void doStart() throws RunBuildException {
    buildLogger.message("Collating data for upload");
    final Collection<T> parameters = collateParameters();

    if (isInterrupted()) {
      return;
    }

    buildLogger.message("Starting data upload");
    if (upload(parameters)) {
      complete(BuildFinishedStatus.FINISHED_SUCCESS);
    } else {
      complete(BuildFinishedStatus.FINISHED_FAILED);
    }
  }

  protected boolean upload(final Collection<T> contexts) {
    boolean uploadSucceeded = true;

    final Iterator<T> it = contexts.iterator();
    while (it.hasNext() && uploadSucceeded) {
      uploadSucceeded = uploadItem(it.next());
    }

    it.forEachRemaining(context -> buildLogger.message(getIdentifier(context) + " -- SKIPPED"));

    return uploadSucceeded;
  }

  protected boolean uploadItem(final T context) {
    try {
      operation.execute(context);
      buildLogger.message(getIdentifier(context) + " -- UPLOADED");
      return true;
    } catch (final Throwable t) {
      buildLogger.error(getIdentifier(context) + " -- FAILED");
      buildLogger.buildFailureDescription(t.getMessage());
      logStackTrace(t);
      return false;
    }
  }

  protected abstract Collection<T> collateParameters() throws RunBuildException;

  protected abstract String getIdentifier(final T parameter);
}
