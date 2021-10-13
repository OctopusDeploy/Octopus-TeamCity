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

import static octopus.teamcity.agent.logging.BuildLogAppender.BUILD_LOG_APPENDER_NAME;

import com.octopus.sdk.logging.SdkLogAppenderHelper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.logging.BuildLogAppender;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import org.apache.logging.log4j.Level;

public abstract class InterruptableBuildProcess implements BuildProcess {

  private volatile boolean interrupted = false;
  private final CompletableFuture<BuildFinishedStatus> uploadFinishedFuture =
      new CompletableFuture<>();

  protected final BuildRunnerContext context;

  public InterruptableBuildProcess(BuildRunnerContext context) {
    this.context = context;
  }

  protected void complete(final BuildFinishedStatus status) {
    uploadFinishedFuture.complete(status);
  }

  public abstract void doStart() throws RunBuildException;

  @Override
  public void start() throws RunBuildException {
    try (final SdkLogAppenderHelper ignored =
        SdkLogAppenderHelper.registerLogAppender(
            BuildLogAppender.createAppender(
                BUILD_LOG_APPENDER_NAME, context.getBuild().getBuildLogger()),
            isVerboseLogging(context.getRunnerParameters()))) {
      doStart();
    }
  }

  @Override
  public boolean isInterrupted() {
    return interrupted;
  }

  @Override
  public boolean isFinished() {
    return uploadFinishedFuture.isDone();
  }

  @Override
  public void interrupt() {
    interrupted = true;
  }

  @Override
  public BuildFinishedStatus waitFor() {
    try {
      return uploadFinishedFuture.get();
    } catch (final InterruptedException e) {
      return BuildFinishedStatus.INTERRUPTED;
    } catch (final ExecutionException e) {
      return BuildFinishedStatus.FINISHED_FAILED;
    }
  }

  private Level isVerboseLogging(final Map<String, String> runnerParameters) {
    if (runnerParameters != null
        && Boolean.parseBoolean(runnerParameters.get(CommonStepPropertyNames.VERBOSE_LOGGING))) {
      return Level.DEBUG;
    }
    return Level.INFO;
  }

}
