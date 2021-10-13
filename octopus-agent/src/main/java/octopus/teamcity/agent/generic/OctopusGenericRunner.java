/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent.generic;

import static jetbrains.buildServer.messages.DefaultMessagesInfo.BLOCK_TYPE_BUILD_STEP;

import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.http.OctopusClientFactory;
import com.octopus.sdk.operation.buildinformation.BuildInformationUploader;
import com.octopus.sdk.operation.pushpackage.PushPackageUploader;

import java.net.MalformedURLException;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentBuildRunner;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.buildinformation.BaseBuildVcsData;
import octopus.teamcity.agent.buildinformation.BuildVcsData;
import octopus.teamcity.agent.buildinformation.OctopusBuildInformationBuildProcess;
import octopus.teamcity.agent.createrelease.OctopusCreateReleaseBuildProcess;
import octopus.teamcity.agent.pushpackage.FileSelector;
import octopus.teamcity.agent.pushpackage.OctopusPushPackageBuildProcess;
import octopus.teamcity.agent.runbookrun.OctopusRunbookRunBuildProcess;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.common.connection.ConnectionUserData;

public class OctopusGenericRunner implements AgentBuildRunner {

  private static final String ACTIVITY_NAME = "OctopusDeploy";

  @Override
  public AgentBuildRunnerInfo getRunnerInfo() {
    return new AgentBuildRunnerInfo() {
      @Override
      public String getType() {
        return OctopusConstants.GENERIC_RUNNER_TYPE;
      }

      @Override
      public boolean canRun(final BuildAgentConfiguration buildAgentConfiguration) {
        return true;
      }
    };
  }

  @Override
  public BuildProcess createBuildProcess(
      final AgentRunningBuild runningBuild, final BuildRunnerContext context)
      throws RunBuildException {

    final BuildProgressLogger logger = runningBuild.getBuildLogger();
    final CommonStepUserData commonStepUserData =
        new CommonStepUserData(context.getRunnerParameters());
    final ConnectionUserData connectionUserData =
        new ConnectionUserData(context.getRunnerParameters());

    final String activityName = ACTIVITY_NAME + " - " + commonStepUserData.getStepType();
    logger.activityStarted(activityName, BLOCK_TYPE_BUILD_STEP);

    try {
      logger.message(
          "Creating connection to Octopus server @ "
              + connectionUserData.getServerUrl().toString());
      final ConnectData connection = TypeConverters.from(connectionUserData);
      final OctopusClient client = OctopusClientFactory.createClient(connection);
      return createBuildProcess(commonStepUserData.getStepType(), client, runningBuild, context);
    } catch (final MalformedURLException e) {
      throw new RunBuildException("Unable to decode supplied Octopus Server URL");
    }
  }

  private BuildProcess createBuildProcess(
      final String stepType,
      final OctopusClient client,
      final AgentRunningBuild runningBuild,
      final BuildRunnerContext context)
      throws RunBuildException {

    // TODO(tmm): Probably should make these do something nicer than switch on hard-strings, which
    // are otherwise available in the OctopusBuildStepCollection
    switch (stepType) {
      case ("build-information"):
        final BuildInformationUploader buildInformationUploader =
            BuildInformationUploader.create(client);
        final BaseBuildVcsData buildVcsData = BuildVcsData.create(runningBuild);
        return new OctopusBuildInformationBuildProcess(
            buildInformationUploader, buildVcsData, context);
      case ("push-package"):
        final PushPackageUploader pushPackageUploader = PushPackageUploader.create(client);
        final FileSelector fileSelector = new FileSelector(context.getWorkingDirectory().toPath());
        return new OctopusPushPackageBuildProcess(pushPackageUploader, fileSelector, context);
      case ("create-release"):
        return new OctopusCreateReleaseBuildProcess(context);
      case ("runbook-run"):
        return new OctopusRunbookRunBuildProcess(context);
      default:
        throw new RunBuildException("Unknown build step type " + stepType);
    }
  }
}
