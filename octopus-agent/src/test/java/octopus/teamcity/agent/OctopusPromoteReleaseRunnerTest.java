package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

class OctopusPromoteReleaseRunnerTest {
  private final OctopusPromoteReleaseRunner runner = new OctopusPromoteReleaseRunner();

  private BuildRunnerContext contextWith(final Map<String, String> runnerParams) {
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    lenient().when(context.getRunnerParameters()).thenReturn(runnerParams);
    return context;
  }

  @Test
  void rejectsOidcSource() {
    final Map<String, String> params = new HashMap<>();
    params.put(
        OctopusConstants.Instance.getApiKeySourceKey(),
        ConnectionPropertyNames.API_KEY_SOURCE_OIDC);
    final AgentRunningBuild build = mock(AgentRunningBuild.class);

    assertThatThrownBy(() -> runner.createBuildProcess(build, contextWith(params)))
        .isInstanceOf(RunBuildException.class)
        .hasMessageContaining("OIDC authentication is not supported for the Promote release step");
  }

  @Test
  void allowsNonOidcSource() {
    final AgentRunningBuild build = mock(AgentRunningBuild.class);
    when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
    assertThatCode(() -> runner.createBuildProcess(build, contextWith(new HashMap<>())))
        .doesNotThrowAnyException();
  }
}
