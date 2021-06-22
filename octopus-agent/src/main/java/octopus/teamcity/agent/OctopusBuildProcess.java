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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.LoggingProcessListener;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public abstract class OctopusBuildProcess implements BuildProcess {
    private final AgentRunningBuild runningBuild;
    private final BuildRunnerContext context;
    private Process process;
    private File extractedTo;
    private OutputReaderThread standardError;
    private OutputReaderThread standardOutput;
    private boolean isFinished;
    private final BuildProgressLogger logger;

    protected OctopusBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
        this.runningBuild = runningBuild;
        this.context = context;

        logger = runningBuild.getBuildLogger();
    }

    @Override
    public void start() throws RunBuildException {
        extractOctoExe();

        OctopusCommandBuilder arguments = createCommand();
        startOcto(arguments);
    }

    protected abstract OctopusCommandBuilder createCommand();
    protected abstract String getLogMessage();

    protected BuildRunnerContext getContext() {
        return context;
    }

    protected BuildProgressLogger getLogger() {
        return logger;
    }

    private void extractOctoExe() throws RunBuildException {
        final File tempDirectory = runningBuild.getBuildTempDirectory();
        try {
            extractedTo = new File(tempDirectory, "octo-temp");
            if (!extractedTo.exists()) {
                if (!extractedTo.mkdirs())
                    throw new RuntimeException("Unable to create temp output directory " + extractedTo);
            }

            EmbeddedResourceExtractor extractor = new EmbeddedResourceExtractor();
            extractor.extractTo(extractedTo.getAbsolutePath());
        } catch (Exception e) {
            final String message = "Unable to create temporary file in " + tempDirectory + " for Octopus: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
    }

    private void startOcto(final OctopusCommandBuilder command) throws RunBuildException {
        String[] userVisibleCommand = command.buildMaskedCommand();
        String[] realCommand = command.buildCommand();
        Boolean useOcto = false;
        Boolean useExe = runningBuild.getAgentConfiguration().getSystemInfo().isWindows();
        if(!useExe){
            useOcto = OctopusOsUtils.HasOcto(runningBuild.getAgentConfiguration());
        }

        logger.activityStarted("Octopus Deploy", DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);
        if(useExe) {
            logger.message("Running command:   octo.exe " + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
        } else if(useOcto) {
            logger.message("Running command:   octo " + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
        } else {
            logger.message("Running command:   dotnet octo.dll " + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
        }
        logger.progressMessage(getLogMessage());

        try {
            final String octopusVersion = getSelectedOctopusVersion();

            final ArrayList<String> arguments = new ArrayList<>();
            if(useExe) {
                arguments.add(new File(extractedTo, octopusVersion + "/octo.exe").getAbsolutePath());
            } else if(useOcto) {
                arguments.add("octo");
            } else {
                arguments.add("dotnet");
                String dllPath = new File(extractedTo, octopusVersion + "/Core/octo.dll").getAbsolutePath();
                arguments.add(dllPath);
            }

            arguments.addAll(Arrays.asList(realCommand));

            final String extensionVersion = getClass().getPackage().getImplementationVersion();

            final ProcessBuilder builder = new ProcessBuilder();

            Map<String, String> programEnvironmentVariables = context.getBuildParameters().getEnvironmentVariables();
            Map<String, String> environment = builder.environment();
            environment.put("OCTOEXTENSION", extensionVersion);
            environment.putAll(programEnvironmentVariables);

            process = builder
                    .command(arguments)
                    .directory(context.getWorkingDirectory())
                    .start();

            final LoggingProcessListener listener = new LoggingProcessListener(logger);

            standardError = new OutputReaderThread(process.getErrorStream(),listener::onErrorOutput);
            standardOutput = new OutputReaderThread(process.getInputStream(), listener::onStandardOutput);

            standardError.start();
            standardOutput.start();
        } catch (IOException e) {
            final String message = "Error from octo: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }
    }

    private String getSelectedOctopusVersion() {
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;

        final String key = constants.getOctopusVersion();
        if (!parameters.containsKey(key)) {
            return constants.getVersion3().replace("+", "");
        }

        final String octopusVersion = parameters.get(constants.getOctopusVersion());
        if (octopusVersion == null ||
                octopusVersion.length() == 0 ||
                octopusVersion.equals(constants.getPreviewVersion())) {
            return constants.getVersion3().replace("+", "");
        }

        return octopusVersion.replace("+", "");
    }

    @Override
    public boolean isInterrupted() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public void interrupt() {
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    @NotNull
    public BuildFinishedStatus waitFor() throws RunBuildException {
        int exitCode;

        try {
            exitCode = process.waitFor();

            standardError.join();
            standardOutput.join();

            logger.message("octo exit code: " + exitCode);
            logger.activityFinished("Octopus Deploy", DefaultMessagesInfo.BLOCK_TYPE_INDENTATION);

            isFinished = true;
        }
        catch (InterruptedException e) {
            isFinished = true;
            final String message = "Unable to wait for octo: " + e.getMessage();
            Logger.getInstance(getClass().getName()).error(message, e);
            throw new RunBuildException(message);
        }

        if (exitCode == 0)
            return BuildFinishedStatus.FINISHED_SUCCESS;

        runningBuild.getBuildLogger().progressFinished();

        String message = "Unable to create or deploy release. Please check the build log for details on the error.";

        if (runningBuild.getFailBuildOnExitCode()) {
            runningBuild.getBuildLogger().buildFailureDescription(message);
            return BuildFinishedStatus.FINISHED_FAILED;
        } else {
            runningBuild.getBuildLogger().error(message);
            return BuildFinishedStatus.FINISHED_SUCCESS;
        }
    }

    private interface OutputWriter {
        void write(String text);
    }

    private static class OutputReaderThread extends Thread {
        private final InputStream is;
        private final OutputWriter output;

        OutputReaderThread(InputStream is, OutputWriter output) {
            this.is = is;
            this.output = output;
        }

        @Override
        public void run() {
            final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            final BufferedReader br = new BufferedReader(isr);
            String line;

            try {
                while ((line = br.readLine()) != null) {
                    output.write(line.replaceAll("[\\r\\n]", ""));
                }
            } catch (IOException e) {
                output.write("ERROR: " + e.getMessage());
            }
        }
    }
}
