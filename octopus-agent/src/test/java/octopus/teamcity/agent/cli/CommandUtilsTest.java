package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import octopus.teamcity.common.OverwriteMode;
import org.junit.jupiter.api.Test;

class CommandUtilsTest {

  @Test
  void parsesVersionFromJson() {
    String jsonOutput = "{\"Version\": \"1.0.0\"}";
    assertThat(CommandUtils.getReleaseVersion(jsonOutput)).isEqualTo("1.0.0");
  }

  @Test
  void parsesTaskIdFromJsonArray() {
    String jsonOutput = "[{\"ServerTaskId\": \"task-123\"}]";
    assertThat(CommandUtils.getServerTaskId(jsonOutput)).isEqualTo("task-123");
  }

  @Test
  void isCreateReleasereturnsTrueWhenOutputContainsVersion() {
    assertThat(CommandUtils.isCreateReleaseCommand("{\"Version\": \"1.0.0\"}"))
        .isTrue();
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
    assertThat(CommandUtils.isDeployRelease("[{\"ServerTaskId\": \"task-123\"}]")).isTrue();
  }

  @Test
  void isDeployReleaseReturnsFalseWhenNonServerTaskIdInOutput() {
    assertThat(CommandUtils.isDeployRelease("[{\"State\": \"Success\"}]")).isFalse();
  }

  @Test
  void isDeployReleaseReturnsFalseWhenOutputIsEmpty() {
    assertThat(CommandUtils.isDeployRelease("")).isFalse();
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
    assertThat(CommandUtils.getOverwriteMode(OverwriteMode.OverwriteExisting)).isEqualTo("overwrite");
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
