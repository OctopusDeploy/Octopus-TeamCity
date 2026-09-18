package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class CreateReleaseBuildProcessTest {

  private static final OctopusConstants CONSTANTS = OctopusConstants.Instance;
  private static final String CREATE_RELEASE_JSON =
      "{\"ID\": \"Releases-14\", \"Version\": \"1.2.3\", \"Channel\": \"Default\"}";
  private static final String SPACE_VIEW_JSON =
      "{\"Id\": \"Spaces-162\", \"Name\": \"Build Platform\", \"TaskQueue\": \"Running\"}";
  private static final String DEPLOY_RELEASE_JSON = "[{\"ServerTaskId\": \"task-xyz\"}]";

  private BuildProgressLogger logger;

  private CreateReleaseBuildProcess buildProcessFor(Map<String, String> params) {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);
    when(context.getRunnerParameters()).thenReturn(params);
    when(runningBuild.getBuildLogger()).thenReturn(logger);

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
  void logsALinkToTheReleaseItCreated() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(params(CONSTANTS.getServerKey(), "https://my.octopus.app")).createCommand();

    Commands.of(commands, SpaceViewCommand.class).readResponse(SPACE_VIEW_JSON);
    Commands.of(commands, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);

    verify(logger)
        .message(
            "View this release in Octopus Deploy: "
                + "https://my.octopus.app/app#/Spaces-162/releases/Releases-14");
  }

  @Test
  void publishesTheReleaseAsParametersForLaterSteps() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(params(CONSTANTS.getServerKey(), "https://my.octopus.app")).createCommand();

    Commands.of(commands, SpaceViewCommand.class).readResponse(SPACE_VIEW_JSON);
    Commands.of(commands, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);

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
  void namesTheReleaseProjectAndSpaceItCreatedIn() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(
                params(
                    CONSTANTS.getServerKey(),
                    "https://my.octopus.app",
                    CONSTANTS.getProjectNameKey(),
                    "Deploy Web"))
            .createCommand();

    Commands.of(commands, SpaceViewCommand.class).readResponse(SPACE_VIEW_JSON);
    Commands.of(commands, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);

    verify(logger).message("Created release 1.2.3 of project Deploy Web in space Spaces-162");
  }

  @Test
  void warnsRatherThanFailingWhenTheSpaceIsUnknown() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(params(CONSTANTS.getServerKey(), "https://my.octopus.app")).createCommand();

    // The space view command failed, so it never answered.
    Commands.of(commands, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);

    verify(logger, never()).message(startsWith("View this release"));
    verify(logger).warning(contains("will not link to it"));
    verify(logger).warning(contains("The space was not known"));
    verify(logger).warning(contains(CREATE_RELEASE_JSON));
  }

  @Test
  void warnsRatherThanFailingWhenTheCreatedReleaseCannotBeRead() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(params(CONSTANTS.getServerKey(), "https://my.octopus.app")).createCommand();

    Commands.of(commands, SpaceViewCommand.class).readResponse(SPACE_VIEW_JSON);
    Commands.of(commands, CreateReleaseCommand.class)
        .readResponse("Warning: cannot fetch release details. Version unknown");

    verify(logger, never()).message(startsWith("View this release"));
    verify(logger).warning(contains("will not link to it"));
    // Whatever the CLI said instead goes in the warning, or there is nothing to work back from.
    verify(logger).warning(contains("Warning: cannot fetch release details. Version unknown"));
  }

  /**
   * A release the step cannot name is a release the deployment it is about to run cannot deploy, so
   * this one is worth failing over - with the response that could not be read, rather than a parser
   * error from three frames down.
   */
  @Test
  void failsWithTheCliResponseWhenADeployingStepCannotReadTheReleaseNumber() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(
                params(
                    CONSTANTS.getServerKey(),
                    "https://my.octopus.app",
                    CONSTANTS.getDeployToKey(),
                    "Development"))
            .createCommand();

    assertThatThrownBy(
            () ->
                Commands.of(commands, CreateReleaseCommand.class)
                    .readResponse("Warning: cannot fetch release details. Version unknown"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("no release to deploy")
        .hasMessageContaining("Warning: cannot fetch release details. Version unknown");
  }

  @Test
  void deploysTheReleaseTheCreateCommandSaidItMade() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(
                params(
                    CONSTANTS.getServerKey(),
                    "https://my.octopus.app",
                    CONSTANTS.getDeployToKey(),
                    "Development"))
            .createCommand();

    Commands.of(commands, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);

    assertThat(Commands.of(commands, DeployReleaseCommand.class).buildCommand())
        .containsSequence("--version", "1.2.3");
  }

  @Test
  void waitsForTheTaskTheDeploymentStarted() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(
                params(
                    CONSTANTS.getDeployToKey(),
                    "Development",
                    CONSTANTS.getWaitForDeployments(),
                    "true"))
            .createCommand();

    Commands.of(commands, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);
    Commands.of(commands, DeployReleaseCommand.class).readResponse(DEPLOY_RELEASE_JSON);

    assertThat(Commands.of(commands, WaitForTaskCommand.class).buildCommand()).contains("task-xyz");
  }

  @Test
  void looksUpTheSpaceOnlyWhenTheStepIsNotAlreadyConfiguredWithItsId() {
    final List<OctopusCommandBuilder> byName =
        buildProcessFor(params(CONSTANTS.getSpaceName(), "Build Platform")).createCommand();
    assertThat(Commands.ranA(byName, SpaceViewCommand.class)).isTrue();

    final List<OctopusCommandBuilder> byId =
        buildProcessFor(
                params(
                    CONSTANTS.getSpaceName(),
                    "Spaces-162",
                    CONSTANTS.getServerKey(),
                    "https://my.octopus.app"))
            .createCommand();
    assertThat(Commands.ranA(byId, SpaceViewCommand.class)).isFalse();

    // Configuration alone is enough to link to the release, with no space view to answer.
    Commands.of(byId, CreateReleaseCommand.class).readResponse(CREATE_RELEASE_JSON);
    verify(logger)
        .message(
            "View this release in Octopus Deploy: "
                + "https://my.octopus.app/app#/Spaces-162/releases/Releases-14");
  }

  @Test
  void doesNotDeployOrWaitWhenTheStepOnlyCreatesARelease() {
    final List<OctopusCommandBuilder> commands =
        buildProcessFor(params(CONSTANTS.getWaitForDeployments(), "true")).createCommand();

    assertThat(Commands.ranA(commands, DeployReleaseCommand.class)).isFalse();
    assertThat(Commands.ranA(commands, WaitForTaskCommand.class)).isFalse();
  }
}
