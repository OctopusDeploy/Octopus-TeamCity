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

package octopus.teamcity.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.ArtifactsPreprocessor;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsBuilder;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.OverwriteMode;
import org.jetbrains.annotations.NotNull;

public class OctopusPushPackageBuildProcess extends OctopusBuildProcess {

  protected final ExtensionHolder myExtensionHolder;
  protected final AgentRunningBuild myRunningBuild;
  protected List<ArtifactsCollection> artifactsCollections;

  public OctopusPushPackageBuildProcess(
      @NotNull AgentRunningBuild runningBuild,
      @NotNull BuildRunnerContext context,
      @NotNull final ExtensionHolder extensionHolder) {
    super(runningBuild, context);

    myExtensionHolder = extensionHolder;
    myRunningBuild = runningBuild;
  }

  @Override
  protected String getLogMessage() {
    return "Pushing packages to Octopus server";
  }

  @Override
  public void start() throws RunBuildException {
    final Collection<ArtifactsPreprocessor> preprocessors =
        myExtensionHolder.getExtensions(ArtifactsPreprocessor.class);

    final Map<String, String> parameters = getContext().getRunnerParameters();
    final OctopusConstants constants = OctopusConstants.Instance;
    final String packagePaths = parameters.get(constants.getPackagePathsKey());
    final boolean publishArtifacts =
        Boolean.parseBoolean(parameters.get(constants.getPublishArtifactsKey()));

    final ArtifactsBuilder builder = new ArtifactsBuilder();
    builder.setPreprocessors(preprocessors);
    builder.setBaseDir(myRunningBuild.getCheckoutDirectory());
    builder.setArtifactsPaths(packagePaths);

    artifactsCollections = builder.build();

    super.start();

    if (!publishArtifacts) return;

    BuildProgressLogger logger = myRunningBuild.getBuildLogger();
    for (ArtifactsCollection artifactsCollection : artifactsCollections) {
      for (Map.Entry<File, String> fileStringEntry :
          artifactsCollection.getFilePathMap().entrySet()) {
        final File source = fileStringEntry.getKey();

        String message = ServiceMessage.asString("publishArtifacts", source.getAbsolutePath());

        logger.message(message);
      }
    }
  }

  @Override
  protected OctopusCommandBuilder createCommand() {
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final OctopusConstants constants = OctopusConstants.Instance;

    return new OctopusCommandBuilder() {
      @Override
      protected String[] buildCommand(boolean masked) {
        final ArrayList<String> commands = new ArrayList<String>();
        final String serverUrl = parameters.get(constants.getServerKey());
        final String apiKey = parameters.get(constants.getApiKey());
        final String spaceName = parameters.get(constants.getSpaceName());
        final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());

        final String forcePush = parameters.get(constants.getForcePushKey());
        OverwriteMode overwriteMode = OverwriteMode.FailIfExists;
        if ("true".equals(forcePush)) {
          overwriteMode = OverwriteMode.OverwriteExisting;
        } else if (OverwriteMode.IgnoreIfExists.name().equals(forcePush)) {
          overwriteMode = OverwriteMode.IgnoreIfExists;
        }

        commands.add("push");
        commands.add("--server");
        commands.add(serverUrl);
        commands.add("--apikey");
        commands.add(masked ? "SECRET" : apiKey);

        if (spaceName != null && !spaceName.isEmpty()) {
          commands.add("--space");
          commands.add(spaceName);
        }

        for (ArtifactsCollection artifactsCollection : artifactsCollections) {
          for (Map.Entry<File, String> fileStringEntry :
              artifactsCollection.getFilePathMap().entrySet()) {
            final File source = fileStringEntry.getKey();

            commands.add("--package");
            commands.add(source.getAbsolutePath());
          }
        }

        if (overwriteMode != OverwriteMode.FailIfExists) {
          commands.add("--overwrite-mode");
          commands.add(overwriteMode.name());
        }

        if (commandLineArguments != null && !commandLineArguments.isEmpty()) {
          commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
        }

        return commands.toArray(new String[commands.size()]);
      }
    };
  }
}
