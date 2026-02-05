package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import octopus.teamcity.common.OverwriteMode;
import org.junit.jupiter.api.Test;

class CommandUtilsTest {

  @Test
  void getReleaseVersionParsesVersionFromJson() {
    String jsonOutput = "{\"Version\": \"1.0.0\"}";
    assertThat(CommandUtils.getReleaseVersion(jsonOutput)).isEqualTo("1.0.0");
  }

  @Test
  void getReleaseVersionThrowsExceptionWhenVersionFieldMissing() {
    String jsonOutput = "{\"Id\": \"1\", \"Name\": \"Release1\"}";
    assertThatThrownBy(() -> CommandUtils.getReleaseVersion(jsonOutput))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void getServerTaskIdParsesTaskIdFromJsonArray() {
    String jsonOutput = "[{\"ServerTaskId\": \"task-123\"}]";
    assertThat(CommandUtils.getServerTaskId(jsonOutput)).isEqualTo("task-123");
  }

  @Test
  void getServerTaskIdThrowsExceptionWhenServerTaskIdFieldMissing() {
    String jsonOutput = "[{\"State\": \"Success\"}]";
    assertThatThrownBy(() -> CommandUtils.getServerTaskId(jsonOutput))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void isCreateReleaseCommandReturnsTrueWhenOutputContainsVersion() {
    assertThat(CommandUtils.isCreateReleaseCommand("{\"Version\": \"1.0.0\"}"))
        .isTrue();
  }

  @Test
  void isCreateReleaseCommandReturnsTrueWithVersionInComplexJson() {
    assertThat(CommandUtils.isCreateReleaseCommand("{\"Version\": \"2.5.3\", \"Id\": \"1\"}"))
        .isTrue();
  }

  @Test
  void isCreateReleaseCommandReturnsFalseWhenOutputDoesNotContainVersion() {
    assertThat(CommandUtils.isCreateReleaseCommand("{\"Id\": \"1\", \"Name\": \"Release1\"}"))
        .isFalse();
  }

  @Test
  void isCreateReleaseCommandReturnsFalseWhenOutputIsEmpty() {
    assertThat(CommandUtils.isCreateReleaseCommand("")).isFalse();
  }

  @Test
  void isDeployReleaseReturnsTrueWhenOutputContainsServerTaskId() {
    assertThat(CommandUtils.isDeployRelease("[{\"ServerTaskId\": \"task-123\"}]")).isTrue();
  }

  @Test
  void isDeployReleaseReturnsTrueWithServerTaskIdInComplexJson() {
    assertThat(CommandUtils.isDeployRelease("[{\"ServerTaskId\": \"task-456\", \"State\": \"Success\"}]"))
        .isTrue();
  }

  @Test
  void isDeployReleaseReturnsFalseWhenOutputDoesNotContainServerTaskId() {
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
  void getVersionReturnsReleaseNumberWhenAutoCreatedReleaseNumberIsBlank() {
    assertThat(CommandUtils.getVersion("1.0.0", "   ")).isEqualTo("1.0.0");
  }

  @Test
  void getVersionReturnsAutoCreatedReleaseNumberWhenReleaseNumberIsEmpty() {
    assertThat(CommandUtils.getVersion("", "2.0.0")).isEqualTo("2.0.0");
  }

  @Test
  void getVersionReturnsAutoCreatedReleaseNumberWhenReleaseNumberIsNull() {
    assertThat(CommandUtils.getVersion(null, "2.0.0")).isEqualTo("2.0.0");
  }

  @Test
  void getVersionReturnsAutoCreatedReleaseNumberWhenReleaseNumberIsBlank() {
    assertThat(CommandUtils.getVersion("   ", "2.0.0")).isEqualTo("2.0.0");
  }

  @Test
  void getVersionReturnsEmptyStringWhenBothParametersAreEmpty() {
    assertThat(CommandUtils.getVersion("", "")).isEmpty();
  }

  @Test
  void getVersionReturnsEmptyStringWhenBothParametersAreNull() {
    assertThat(CommandUtils.getVersion(null, null)).isEmpty();
  }

  @Test
  void getVersionReturnsEmptyStringWhenBothParametersAreBlank() {
    assertThat(CommandUtils.getVersion("   ", "   ")).isEmpty();
  }
}
