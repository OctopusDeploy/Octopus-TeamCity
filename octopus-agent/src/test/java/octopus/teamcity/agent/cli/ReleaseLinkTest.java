package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReleaseLinkTest {

  @Test
  void buildsTheAddressOctopusItselfUsesForARelease() {
    assertThat(ReleaseLink.of("https://my.octopus.app", "Spaces-162", "Releases-14"))
        .contains("https://my.octopus.app/app#/Spaces-162/releases/Releases-14");
  }

  @Test
  void doesNotDoubleUpTheSlashOnAServerUrlThatEndsInOne() {
    assertThat(ReleaseLink.of("https://my.octopus.app/  ", "Spaces-1", "Releases-14"))
        .contains("https://my.octopus.app/app#/Spaces-1/releases/Releases-14");
  }

  @Test
  void hasNoAddressWithoutAServerSpaceAndRelease() {
    assertThat(ReleaseLink.of(null, "Spaces-1", "Releases-14")).isEmpty();
    assertThat(ReleaseLink.of("https://my.octopus.app", "", "Releases-14")).isEmpty();
    assertThat(ReleaseLink.of("https://my.octopus.app", "Spaces-1", null)).isEmpty();
  }
}
