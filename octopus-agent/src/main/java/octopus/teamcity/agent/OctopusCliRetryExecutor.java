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

import java.io.IOException;
import java.util.Map;
import java.util.Random;

import jetbrains.buildServer.agent.BuildProgressLogger;

public class OctopusCliRetryExecutor {

  @FunctionalInterface
  public interface CliExecution {
    CliResult execute() throws IOException, InterruptedException;
  }

  public static class CliResult {
    private final int exitCode;
    private final String combinedOutput;

    public CliResult(int exitCode, String combinedOutput) {
      this.exitCode = exitCode;
      this.combinedOutput = combinedOutput;
    }

    public int getExitCode() {
      return exitCode;
    }

    public String getCombinedOutput() {
      return combinedOutput;
    }
  }

  @FunctionalInterface
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  @FunctionalInterface
  interface Clock {
    long currentTimeMillis();
  }

  static final long DEFAULT_TOTAL_TIMEOUT_MS = 900_000L;
  static final long DEFAULT_INITIAL_DELAY_MS = 5_000L;
  static final long DEFAULT_MAX_DELAY_MS = 120_000L;
  static final String ENV_RETRY_ENABLED = "OCTOPUS_RETRY_ENABLED";
  static final String ENV_RETRY_TIMEOUT = "OCTOPUS_RETRY_TIMEOUT";
  static final String ENV_RETRY_INITIAL_DELAY = "OCTOPUS_RETRY_INITIAL_DELAY";
  static final String ENV_RETRY_MAX_DELAY = "OCTOPUS_RETRY_MAX_DELAY";

  private final boolean enabled;
  private final long totalTimeoutMs;
  private final long initialDelayMs;
  private final long maxDelayMs;
  private final BuildProgressLogger logger;
  private final Sleeper sleeper;
  private final Clock clock;
  private final Random random;

  public OctopusCliRetryExecutor(BuildProgressLogger logger, Map<String, String> environment) {
    this(logger, environment, Thread::sleep, System::currentTimeMillis, new Random());
  }

  OctopusCliRetryExecutor(
      BuildProgressLogger logger,
      Map<String, String> environment,
      Sleeper sleeper,
      Clock clock,
      Random random) {
    this.logger = logger;
    this.sleeper = sleeper;
    this.clock = clock;
    this.random = random;
    this.enabled = parseBooleanEnv(environment, ENV_RETRY_ENABLED, true);
    this.totalTimeoutMs = parseLongEnv(environment, ENV_RETRY_TIMEOUT, DEFAULT_TOTAL_TIMEOUT_MS);
    this.initialDelayMs =
        parseLongEnv(environment, ENV_RETRY_INITIAL_DELAY, DEFAULT_INITIAL_DELAY_MS);
    this.maxDelayMs = parseLongEnv(environment, ENV_RETRY_MAX_DELAY, DEFAULT_MAX_DELAY_MS);
  }

  public CliResult executeWithRetry(CliExecution execution)
      throws IOException, InterruptedException {
    CliResult result = execution.execute();

    if (!enabled || result.getExitCode() == 0) {
      return result;
    }

    OctopusErrorClassifier.ErrorCategory category =
        OctopusErrorClassifier.classify(result.getExitCode(), result.getCombinedOutput());
    if (category != OctopusErrorClassifier.ErrorCategory.TRANSIENT) {
      return result;
    }

    long startTime = clock.currentTimeMillis();
    int attempt = 1;

    while (true) {
      long elapsed = clock.currentTimeMillis() - startTime;
      long remaining = totalTimeoutMs - elapsed;

      if (remaining <= 0) {
        long totalMinutes = (clock.currentTimeMillis() - startTime) / 60_000;
        logger.warning(
            String.format(
                "Octopus Deploy unavailable after %d attempts over %dm. Failing build.",
                attempt, totalMinutes));
        return result;
      }

      long delay = calculateDelay(attempt);
      long remainingMinutes = remaining / 60_000;
      long delaySeconds = delay / 1_000;

      logger.warning(
          String.format(
              "Octopus Deploy appears unavailable (attempt %d). Retrying in %ds... (%dm remaining)",
              attempt, delaySeconds, remainingMinutes));

      sleeper.sleep(delay);
      attempt++;

      result = execution.execute();

      if (result.getExitCode() == 0) {
        logger.message(String.format("Octopus Deploy retry successful on attempt %d.", attempt));
        return result;
      }

      category = OctopusErrorClassifier.classify(result.getExitCode(), result.getCombinedOutput());
      if (category != OctopusErrorClassifier.ErrorCategory.TRANSIENT) {
        return result;
      }
    }
  }

  long calculateDelay(int attempt) {
    long delay = initialDelayMs * (1L << Math.min(attempt - 1, 30));
    long jitter = random.nextInt(1_000);
    return Math.min(delay + jitter, maxDelayMs);
  }

  private static boolean parseBooleanEnv(
      Map<String, String> env, String key, boolean defaultValue) {
    String value = env.get(key);
    if (value == null || value.isEmpty()) {
      return defaultValue;
    }
    return Boolean.parseBoolean(value);
  }

  private static long parseLongEnv(Map<String, String> env, String key, long defaultValue) {
    String value = env.get(key);
    if (value == null || value.isEmpty()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
