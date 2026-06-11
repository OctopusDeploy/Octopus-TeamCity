package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class OctopusDeployReleaseBuildProcessTest {

  @Test
  void buildCommand_includesWaitForDeployment_whenWaitingWithTimeout() {
    final OctopusConstants constants = OctopusConstants.Instance;
    Map<String, String> params = new HashMap<>();
    params.put(constants.getServerKey(), "https://octopus.example.com");
    params.put(constants.getApiKey(), "API-KEY");
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getDeployToKey(), "Production");
    params.put(constants.getWaitForDeployments(), "true");
    params.put(constants.getDeploymentTimeout(), "00:30:00");

    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(runningBuild.getBuildLogger()).thenReturn(logger);

    OctopusDeployReleaseBuildProcess proc =
        new OctopusDeployReleaseBuildProcess(runningBuild, context);

    String[] command = proc.createCommand().buildCommand();

    // --deploymenttimeout/--cancelontimeout are silently ignored by the Octopus CLI unless
    // --waitfordeployment is also passed, so it must be present when waiting on a deployment.
    assertThat(command).contains("--waitfordeployment");
    assertThat(command).contains("--deploymenttimeout", "00:30:00");
  }
}
