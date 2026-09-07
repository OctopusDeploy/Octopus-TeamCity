package octopus.teamcity.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ReleaseSummaryTest {

  private static Optional<ReleaseSummary> roundTrip(final ReleaseSummary summary)
      throws IOException {
    final ByteArrayOutputStream written = new ByteArrayOutputStream();
    summary.writeTo(written);
    return ReleaseSummary.readFrom(new ByteArrayInputStream(written.toByteArray()));
  }

  @Test
  void survivesTheTripFromTheAgentToTheServer() throws Exception {
    final Optional<ReleaseSummary> read =
        roundTrip(
            new ReleaseSummary(
                "https://my.octopus.app/app#/Spaces-1/releases/Releases-14", "1.2.3"));

    assertThat(read).isPresent();
    assertThat(read.get().getUrl())
        .isEqualTo("https://my.octopus.app/app#/Spaces-1/releases/Releases-14");
    assertThat(read.get().getVersion()).isEqualTo("1.2.3");
  }

  @Test
  void keepsTheUrlOfAReleaseWithoutAKnownVersion() throws Exception {
    final Optional<ReleaseSummary> read =
        roundTrip(
            new ReleaseSummary("https://my.octopus.app/app#/Spaces-1/releases/Releases-14", null));

    assertThat(read).isPresent();
    assertThat(read.get().getVersion()).isNull();
  }

  @Test
  void ignoresAFileThatNamesNoRelease() throws Exception {
    assertThat(ReleaseSummary.readFrom(new ByteArrayInputStream("version=1.2.3".getBytes("UTF-8"))))
        .isEmpty();
    assertThat(ReleaseSummary.readFrom(new ByteArrayInputStream(new byte[0]))).isEmpty();
  }

  @Test
  void namesOneFilePerStepSoStepsDoNotOverwriteEachOther() {
    assertThat(ReleaseSummary.artifactNameFor("RUNNER_1"))
        .isNotEqualTo(ReleaseSummary.artifactNameFor("RUNNER_2"));
    assertThat(ReleaseSummary.isSummary(ReleaseSummary.artifactNameFor("RUNNER_1"))).isTrue();
  }

  @Test
  void keepsAStepIdOutOfTheFileNameIfItCouldNotBeOne() {
    assertThat(ReleaseSummary.artifactNameFor("../../etc/passwd"))
        .isEqualTo("release-.._.._etc_passwd.properties");
  }

  @Test
  void recognisesOnlyItsOwnFiles() {
    assertThat(ReleaseSummary.isSummary("notes.txt")).isFalse();
    assertThat(ReleaseSummary.isSummary("release-RUNNER_1.properties.bak")).isFalse();
  }
}
