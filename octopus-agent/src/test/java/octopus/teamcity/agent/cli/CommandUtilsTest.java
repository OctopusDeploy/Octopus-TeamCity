package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;

import octopus.teamcity.common.OverwriteMode;
import org.junit.jupiter.api.Test;

class CommandUtilsTest {

  @Test
  void parsesVersionFromJson() {
    String jsonOutput = "{\"Version\": \"1.0.0\"}";
    assertThat(CommandUtils.getReleaseVersion(jsonOutput)).contains("1.0.0");
  }

  @Test
  void parsesReleaseIdFromJson() {
    String jsonOutput = "{\"ID\": \"Releases-14\", \"Version\": \"1.0.0\"}";
    assertThat(CommandUtils.getReleaseId(jsonOutput)).contains("Releases-14");
  }

  @Test
  void parsesSpaceIdFromJson() {
    String jsonOutput = "{\"Id\": \"Spaces-162\", \"Name\": \"Build Platform\"}";
    assertThat(CommandUtils.getSpaceId(jsonOutput)).contains("Spaces-162");
  }

  /**
   * Whatever the CLI printed instead of the JSON that was asked for, reading a value out of it
   * answers "not there" - the caller decides what to do without it, and never has to handle an
   * exception to find out.
   */
  @Test
  void readsNothingRatherThanThrowingWhenTheOutputIsNotTheExpectedJson() {
    for (String output :
        new String[] {
          null,
          "",
          "   ",
          "Warning: cannot fetch release details. Version unknown",
          "Error: project 'Deploy Web' not found",
          "[{\"Version\": \"1.0.0\"}]",
          "{\"Version\": {\"Major\": 1}}",
          "{\"Version\": \"\"}",
          "{\"Channel\": \"Default\"}"
        }) {
      assertThat(CommandUtils.getReleaseVersion(output)).isEmpty();
      assertThat(CommandUtils.getReleaseId(output)).isEmpty();
      assertThat(CommandUtils.getSpaceId(output)).isEmpty();
    }
  }

  @Test
  void isSpaceIdRecognisesOnlyAnActualSpaceId() {
    assertThat(CommandUtils.isSpaceId("Spaces-1")).isTrue();
    assertThat(CommandUtils.isSpaceId(" Spaces-162 ")).isTrue();
    assertThat(CommandUtils.isSpaceId("Default")).isFalse();
    assertThat(CommandUtils.isSpaceId("Spaces-")).isFalse();
    assertThat(CommandUtils.isSpaceId(null)).isFalse();
  }

  @Test
  void parsesTaskIdFromJsonArray() {
    String jsonOutput = "[{\"ServerTaskId\": \"task-123\"}]";
    assertThat(CommandUtils.getServerTaskId(jsonOutput)).contains("task-123");
  }

  @Test
  void readsNoTaskIdRatherThanThrowingWhenTheOutputIsNotTheExpectedJson() {
    for (String output :
        new String[] {
          null,
          "",
          "Error: the deployment was not started",
          "[]",
          "[\"task-123\"]",
          "{\"ServerTaskId\": \"task-123\"}",
          "[{\"State\": \"Success\"}]"
        }) {
      assertThat(CommandUtils.getServerTaskId(output)).isEmpty();
    }
  }

  @Test
  void loggableOutputKeepsAResponseShortEnoughToReadInABuildLog() {
    assertThat(CommandUtils.loggableOutput(null)).isEmpty();
    assertThat(CommandUtils.loggableOutput("  Error: no space found  "))
        .isEqualTo("Error: no space found");

    final String longOutput = new String(new char[600]).replace("\0", "x");
    assertThat(CommandUtils.loggableOutput(longOutput))
        .hasSize(500 + "... (truncated)".length())
        .endsWith("... (truncated)");
  }

  @Test
  void getOverwriteModeReturnsFailForFailIfExists() {
    assertThat(CommandUtils.getOverwriteMode(OverwriteMode.FailIfExists)).isEqualTo("fail");
  }

  @Test
  void getOverwriteModeReturnsIgnoreForIgnoreIfExists() {
    assertThat(CommandUtils.getOverwriteMode(OverwriteMode.IgnoreIfExists)).isEqualTo("ignore");
  }

  @Test
  void getOverwriteModeReturnsOverwriteForDefault() {
    assertThat(CommandUtils.getOverwriteMode(OverwriteMode.OverwriteExisting))
        .isEqualTo("overwrite");
  }

  @Test
  void getVersionReturnsReleaseNumberWhenProvided() {
    assertThat(CommandUtils.getVersion("1.0.0", "2.0.0")).isEqualTo("1.0.0");
  }

  @Test
  void getVersionReturnsReleaseNumberWhenAutoCreatedReleaseNumberIsEmpty() {
    assertThat(CommandUtils.getVersion("1.0.0", "")).isEqualTo("1.0.0");
  }

  @Test
  void getVersionReturnsReleaseNumberWhenAutoCreatedReleaseNumberIsNull() {
    assertThat(CommandUtils.getVersion("1.0.0", null)).isEqualTo("1.0.0");
  }

  @Test
  void getVersionReturnsAutoCreatedReleaseNumberWhenReleaseNumberIsNull() {
    assertThat(CommandUtils.getVersion(null, "2.0.0")).isEqualTo("2.0.0");
  }
}
