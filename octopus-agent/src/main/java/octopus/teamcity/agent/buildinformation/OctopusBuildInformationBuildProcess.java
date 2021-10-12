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

package octopus.teamcity.agent.buildinformation;

import com.octopus.sdk.operation.buildinformation.BuildInformationUploader;
import com.octopus.sdk.operation.buildinformation.BuildInformationUploaderContext;
import com.octopus.sdk.operation.buildinformation.BuildInformationUploaderContextBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.agent.generic.TypeConverters;
import octopus.teamcity.common.buildinfo.BuildInfoUserData;
import octopus.teamcity.common.commonstep.CommonStepUserData;

public class OctopusBuildInformationBuildProcess extends InterruptableBuildProcess {

  private final BaseBuildVcsData buildVcsData;
  private final BuildProgressLogger buildLogger;
  private final BuildInformationUploader uploader;

  public OctopusBuildInformationBuildProcess(
      final BuildInformationUploader uploader,
      final BaseBuildVcsData buildVcsData,
      final BuildRunnerContext context) {
    super(context);
    this.uploader = uploader;
    this.buildVcsData = buildVcsData;
    this.buildLogger = context.getBuild().getBuildLogger();
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Collating data for upload");
      final List<BuildInformationUploaderContext> buildInformationContexts =
          collateBuildInformation();

      if (isInterrupted()) {
        complete(BuildFinishedStatus.INTERRUPTED);
        return;
      }

      buildLogger.message("Starting data upload");
      if (upload(buildInformationContexts)) {
        complete(BuildFinishedStatus.FINISHED_SUCCESS);
      } else {
        complete(BuildFinishedStatus.FINISHED_FAILED);
      }
    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }

  public List<BuildInformationUploaderContext> collateBuildInformation()
      throws MalformedURLException {
    final Map<String, String> parameters = context.getRunnerParameters();
    final AgentRunningBuild runningBuild = context.getBuild();
    final Map<String, String> sharedConfigParameters = runningBuild.getSharedConfigParameters();

    final CommonStepUserData commonStepUserData = new CommonStepUserData(parameters);
    final BuildInfoUserData buildInfoUserData = new BuildInfoUserData(parameters);
    final String buildId = Long.toString(runningBuild.getBuildId());

    final BuildInformationUploaderContextBuilder buildInfoBuilder =
        new BuildInformationUploaderContextBuilder()
            .withBuildEnvironment("TeamCity")
            .withSpaceName(commonStepUserData.getSpaceName().orElse(null))
            .withPackageVersion(buildInfoUserData.getPackageVersion())
            .withVcsType(sharedConfigParameters.get("octopus_vcstype"))
            .withVcsRoot(sharedConfigParameters.get("vcsroot.url"))
            .withVcsCommitNumber(sharedConfigParameters.get("build.vcs.number"))
            .withBranch(buildVcsData.getBranchName())
            .withCommits(buildVcsData.getCommits())
            .withBuildUrl(
                new URL(
                    runningBuild.getAgentConfiguration().getServerUrl()
                        + "/viewLog.html?buildId="
                        + buildId))
            .withBuildNumber(runningBuild.getBuildNumber())
            .withOverwriteMode(TypeConverters.from(buildInfoUserData.getOverwriteMode()));

    return buildInfoUserData.getPackageIds().stream()
        .map(packageId -> buildInfoBuilder.withPackageId(packageId).build())
        .collect(Collectors.toList());
  }

  private boolean upload(final List<BuildInformationUploaderContext> buildInformationContexts) {
    boolean allUploadsSuccessful = true;
    for (final BuildInformationUploaderContext context : buildInformationContexts) {
      buildLogger.message("Uploading " + context.getPackageId());
      try {
        uploader.upload(context);
      } catch (final Throwable t) {
        allUploadsSuccessful = false;
        buildLogger.error("Upload of information failed for packageId: " + context.getPackageId());
        buildLogger.error(t.getMessage());
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        buildLogger.debug(pw.toString());
      }
    }

    return allUploadsSuccessful;
  }
}
