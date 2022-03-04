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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.GenericUploadingBuildProcess;
import octopus.teamcity.agent.generic.TypeConverters;
import octopus.teamcity.common.pushpackage.PushPackageUserData;

public class OctopusPushPackageBuildProcess
    extends GenericUploadingBuildProcess<PushPackageUploaderContext> {

  private final FileSelector fileSelector;

  public OctopusPushPackageBuildProcess(
      final PushPackageUploader uploader,
      final FileSelector fileSelector,
      final BuildRunnerContext context) {
    super(context, uploader);
    this.fileSelector = fileSelector;
  }

  @Override
  protected List<PushPackageUploaderContext> collateParameters() {
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
            .withSpaceIdOrName(pushPackageUserData.getSpaceName())
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

  @Override
  protected String getIdentifier(final PushPackageUploaderContext parameter) {
    return parameter.getFile().getName();
  }

  private Set<File> determineFilesToUpload(final String globs) {
    final List<String> packageFileGlobs = Lists.newArrayList(globs.split("\n"));
    return fileSelector.getMatchingFiles(packageFileGlobs);
  }
}
