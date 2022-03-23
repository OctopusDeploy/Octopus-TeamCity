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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.GenericUploadingBuildProcess;
import octopus.teamcity.agent.generic.TypeConverters;
import octopus.teamcity.common.buildinfo.BuildInfoUserData;

public class OctopusBuildInformationBuildProcess
    extends GenericUploadingBuildProcess<BuildInformationUploaderContext> {

  private final BaseBuildVcsData buildVcsData;

  public OctopusBuildInformationBuildProcess(
      final BuildInformationUploader uploader,
      final BaseBuildVcsData buildVcsData,
      final BuildRunnerContext context) {
    super(context, uploader);
    this.buildVcsData = buildVcsData;
  }

  @Override
  public List<BuildInformationUploaderContext> collateParameters() throws RunBuildException {
    final Map<String, String> parameters = context.getRunnerParameters();
    final AgentRunningBuild runningBuild = context.getBuild();
    final Map<String, String> sharedConfigParameters = runningBuild.getSharedConfigParameters();

    final BuildInfoUserData buildInfoUserData = new BuildInfoUserData(parameters);

    final URL buildUrl = constructBuildUrl(sharedConfigParameters.get("externalBuildURL"));

    final BuildInformationUploaderContextBuilder buildInfoBuilder =
        new BuildInformationUploaderContextBuilder()
            .withBuildEnvironment("TeamCity")
            .withSpaceIdOrName(buildInfoUserData.getSpaceName())
            .withPackageVersion(buildInfoUserData.getPackageVersion())
            .withVcsType(sharedConfigParameters.get("octopus_vcstype"))
            .withVcsRoot(sharedConfigParameters.get("vcsroot.url"))
            .withVcsCommitNumber(sharedConfigParameters.get("build.vcs.number"))
            .withBranch(buildVcsData.getBranchName())
            .withCommits(buildVcsData.getCommits())
            .withBuildUrl(buildUrl)
            .withBuildNumber(runningBuild.getBuildNumber())
            .withOverwriteMode(TypeConverters.from(buildInfoUserData.getOverwriteMode()));

    return buildInfoUserData.getPackageIds().stream()
        .map(packageId -> buildInfoBuilder.withPackageId(packageId).build())
        .collect(Collectors.toList());
  }

  private URL constructBuildUrl(final String externalBuildUrl)
      throws RunBuildException {

    try {
      return new URL(externalBuildUrl);
    } catch (final MalformedURLException e) {
      throw new RunBuildException("Failed to construct a build URL from " + externalBuildUrl, e);
    }
  }

  @Override
  protected String getIdentifier(final BuildInformationUploaderContext parameter) {
    return parameter.getPackageId();
  }
}
