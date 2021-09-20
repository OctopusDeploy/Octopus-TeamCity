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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.octopus.sdk.operations.pushpackage.PushPackageUploader;
import com.octopus.sdk.operations.pushpackage.PushPackageUploaderContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OverwriteMode;
import octopus.teamcity.common.pushpackage.PushPackagePropertyNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class OctopusPushPackageBuildProcessTest {

  private final PushPackageUploader uploader = mock(PushPackageUploader.class);
  private final BuildRunnerContext context = mock(BuildRunnerContext.class);
  private final AgentRunningBuild mockBuild = mock(AgentRunningBuild.class);
  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);
  private final FileSelector fileSelector = mock(FileSelector.class);

  final Map<String, String> userEnteredData = new HashMap<>();

  @BeforeEach
  public void setup() {
    when(mockBuild.getBuildLogger()).thenReturn(logger);
    when(context.getBuild()).thenReturn(mockBuild);
    when(context.getRunnerParameters()).thenReturn(userEnteredData);
  }

  @Test
  public void pushBuildInfoToOctopusServer(@TempDir Path testDir)
      throws RunBuildException, IOException {
    final File toUpload = Files.createFile(testDir.resolve("file.zip")).toFile();
    final Set<File> matchedFiles = Collections.singleton(toUpload);
    when(fileSelector.getMatchingFiles(any())).thenReturn(matchedFiles);

    userEnteredData.put(
        PushPackagePropertyNames.OVERWRITE_MODE, OverwriteMode.OverwriteExisting.name());
    userEnteredData.put(PushPackagePropertyNames.PACKAGE_PATHS, "*.zip");
    userEnteredData.put(PushPackagePropertyNames.USE_DELTA_COMPRESSION, "false");

    when(fileSelector.getMatchingFiles(any())).thenReturn(matchedFiles);

    final OctopusPushPackageBuildProcess buildProcess =
        new OctopusPushPackageBuildProcess(uploader, fileSelector, context);

    buildProcess.start();

    assertThat(buildProcess.isFinished()).isTrue();
    assertThat(buildProcess.waitFor()).isEqualTo(BuildFinishedStatus.FINISHED_SUCCESS);
    verify(fileSelector).getMatchingFiles(singletonList("*.zip"));

    final ArgumentCaptor<PushPackageUploaderContext> uploadPayload =
        ArgumentCaptor.forClass(PushPackageUploaderContext.class);
    verify(uploader).upload(uploadPayload.capture());

    assertThat(uploadPayload.getValue().getFile()).isEqualTo(toUpload);
    assertThat(uploadPayload.getValue().getOverwriteMode())
        .isEqualTo(com.octopus.sdk.api.OverwriteMode.OverwriteExisting);
  }

  @Test
  public void uploadIsInvokedForEachFileReturnedBySelector(@TempDir Path testDir)
      throws IOException, RunBuildException {
    final int fileCount = 3;
    final Set<File> matchedFiles = new HashSet<>();
    for (int i = 0; i < fileCount; i++) {
      matchedFiles.add(Files.createFile(testDir.resolve(i + "file.zip")).toFile());
    }

    when(fileSelector.getMatchingFiles(any())).thenReturn(matchedFiles);
    userEnteredData.put(
        PushPackagePropertyNames.OVERWRITE_MODE, OverwriteMode.OverwriteExisting.name());
    userEnteredData.put(PushPackagePropertyNames.PACKAGE_PATHS, "*.zip");
    userEnteredData.put(PushPackagePropertyNames.USE_DELTA_COMPRESSION, "false");

    final OctopusPushPackageBuildProcess buildProcess =
        new OctopusPushPackageBuildProcess(uploader, fileSelector, context);

    buildProcess.start();

    final ArgumentCaptor<PushPackageUploaderContext> uploadPayload =
        ArgumentCaptor.forClass(PushPackageUploaderContext.class);
    verify(uploader, times(fileCount)).upload(uploadPayload.capture());

    final List<File> filesUploaded =
        uploadPayload.getAllValues().stream()
            .map(PushPackageUploaderContext::getFile)
            .collect(Collectors.toList());

    assertThat(filesUploaded).containsExactlyInAnyOrderElementsOf(matchedFiles);
  }
}
