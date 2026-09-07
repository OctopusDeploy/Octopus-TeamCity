package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;

import octopus.teamcity.common.OverwriteMode;
import org.junit.jupiter.api.Test;

class CommandUtilsTest {

  @Test
  void parsesVersionFromJson() {
    String jsonOutput = "{\"Version\": \"1.0.0\"}";
    assertThat(CommandUtils.getReleaseVersion(jsonOutput)).isEqualTo("1.0.0");
  }

  @Test
  void parsesReleaseIdFromJson() {
    String jsonOutput = "{\"ID\": \"Releases-14\", \"Version\": \"1.0.0\"}";
    assertThat(CommandUtils.getReleaseId(jsonOutput)).isEqualTo("Releases-14");
  }

  @Test
  void parsesSpaceIdFromJson() {
    String jsonOutput = "{\"Id\": \"Spaces-162\", \"Name\": \"Build Platform\"}";
    assertThat(CommandUtils.getSpaceId(jsonOutput)).isEqualTo("Spaces-162");
  }

  @Test
  void isSpaceViewReturnsTrueForASpacesOwnResponse() {
    String jsonOutput =
        "{\"Id\": \"Spaces-162\", \"Name\": \"Build Platform\", \"Description\": \"\","
            + " \"TaskQueue\": \"Running\", \"WebUrl\": \"https://my.octopus.app/app#/configuration/spaces/Spaces-162\"}";
    assertThat(CommandUtils.isSpaceViewCommand(jsonOutput)).isTrue();
  }

  @Test
  void isSpaceViewReturnsFalseForAReleaseWhoseNotesMentionATaskQueue() {
    String jsonOutput =
        "{\"ID\": \"Releases-14\", \"Version\": \"1.0.0\","
            + " \"ReleaseNotes\": \"Id and TaskQueue handling\"}";
    assertThat(CommandUtils.isSpaceViewCommand(jsonOutput)).isFalse();
  }

  @Test
  void isSpaceViewReturnsFalseForOutputThatIsNotAJsonObject() {
    assertThat(CommandUtils.isSpaceViewCommand("")).isFalse();
    assertThat(CommandUtils.isSpaceViewCommand("Error: no space found")).isFalse();
    assertThat(CommandUtils.isSpaceViewCommand("[{\"TaskQueue\": \"Running\"}]")).isFalse();
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
    assertThat(CommandUtils.getServerTaskId(jsonOutput)).isEqualTo("task-123");
  }

  @Test
  void isCreateReleasereturnsTrueWhenOutputContainsVersion() {
    assertThat(CommandUtils.isCreateReleaseCommand("{\"Version\": \"1.0.0\"}")).isTrue();
  }

  @Test
  void isCreateRelease_returnsFalseWhenOutputDoesNotContainVersion() {
    assertThat(CommandUtils.isCreateReleaseCommand("{\"Id\": \"1\", \"Name\": \"Release1\"}"))
        .isFalse();
  }

  @Test
  void isCreateRelease_ReturnsFalseWhenOutputIsEmpty() {
    assertThat(CommandUtils.isCreateReleaseCommand("")).isFalse();
  }

  @Test
  void isDeployReleaseReturnsTrueWhenOutputContainsServerTaskId() {
    assertThat(CommandUtils.isDeployReleaseCommand("[{\"ServerTaskId\": \"task-123\"}]")).isTrue();
  }

  @Test
  void isDeployReleaseReturnsFalseWhenNonServerTaskIdInOutput() {
    assertThat(CommandUtils.isDeployReleaseCommand("[{\"State\": \"Success\"}]")).isFalse();
  }

  @Test
  void isDeployReleaseReturnsFalseWhenOutputIsEmpty() {
    assertThat(CommandUtils.isDeployReleaseCommand("")).isFalse();
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
