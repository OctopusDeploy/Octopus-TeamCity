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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jetbrains.buildServer.agent.BuildProgressLogger;
import octopus.teamcity.agent.OctopusCliRetryExecutor.CliResult;
import org.junit.jupiter.api.Test;

class OctopusCliRetryExecutorTest {

  private static final CliResult SUCCESS = new CliResult(0, "OK");
  private static final CliResult TRANSIENT_FAILURE = new CliResult(1, "connection refused");
  private static final CliResult PERMANENT_FAILURE =
      new CliResult(1, "Could not find project 'Missing'");

  private OctopusCliRetryExecutor createExecutor(
      Map<String, String> env,
      OctopusCliRetryExecutor.Sleeper sleeper,
      OctopusCliRetryExecutor.Clock clock) {
    return new OctopusCliRetryExecutor(
        mock(BuildProgressLogger.class), env, sleeper, clock, new Random(42));
  }

  private OctopusCliRetryExecutor createExecutor(Map<String, String> env) {
    AtomicLong time = new AtomicLong(0);
    return new OctopusCliRetryExecutor(
        mock(BuildProgressLogger.class),
        env,
        millis -> time.addAndGet(millis),
        time::get,
        new Random(42));
  }

  @Test
  void successOnFirstAttemptDoesNotRetry() throws Exception {
    OctopusCliRetryExecutor executor = createExecutor(new HashMap<>());
    AtomicInteger callCount = new AtomicInteger(0);

    CliResult result =
        executor.executeWithRetry(
            () -> {
              callCount.incrementAndGet();
              return SUCCESS;
            });

    assertThat(result.getExitCode()).isEqualTo(0);
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  void permanentFailureDoesNotRetry() throws Exception {
    OctopusCliRetryExecutor executor = createExecutor(new HashMap<>());
    AtomicInteger callCount = new AtomicInteger(0);

    CliResult result =
        executor.executeWithRetry(
            () -> {
              callCount.incrementAndGet();
              return PERMANENT_FAILURE;
            });

    assertThat(result.getExitCode()).isEqualTo(1);
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  void transientFailureThenSuccessRetriesOnce() throws Exception {
    AtomicLong time = new AtomicLong(0);
    OctopusCliRetryExecutor executor =
        createExecutor(new HashMap<>(), millis -> time.addAndGet(millis), time::get);

    AtomicInteger callCount = new AtomicInteger(0);

    CliResult result =
        executor.executeWithRetry(
            () -> {
              if (callCount.incrementAndGet() == 1) {
                return TRANSIENT_FAILURE;
              }
              return SUCCESS;
            });

    assertThat(result.getExitCode()).isEqualTo(0);
    assertThat(callCount.get()).isEqualTo(2);
  }

  @Test
  void transientFailuresUntilTimeoutReturnsLastFailure() throws Exception {
    Map<String, String> env = new HashMap<>();
    env.put("OCTOPUS_RETRY_TIMEOUT", "30000");
    env.put("OCTOPUS_RETRY_INITIAL_DELAY", "10000");

    AtomicLong time = new AtomicLong(0);
    OctopusCliRetryExecutor executor =
        createExecutor(env, millis -> time.addAndGet(millis), time::get);

    AtomicInteger callCount = new AtomicInteger(0);

    CliResult result =
        executor.executeWithRetry(
            () -> {
              callCount.incrementAndGet();
              return TRANSIENT_FAILURE;
            });

    assertThat(result.getExitCode()).isEqualTo(1);
    assertThat(callCount.get()).isGreaterThan(1);
  }

  @Test
  void backoffDelaysIncreaseExponentially() {
    OctopusCliRetryExecutor executor = createExecutor(new HashMap<>());

    long delay1 = executor.calculateDelay(1);
    long delay2 = executor.calculateDelay(2);
    long delay3 = executor.calculateDelay(3);

    assertThat(delay2).isGreaterThan(delay1);
    assertThat(delay3).isGreaterThan(delay2);
  }

  @Test
  void backoffDelayIsCappedAtMaxDelay() {
    Map<String, String> env = new HashMap<>();
    env.put("OCTOPUS_RETRY_MAX_DELAY", "60000");

    OctopusCliRetryExecutor executor = createExecutor(env);

    long delay = executor.calculateDelay(20);
    assertThat(delay).isLessThanOrEqualTo(60_000L);
  }

  @Test
  void retryDisabledViaEnvVar() throws Exception {
    Map<String, String> env = new HashMap<>();
    env.put("OCTOPUS_RETRY_ENABLED", "false");

    OctopusCliRetryExecutor executor = createExecutor(env);
    AtomicInteger callCount = new AtomicInteger(0);

    CliResult result =
        executor.executeWithRetry(
            () -> {
              callCount.incrementAndGet();
              return TRANSIENT_FAILURE;
            });

    assertThat(result.getExitCode()).isEqualTo(1);
    assertThat(callCount.get()).isEqualTo(1);
  }

  @Test
  void invalidEnvVarsFallBackToDefaults() {
    Map<String, String> env = new HashMap<>();
    env.put("OCTOPUS_RETRY_TIMEOUT", "not-a-number");
    env.put("OCTOPUS_RETRY_INITIAL_DELAY", "");

    OctopusCliRetryExecutor executor = createExecutor(env);
    long delay = executor.calculateDelay(1);
    assertThat(delay).isGreaterThan(0);
  }

  @Test
  void interruptedExceptionPropagates() {
    AtomicLong time = new AtomicLong(0);
    OctopusCliRetryExecutor executor =
        createExecutor(
            new HashMap<>(),
            millis -> {
              throw new InterruptedException("build cancelled");
            },
            time::get);

    assertThatThrownBy(() -> executor.executeWithRetry(() -> TRANSIENT_FAILURE))
        .isInstanceOf(InterruptedException.class)
        .hasMessageContaining("build cancelled");
  }

  @Test
  void transientThenPermanentStopsRetrying() throws Exception {
    AtomicLong time = new AtomicLong(0);
    OctopusCliRetryExecutor executor =
        createExecutor(new HashMap<>(), millis -> time.addAndGet(millis), time::get);

    AtomicInteger callCount = new AtomicInteger(0);

    CliResult result =
        executor.executeWithRetry(
            () -> {
              if (callCount.incrementAndGet() == 1) {
                return TRANSIENT_FAILURE;
              }
              return PERMANENT_FAILURE;
            });

    assertThat(result.getExitCode()).isEqualTo(1);
    assertThat(callCount.get()).isEqualTo(2);
    assertThat(result.getCombinedOutput()).contains("Could not find project");
  }

  @Test
  void successLogsRetryMessage() throws Exception {
    BuildProgressLogger mockLogger = mock(BuildProgressLogger.class);
    AtomicLong time = new AtomicLong(0);
    OctopusCliRetryExecutor executor =
        new OctopusCliRetryExecutor(
            mockLogger,
            new HashMap<>(),
            millis -> time.addAndGet(millis),
            time::get,
            new Random(42));

    AtomicInteger callCount = new AtomicInteger(0);
    executor.executeWithRetry(
        () -> {
          if (callCount.incrementAndGet() == 1) {
            return TRANSIENT_FAILURE;
          }
          return SUCCESS;
        });

    verify(mockLogger).warning(anyString());
    verify(mockLogger).message(anyString());
  }
}
