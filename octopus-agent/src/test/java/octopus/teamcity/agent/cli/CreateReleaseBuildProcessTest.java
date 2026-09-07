package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.ReleaseSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CreateReleaseBuildProcessTest {

  private static final String CREATE_RELEASE_JSON =
      "{\"ID\": \"Releases-14\", \"Version\": \"1.2.3\", \"Channel\": \"Default\"}";
  private static final String SPACE_VIEW_JSON =
      "{\"Id\": \"Spaces-162\", \"Name\": \"Build Platform\", \"TaskQueue\": \"Running\"}";

  private BuildProgressLogger logger;

  @TempDir File buildTempDirectory;

  private Object getPrivateField(Object instance, String fieldName) throws Exception {
    Field f = instance.getClass().getDeclaredField(fieldName);
    f.setAccessible(true);
    return f.get(instance);
  }

  private CreateReleaseBuildProcess buildProcessFor(Map<String, String> params) {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(context.getId()).thenReturn("RUNNER_1");
    when(context.getBuild()).thenReturn(runningBuild);
    when(runningBuild.getBuildLogger()).thenReturn(logger);
    when(runningBuild.getBuildTempDirectory()).thenReturn(buildTempDirectory);

    return new CreateReleaseBuildProcess(runningBuild, context);
  }

  private static Map<String, String> params(String... keysAndValues) {
    final Map<String, String> params = new HashMap<>();
    for (int i = 0; i < keysAndValues.length; i += 2) {
      params.put(keysAndValues[i], keysAndValues[i + 1]);
    }
    return params;
  }

  @Test
  void processOutput_setsAutoCreatedReleaseNumber_whenCreateReleaseOutput() throws Exception {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc = buildProcessFor(params(constants.getDeployToKey(), "test"));

    proc.processOutput("{\"Version\": \"1.2.3\"}", 0);

    assertThat(getPrivateField(proc, "autoCreatedReleaseNumber")).isEqualTo("1.2.3");
  }

  @Test
  void processOutput_setsServerTaskId_whenDeployOutput() throws Exception {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc =
        buildProcessFor(params(constants.getWaitForDeployments(), "true"));

    proc.processOutput("[{\"ServerTaskId\": \"task-xyz\"}]", 0);

    assertThat(getPrivateField(proc, "serverTaskId")).isEqualTo("task-xyz");
  }

  @Test
  void logsALinkToTheReleaseItCreated() {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc =
        buildProcessFor(params(constants.getServerKey(), "https://my.octopus.app"));

    proc.processOutput(SPACE_VIEW_JSON, 0);
    proc.processOutput(CREATE_RELEASE_JSON, 0);

    verify(logger)
        .message(
            "View this release in Octopus Deploy: "
                + "https://my.octopus.app/app#/Spaces-162/releases/Releases-14");
  }

  @Test
  void publishesTheReleaseAsParametersForLaterSteps() {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc =
        buildProcessFor(params(constants.getServerKey(), "https://my.octopus.app"));

    proc.processOutput(SPACE_VIEW_JSON, 0);
    proc.processOutput(CREATE_RELEASE_JSON, 0);

    verify(logger)
        .message(
            contains(
                "setParameter name='"
                    + CreateReleaseBuildProcess.RELEASE_URL_PARAMETER
                    + "' value='https://my.octopus.app/app#/Spaces-162/releases/Releases-14'"));
    verify(logger)
        .message(
            contains(
                "setParameter name='"
                    + CreateReleaseBuildProcess.RELEASE_NUMBER_PARAMETER
                    + "' value='1.2.3'"));
  }

  @Test
  void recordsTheReleaseForTheBuildOverview() throws Exception {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc =
        buildProcessFor(params(constants.getServerKey(), "https://my.octopus.app"));

    proc.processOutput(SPACE_VIEW_JSON, 0);
    proc.processOutput(CREATE_RELEASE_JSON, 0);

    final File summary = new File(buildTempDirectory, ReleaseSummary.artifactNameFor("RUNNER_1"));
    try (InputStream contents = new FileInputStream(summary)) {
      final Optional<ReleaseSummary> release = ReleaseSummary.readFrom(contents);
      assertThat(release).isPresent();
      assertThat(release.get().getUrl())
          .isEqualTo("https://my.octopus.app/app#/Spaces-162/releases/Releases-14");
      assertThat(release.get().getVersion()).isEqualTo("1.2.3");
    }

    verify(logger)
        .message(
            contains(
                "publishArtifacts '"
                    + summary.getAbsolutePath()
                    + " => "
                    + ReleaseSummary.ARTIFACT_DIRECTORY
                    + "'"));
  }

  @Test
  void warnsRatherThanFailingWhenTheSpaceIsUnknown() {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc =
        buildProcessFor(params(constants.getServerKey(), "https://my.octopus.app"));

    proc.processOutput(CREATE_RELEASE_JSON, 0);

    verify(logger, never()).message(startsWith("View this release"));
    verify(logger).warning(contains("will not link to it"));
  }

  @Test
  void warnsRatherThanFailingWhenTheCreatedReleaseCannotBeRead() {
    final OctopusConstants constants = OctopusConstants.Instance;
    CreateReleaseBuildProcess proc =
        buildProcessFor(params(constants.getServerKey(), "https://my.octopus.app"));

    proc.processOutput(SPACE_VIEW_JSON, 0);
    proc.processOutput("Warning: cannot fetch release details. Version unknown", 0);

    verify(logger, never()).message(startsWith("View this release"));
    verify(logger).warning(contains("will not link to it"));
  }

  @Test
  void looksUpTheSpaceOnlyWhenTheStepIsNotAlreadyConfiguredWithItsId() throws Exception {
    final OctopusConstants constants = OctopusConstants.Instance;

    CreateReleaseBuildProcess byName =
        buildProcessFor(params(constants.getSpaceName(), "Build Platform"));
    assertThat(commandNames(byName.createCommand())).contains("space view");

    CreateReleaseBuildProcess byId =
        buildProcessFor(params(constants.getSpaceName(), "Spaces-162"));
    assertThat(commandNames(byId.createCommand())).doesNotContain("space view");
    assertThat(getPrivateField(byId, "spaceId")).isEqualTo("Spaces-162");
  }

  private static List<String> commandNames(List<OctopusCommandBuilder> commands) {
    final List<String> names = new ArrayList<>();
    for (OctopusCommandBuilder command : commands) {
      final String[] arguments = command.buildCommand();
      names.add(arguments.length > 1 ? arguments[0] + " " + arguments[1] : arguments[0]);
    }
    return names;
  }
}
