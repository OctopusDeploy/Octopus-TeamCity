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

import static jetbrains.buildServer.messages.DefaultMessagesInfo.BLOCK_TYPE_BUILD_STEP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProcess;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.LoggingProcessListener;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

public abstract class OctopusBuildProcess implements BuildProcess {
  private final AgentRunningBuild runningBuild;
  private final BuildRunnerContext context;
  private volatile Process process;
  private File extractedTo;
  private boolean isFinished;
  private final BuildProgressLogger logger;
  private OctopusCliRetryExecutor.CliResult cliResult;
  private OctopusCliRetryExecutor retryExecutor;

  protected OctopusBuildProcess(
      @NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
    this.runningBuild = runningBuild;
    this.context = context;

    logger = runningBuild.getBuildLogger();
  }

  @Override
  public void start() throws RunBuildException {
    extractOctoExe();

    retryExecutor =
        new OctopusCliRetryExecutor(logger, context.getBuildParameters().getEnvironmentVariables());

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
      final String message =
          "Unable to create temporary file in " + tempDirectory + " for Octopus: " + e.getMessage();
      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    }
  }

  private void startOcto(final OctopusCommandBuilder command) throws RunBuildException {
    String[] userVisibleCommand = command.buildMaskedCommand();
    String[] realCommand = command.buildCommand();
    Boolean useOcto = false;
    Boolean useExe = runningBuild.getAgentConfiguration().getSystemInfo().isWindows();
    if (!useExe) {
      useOcto = OctopusOsUtils.HasOcto(runningBuild.getAgentConfiguration());
    }

    logger.activityStarted("Octopus Deploy", BLOCK_TYPE_BUILD_STEP);
    if (useExe) {
      logger.message(
          "Running command:   octo.exe "
              + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
    } else if (useOcto) {
      logger.message(
          "Running command:   octo " + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
    } else {
      logger.message(
          "Running command:   dotnet octo.dll "
              + StringUtils.arrayToDelimitedString(userVisibleCommand, " "));
    }
    logger.progressMessage(getLogMessage());

    try {
      final Boolean finalUseExe = useExe;
      final Boolean finalUseOcto = useOcto;

      // Execute the CLI process, wrapped in retry logic. On transient failures (connection
      // refused, 502/503/504, DNS errors, timeouts), the retry executor will re-launch the
      // entire CLI process with exponential backoff. The CaptureWriter on stdout/stderr
      // collects output so the error classifier can inspect it after each attempt.
      cliResult =
          retryExecutor.executeWithRetry(
              () -> executeOctoProcess(realCommand, finalUseExe, finalUseOcto));

      logger.message("octo exit code: " + cliResult.getExitCode());

    } catch (IOException e) {
      final String message = "Error from octo: " + e.getMessage();
      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    } catch (InterruptedException e) {
      isFinished = true;
      final String message = "Unable to wait for octo: " + e.getMessage();
      Logger.getInstance(getClass().getName()).error(message, e);
      throw new RunBuildException(message);
    }
  }

  private OctopusCliRetryExecutor.CliResult executeOctoProcess(
      String[] realCommand, boolean useExe, boolean useOcto)
      throws IOException, InterruptedException {
    final String octopusVersion = getSelectedOctopusVersion();

    final ArrayList<String> arguments = new ArrayList<>();
    if (useExe) {
      arguments.add(new File(extractedTo, octopusVersion + "/octo.exe").getAbsolutePath());
    } else if (useOcto) {
      arguments.add("octo");
    } else {
      arguments.add("dotnet");
      String dllPath = new File(extractedTo, octopusVersion + "/Core/octo.dll").getAbsolutePath();
      arguments.add(dllPath);
    }

    arguments.addAll(Arrays.asList(realCommand));

    final String extensionVersion = getClass().getPackage().getImplementationVersion();

    final ProcessBuilder builder = new ProcessBuilder();

    Map<String, String> programEnvironmentVariables =
        context.getBuildParameters().getEnvironmentVariables();
    Map<String, String> environment = builder.environment();
    environment.put("OCTOEXTENSION", extensionVersion);
    environment.putAll(programEnvironmentVariables);

    process = builder.command(arguments).directory(context.getWorkingDirectory()).start();

    final LoggingProcessListener listener = new LoggingProcessListener(logger);

    CaptureWriter stderrCapture = new CaptureWriter(listener::onErrorOutput);
    CaptureWriter stdoutCapture = new CaptureWriter(listener::onStandardOutput);

    Output.ReaderThread standardError =
        new Output.ReaderThread(process.getErrorStream(), stderrCapture);
    Output.ReaderThread standardOutput =
        new Output.ReaderThread(process.getInputStream(), stdoutCapture);

    standardError.start();
    standardOutput.start();

    int exitCode = process.waitFor();

    standardError.join();
    standardOutput.join();

    String combinedOutput = stdoutCapture.getCapturedOutput() + stderrCapture.getCapturedOutput();
    return new OctopusCliRetryExecutor.CliResult(exitCode, combinedOutput);
  }

  private String getSelectedOctopusVersion() {
    final Map<String, String> parameters = getContext().getRunnerParameters();
    final OctopusConstants constants = OctopusConstants.Instance;

    final String key = constants.getOctopusVersion();
    if (!parameters.containsKey(key)) {
      return constants.getVersion3().replace("+", "");
    }

    final String octopusVersion = parameters.get(constants.getOctopusVersion());
    if (octopusVersion == null
        || octopusVersion.length() == 0
        || octopusVersion.equals(constants.getPreviewVersion())) {
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
    logger.activityFinished("Octopus Deploy", BLOCK_TYPE_BUILD_STEP);
    isFinished = true;

    if (cliResult != null && cliResult.getExitCode() == 0) {
      return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    runningBuild.getBuildLogger().progressFinished();

    String message =
        "Unable to create or deploy release. Please check the build log for details on the error.";

    if (runningBuild.getFailBuildOnExitCode()) {
      runningBuild.getBuildLogger().buildFailureDescription(message);
      return BuildFinishedStatus.FINISHED_FAILED;
    } else {
      runningBuild.getBuildLogger().error(message);
      return BuildFinishedStatus.FINISHED_SUCCESS;
    }
  }
}
