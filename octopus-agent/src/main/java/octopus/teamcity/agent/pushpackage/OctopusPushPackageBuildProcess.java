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

package octopus.teamcity.agent.pushpackage;

import com.octopus.sdk.operation.pushpackage.PushPackageUploader;
import com.octopus.sdk.operation.pushpackage.PushPackageUploaderContext;
import com.octopus.sdk.operation.pushpackage.PushPackageUploaderContextBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;
import octopus.teamcity.agent.generic.TypeConverters;
import octopus.teamcity.common.pushpackage.PushPackageUserData;

public class OctopusPushPackageBuildProcess extends InterruptableBuildProcess {

  private final PushPackageUploader uploader;
  private final BuildRunnerContext context;
  private final BuildProgressLogger buildLogger;
  private final FileSelector fileSelector;

  public OctopusPushPackageBuildProcess(
      final PushPackageUploader uploader,
      final FileSelector fileSelector,
      final BuildRunnerContext context) {
    super(context);
    this.uploader = uploader;
    this.fileSelector = fileSelector;
    this.context = context;
    this.buildLogger = context.getBuild().getBuildLogger();
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Collating data for upload");
      final List<PushPackageUploaderContext> parameters = collateParameters();

      if (isInterrupted()) {
        complete(BuildFinishedStatus.INTERRUPTED);
        return;
      }

      boolean success = true;
      for (final PushPackageUploaderContext c : parameters) {
        try {
          uploader.upload(c);
        } catch (IOException e) {
          e.printStackTrace();
          success = false;
        }
      }

      if (success) {
        complete(BuildFinishedStatus.FINISHED_SUCCESS);
        return;
      }
      complete(BuildFinishedStatus.FINISHED_FAILED);
    } catch (final Throwable t) {
      throw new RunBuildException("Upload to server failed", t);
    }
  }

  private List<PushPackageUploaderContext> collateParameters() {
    final List<PushPackageUploaderContext> result = Lists.newArrayList();
    final Map<String, String> parameters = context.getRunnerParameters();
    final PushPackageUserData pushPackageUserData = new PushPackageUserData(parameters);

    final Set<File> filesToUpload = determineFilesToUpload(pushPackageUserData.getPackagePaths());
    if (filesToUpload.isEmpty()) {
      buildLogger.error(
          "Supplied package globs ("
              + pushPackageUserData.getPackagePaths()
              + ") found no matching"
              + "files");
      throw new IllegalStateException("No files found which match supplied glob");
    }

    final PushPackageUploaderContextBuilder pushPackageUploaderContextBuilder =
        new PushPackageUploaderContextBuilder()
            .withSpaceName(pushPackageUserData.getSpaceName())
            .withOverwriteMode(TypeConverters.from(pushPackageUserData.getOverwriteMode()));

    buildLogger.message("Files found to upload:");
    filesToUpload.forEach(
        f -> {
          buildLogger.message("- " + f.getName());
          pushPackageUploaderContextBuilder.withFileToUpload(f);
          result.add(pushPackageUploaderContextBuilder.build());
        });

    return result;
  }

  private Set<File> determineFilesToUpload(final String globs) {
    final List<String> packageFileGlobs = Lists.newArrayList(globs.split("\n"));
    return fileSelector.getMatchingFiles(packageFileGlobs);
  }
}
