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

package octopus.teamcity.agent.cli;

import static jetbrains.buildServer.messages.DefaultMessagesInfo.BLOCK_TYPE_BUILD_STEP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.LoggingProcessListener;
import octopus.teamcity.agent.EmbeddedResourceExtractor;
import octopus.teamcity.agent.OctopusCliRetryExecutor;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.agent.OctopusOsUtils;
import octopus.teamcity.agent.Output;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

public abstract class CLIBuildProcess implements BuildProcess {
  private final AgentRunningBuild runningBuild;
  private final BuildRunnerContext context;
  private volatile Process process;
  private int exitCode;
  private File extractedTo;
  private Output.ReaderThread standardOutput;
  private boolean isFinished;
  private String octopusBinaryName;
  protected final BuildProgressLogger logger;
  private OctopusCliRetryExecutor retryExecutor;

  protected CLIBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    this.runningBuild = runningBuild;
    this.context = context;

    logger = runningBuild.getBuildLogger();
  }

  public abstract void processOutput(String output, int exitCode);

  protected abstract List<OctopusCommandBuilder> createCommand();

  protected BuildRunnerContext getContext() {
    return context;
  }

  @Override
  public void start() throws RunBuildException {
    extractOctopusBinary();
    retryExecutor =
        new OctopusCliRetryExecutor(logger, context.getBuildParameters().getEnvironmentVariables());
    // Execute each CLI command in sequence (e.g. login → create-release → deploy → wait).
    // If any command fails (exitCode == 1), skip remaining commands. Each individual command
    // is independently wrapped in retry logic by startOctopus(), so a transient failure on
    // one command won't skip subsequent commands until retries are exhausted.
    List<OctopusCommandBuilder> commandArgumentsList = createCommand();
    for (OctopusCommandBuilder commandArguments : commandArgumentsList) {
      if (exitCode == 1) {
        return;
      }
      startOctopus(commandArguments);
    }
  }

  @Override
  public boolean isInterrupted() {
    return false;
  }

  protected abstract String getLogMessage();

  @Override
  public void interrupt() {
    if (process != null) {
      process.destroy();
    }
  }

  @Override
  public boolean isFinished() {
    return isFinished;
  }

  @Override
  @NotNull
  public BuildFinishedStatus waitFor() throws RunBuildException {
    logger.activityFinished("Octopus Deploy", BLOCK_TYPE_BUILD_STEP);
    isFinished = true;
    if (exitCode == 0) {
      return BuildFinishedStatus.FINISHED_SUCCESS;
    }
    runningBuild.getBuildLogger().progressFinished();
    String message =
        "Unable to create or deploy release. Please check the build log for details on the error.";

    if (runningBuild.getFailBuildOnExitCode()) {
      runningBuild.getBuildLogger().buildFailureDescription(message);
      return BuildFinishedStatus.FINISHED_FAILED;
    }
    runningBuild.getBuildLogger().error(message);
    return BuildFinishedStatus.FINISHED_SUCCESS;
  }

  private void extractOctopusBinary() throws RunBuildException {
    final File tempDirectory = runningBuild.getBuildTempDirectory();
    try {
      final Map<String, String> parameters = getContext().getRunnerParameters();
      final OctopusConstants constants = OctopusConstants.Instance;
      final String octopusVersion = getCliVersion(parameters, constants).replace("+", "");
      final String osFolder = OctopusOsUtils.getBinaryOsFolder(runningBuild);
      octopusBinaryName = "octopus";
      if (osFolder.equals("windows-os")) {
        octopusBinaryName = "octopus.exe";
      }
      extractedTo =
          new File(tempDirectory, String.format("octopus-temp/%s/%s", octopusVersion, osFolder));
      boolean success = binaryExtracted(octopusVersion, osFolder);
      if (!success) {
        throw new RuntimeException("Failed to set executable permission on octopus binary");
      }
    } catch (Exception e) {
      final String message =
          "Unable to create temporary file in " + tempDirectory + " for Octopus: " + e.getMessage();
      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    }
  }

  private boolean binaryExtracted(String octopusVersion, String osFolder) throws Exception {
    if (!extractedTo.exists()) {
      if (!extractedTo.mkdirs())
        throw new RuntimeException("Unable to create temp output directory " + extractedTo);
    }
    EmbeddedResourceExtractor extractor = new EmbeddedResourceExtractor();
    String destinationName = extractedTo.getAbsolutePath();
    extractor.extractCliTo(destinationName, octopusVersion, osFolder);
    File file = new File(String.format("%s/%s", destinationName, octopusBinaryName));
    return file.setExecutable(true, false);
  }

  private void startOctopus(final OctopusCommandBuilder command) throws RunBuildException {
    String[] userVisibleCommand = command.buildMaskedCommand();
    String[] realCommand = command.buildCommand();
    logger.message(
        "Running command:   octopus "
            + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
    logger.progressMessage(getLogMessage());

    try {
      // Execute this CLI command wrapped in retry logic. Unlike the legacy path, the Go CLI
      // has its own built-in retry on login, so this is a second safety net. On transient
      // failures, the entire process (binary launch → login → command) is re-executed with
      // exponential backoff. stdout and stderr are merged (redirectErrorStream) and captured
      // in outputBuilder for error classification after each attempt.
      OctopusCliRetryExecutor.CliResult result =
          retryExecutor.executeWithRetry(
              () -> {
                final ArrayList<String> arguments = new ArrayList<>();
                arguments.add(
                    new File(extractedTo, String.format("/%s", octopusBinaryName))
                        .getAbsolutePath());

                arguments.addAll(Arrays.asList(realCommand));
                final String extensionVersion = getClass().getPackage().getImplementationVersion();
                final ProcessBuilder builder = new ProcessBuilder();
                builder.redirectErrorStream(true);
                Map<String, String> programEnvironmentVariables =
                    context.getBuildParameters().getEnvironmentVariables();
                Map<String, String> environment = builder.environment();

                // Only set OCTOEXTENSION if the version is available (null when running from IDE)
                if (extensionVersion != null) {
                  environment.put("OCTOEXTENSION", extensionVersion);
                }

                // Filter out any null values from environment variables
                for (Map.Entry<String, String> entry : programEnvironmentVariables.entrySet()) {
                  if (entry.getKey() != null && entry.getValue() != null) {
                    environment.put(entry.getKey(), entry.getValue());
                  }
                }

                logger.message("Starting process...");
                process =
                    builder.command(arguments).directory(context.getWorkingDirectory()).start();
                process.getOutputStream().close();

                StringBuilder outputBuilder = new StringBuilder();
                final LoggingProcessListener listener = new LoggingProcessListener(logger);

                standardOutput =
                    new Output.ReaderThread(
                        process.getInputStream(),
                        line -> {
                          listener.onStandardOutput(line);
                          outputBuilder.append(line);
                        });
                standardOutput.start();
                int processExitCode = process.waitFor();
                standardOutput.join();

                return new OctopusCliRetryExecutor.CliResult(
                    processExitCode, outputBuilder.toString());
              });

      exitCode = result.getExitCode();
      processOutput(result.getCombinedOutput(), exitCode);

    } catch (IOException e) {
      final String message = "Error from octopus: " + e.getMessage();
      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    } catch (InterruptedException e) {
      isFinished = true;
      final String message = "Unable to wait for octopus: " + e.getMessage();
      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    }
  }

  private String getCliVersion(Map<String, String> parameters, OctopusConstants constants) {
    String cliVersion = parameters.get(constants.getOctopusVersion());
    if (cliVersion == null
        || constants.getVersion3().equals(cliVersion)
        || constants.getVersion1().equals(cliVersion)) {
      cliVersion = constants.getVersion2();
    }
    return cliVersion;
  }
}
