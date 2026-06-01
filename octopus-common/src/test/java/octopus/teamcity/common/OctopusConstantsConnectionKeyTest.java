package octopus.teamcity.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OctopusConstantsConnectionKeyTest {
  @Test
  void exposesConnectionIdKey() {
    assertThat(new OctopusConstants().getConnectionIdKey()).isEqualTo("octopus_connection_id");
  }
}
