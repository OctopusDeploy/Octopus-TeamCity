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

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class OctopusPackPackageBuildProcess extends OctopusBuildProcess {

    protected final ExtensionHolder myExtensionHolder;
    protected final AgentRunningBuild myRunningBuild;

    public OctopusPackPackageBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context, @NotNull final ExtensionHolder extensionHolder) {
       super(runningBuild, context);

        myExtensionHolder = extensionHolder;
        myRunningBuild = runningBuild;
    }

    @Override
    protected String getLogMessage() {
        return "Creating package";
    }

    @NotNull
    @Override
    public BuildFinishedStatus waitFor() throws RunBuildException {
        BuildFinishedStatus status = super.waitFor();

        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;
        final String packageId = parameters.get(constants.getPackageIdKey());
        final String packageFormat = parameters.get(constants.getPackageFormatKey()).toLowerCase();
        final String packageVersion = parameters.get(constants.getPackageVersionKey());
        final String outputPath = parameters.get(constants.getPackageOutputPathKey());
        final boolean publishArtifacts = Boolean.parseBoolean(parameters.get(constants.getPublishArtifactsKey()));

        if (!publishArtifacts)
            return status;

        String packagePath = outputPath;
        if (!packagePath.endsWith(File.separator))
            packagePath += File.separator;
        packagePath += packageId + "." + packageVersion + "." + packageFormat;

        BuildProgressLogger logger = myRunningBuild.getBuildLogger();

        String message = ServiceMessage.asString("publishArtifacts", myRunningBuild.getCheckoutDirectory() + File.separator + packagePath);
        logger.message(message);

        return status;
    }

    @Override
    protected OctopusCommandBuilder createCommand() {
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;

        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String packageId = parameters.get(constants.getPackageIdKey());
                final String packageFormat = parameters.get(constants.getPackageFormatKey()).toLowerCase();
                final String packageVersion = parameters.get(constants.getPackageVersionKey());
                final String sourcePath = parameters.get(constants.getPackageSourcePathKey());
                final String outputPath = parameters.get(constants.getPackageOutputPathKey());
                final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());

                commands.add("pack");

                commands.add("--id");
                commands.add(packageId);

                commands.add("--format");
                commands.add(packageFormat);

                commands.add("--version");
                commands.add(packageVersion);

                commands.add("--basePath");
                commands.add(sourcePath);

                commands.add("--outFolder");
                commands.add(outputPath);

                if (commandLineArguments != null && !commandLineArguments.isEmpty()) {
                    commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
                }

                return commands.toArray(new String[commands.size()]);
            }
        };
    }
}
