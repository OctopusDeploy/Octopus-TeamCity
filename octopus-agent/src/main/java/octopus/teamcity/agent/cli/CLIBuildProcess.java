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
import java.util.regex.Pattern;

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
import octopus.teamcity.agent.Output;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.SpaceSelection;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

public abstract class CLIBuildProcess implements BuildProcess {
  private static final Pattern UNKNOWN_FLAG =
      Pattern.compile("unknown (?:shorthand )?flag", Pattern.CASE_INSENSITIVE);

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

  protected abstract List<OctopusCommandBuilder> createCommand() throws RunBuildException;

  /** Whether the step's commands act on an Octopus space, which then must be set. */
  protected boolean usesSpace() {
    return true;
  }

  protected BuildRunnerContext getContext() {
    return context;
  }

  @Override
  public void start() throws RunBuildException {
    if (usesSpace() && !SpaceSelection.isSet(context.getRunnerParameters())) {
      throw new RunBuildException(
          "No Octopus space is set. Choose a space on this step, or on the Octopus connection it"
              + " uses.");
    }
    extractOctopusBinary();
    retryExecutor =
        new OctopusCliRetryExecutor(logger, context.getBuildParameters().getEnvironmentVariables());
    // Execute each CLI command in sequence (e.g. login → create-release → deploy → wait).
    // If any command exits non-zero, skip remaining commands. Each individual command
    // is independently wrapped in retry logic by runCommand(), so a transient failure on
    // one command won't skip subsequent commands until retries are exhausted.
    List<OctopusCommandBuilder> commandArgumentsList = createCommand();
    for (OctopusCommandBuilder commandArguments : commandArgumentsList) {
      exitCode = runCommand(commandArguments);
      if (exitCode != 0) {
        return;
      }
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
    final String osFolder = OctopusOsUtils.getBinaryOsFolder(runningBuild);
    try {
      octopusBinaryName = "octopus";
      if (osFolder.startsWith("windows-os")) {
        octopusBinaryName = "octopus.exe";
      }
      extractedTo = new File(tempDirectory, String.format("octopus-temp/%s", osFolder));
      boolean success = binaryExtracted(osFolder);
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

  private boolean binaryExtracted(String osFolder) throws Exception {
    if (!extractedTo.exists()) {
      if (!extractedTo.mkdirs())
        throw new RuntimeException("Unable to create temp output directory " + extractedTo);
    }
    EmbeddedResourceExtractor extractor = new EmbeddedResourceExtractor();
    String destinationName = extractedTo.getAbsolutePath();
    extractor.extractCliTo(destinationName, osFolder);
    File file = new File(String.format("%s/%s", destinationName, octopusBinaryName));
    return file.setExecutable(true, false);
  }

  int runCommand(final OctopusCommandBuilder command) throws RunBuildException {
    String[] userVisibleCommand = command.buildMaskedCommand();
    String[] realCommand = command.buildCommand();
    logger.message(
        "Running command:   octopus "
            + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
    logger.progressMessage(getLogMessage());

    try {
      // The Go CLI retries its own login; this retry is a second safety net. On a transient
      // failure the whole process is re-run with exponential backoff. stdout and stderr are
      // merged and captured in outputBuilder, which classifies the error after each attempt.
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

      if (command.isOptional() && result.getExitCode() != 0) {
        logger.warning(
            "Command exited with code "
                + result.getExitCode()
                + ". It only gathers extra detail, so the step carries on without it.");
        return 0;
      }

      logger.message("Exit code: " + result.getExitCode());
      if (result.getExitCode() == 0) {
        command.readResponse(result.getCombinedOutput());
      } else {
        final String hint =
            unknownArgumentHint(
                context
                    .getRunnerParameters()
                    .get(OctopusConstants.Instance.getCommandLineArgumentsKey()),
                result.getCombinedOutput());
        if (hint != null) {
          logger.error(hint);
        }
      }
      return result.getExitCode();

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

  /**
   * Explains an "unknown flag" failure from a step with additional command line arguments. Those
   * arguments reach the Octopus CLI unchanged, and the legacy {@code octo} CLI's arguments are not
   * valid there.
   */
  static String unknownArgumentHint(final String additionalArguments, final String output) {
    if (additionalArguments == null || additionalArguments.trim().isEmpty()) {
      return null;
    }
    if (output == null || !UNKNOWN_FLAG.matcher(output).find()) {
      return null;
    }
    return "The Octopus CLI rejected an argument. This step's additional command line arguments"
        + " are passed to the Octopus CLI unchanged, and arguments for the legacy octo CLI, such as"
        + " --progress or --deployTo, are not valid there. Run 'octopus <command> --help' for the"
        + " arguments each command accepts.";
  }
}
