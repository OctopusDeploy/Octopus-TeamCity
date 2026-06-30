package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

class CliSelectionTest {
  private final OctopusConstants constants = OctopusConstants.Instance;

  private AgentRunningBuild buildWithEnv(final Map<String, String> env) {
    final AgentRunningBuild build = mock(AgentRunningBuild.class);
    final BuildParametersMap params = mock(BuildParametersMap.class);
    lenient().when(build.getSharedBuildParameters()).thenReturn(params);
    lenient().when(params.getEnvironmentVariables()).thenReturn(env);
    return build;
  }

  private BuildRunnerContext contextWith(final Map<String, String> runnerParams) {
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    lenient().when(context.getRunnerParameters()).thenReturn(runnerParams);
    return context;
  }

  @Test
  void usesNewCliWhenEnvVarSet() {
    final Map<String, String> env = new HashMap<>();
    env.put("OCTOPUS_NEW_CLI", "true");
    assertThat(OctopusCliSelector.shouldUseNewCli(buildWithEnv(env), contextWith(new HashMap<>())))
        .isTrue();
  }

  @Test
  void usesNewCliWhenSourceIsOidc() {
    final Map<String, String> runnerParams = new HashMap<>();
    runnerParams.put(constants.getApiKeySourceKey(), ConnectionPropertyNames.API_KEY_SOURCE_OIDC);
    assertThat(
            OctopusCliSelector.shouldUseNewCli(
                buildWithEnv(new HashMap<>()), contextWith(runnerParams)))
        .isTrue();
  }

  @Test
  void usesLegacyCliByDefault() {
    assertThat(
            OctopusCliSelector.shouldUseNewCli(
                buildWithEnv(new HashMap<>()), contextWith(new HashMap<>())))
        .isFalse();
  }
}
