package octopus.teamcity.common.connection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnectionPropertyNamesTest {
  private final ConnectionPropertyNames keys = new ConnectionPropertyNames();

  @Test
  void exposesExpectedKeys() {
    assertThat(keys.getDisplayName()).isEqualTo("displayName");
    assertThat(keys.getServerUrlPropertyName()).isEqualTo("octopus_host");
    assertThat(keys.getApiKeyPropertyName()).isEqualTo("secure:octopus_apikey");
    assertThat(keys.getVersionPropertyName()).isEqualTo("octopus_version");
    assertThat(keys.getSpaceNamePropertyName()).isEqualTo("octopus_space_name");
  }
}
