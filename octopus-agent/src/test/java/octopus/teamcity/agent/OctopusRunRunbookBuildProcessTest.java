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

class OctopusRunRunbookBuildProcessTest {
  private static final OctopusConstants CONSTANTS = OctopusConstants.Instance;

  private OctopusRunRunbookBuildProcess processFor(final Map<String, String> params) {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(runningBuild.getBuildLogger()).thenReturn(logger);
    return new OctopusRunRunbookBuildProcess(runningBuild, context);
  }

  private Map<String, String> mandatoryParams() {
    Map<String, String> params = new HashMap<>();
    params.put(CONSTANTS.getServerKey(), "https://octopus.example.com");
    params.put(CONSTANTS.getApiKey(), "API-KEY");
    params.put(CONSTANTS.getProjectNameKey(), "MyProject");
    params.put(CONSTANTS.getRunbookNameKey(), "Rebuild indexes");
    params.put(CONSTANTS.getDeployToKey(), "Production");
    return params;
  }

  @Test
  void buildCommand_runsTheNamedRunbookInEachEnvironment() {
    Map<String, String> params = mandatoryParams();
    params.put(CONSTANTS.getDeployToKey(), "Development,Production");
    params.put(CONSTANTS.getRunbookSnapshotKey(), "Snapshot ABC");
    params.put(CONSTANTS.getTenantTagsKey(), "Regions/South");

    String[] command = processFor(params).createCommand().buildCommand();

    assertThat(command)
        .contains(
            "run-runbook",
            "--project",
            "MyProject",
            "--runbook",
            "Rebuild indexes",
            "--snapshot",
            "Snapshot ABC",
            "--environment",
            "Development",
            "--environment",
            "Production",
            "--tenantTag",
            "Regions/South");
  }

  @Test
  void buildCommand_includesWaitForRun_whenWaitingWithTimeout() {
    Map<String, String> params = mandatoryParams();
    params.put(CONSTANTS.getWaitForDeployments(), "true");
    params.put(CONSTANTS.getDeploymentTimeout(), "00:30:00");
    params.put(CONSTANTS.getCancelDeploymentOnTimeout(), "true");

    String[] command = processFor(params).createCommand().buildCommand();

    // --runTimeout/--cancelOnTimeout are silently ignored by the Octopus CLI unless --waitForRun
    // is also passed, so it must be present when waiting on a runbook run.
    assertThat(command).contains("--waitForRun");
    assertThat(command).contains("--runTimeout", "00:30:00");
    assertThat(command).contains("--cancelOnTimeout");
  }

  @Test
  void buildCommand_omitsWaitOptions_whenNotWaiting() {
    String[] command = processFor(mandatoryParams()).createCommand().buildCommand();

    assertThat(command).doesNotContain("--waitForRun", "--progress");
  }

  @Test
  void buildMaskedCommand_hidesTheApiKey() {
    String[] command = processFor(mandatoryParams()).createCommand().buildMaskedCommand();

    assertThat(command).contains("SECRET").doesNotContain("API-KEY");
  }
}
