package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

class OctopusConnectionTest {

  private final OctopusConnection provider = new OctopusConnection(mock(PluginDescriptor.class));

  private OAuthConnectionDescriptor descriptorWith(final Map<String, String> params) {
    final OAuthConnectionDescriptor d = mock(OAuthConnectionDescriptor.class);
    when(d.getParameters()).thenReturn(params);
    return d;
  }

  @Test
  void typeAndDisplayName() {
    assertThat(provider.getType()).isEqualTo("OctopusConnection");
    assertThat(OctopusConnection.TYPE).isEqualTo("OctopusConnection");
    assertThat(provider.getDisplayName()).isEqualTo("Octopus Deploy");
  }

  @Test
  void defaultPropertiesSeedTheVersion() {
    assertThat(provider.getDefaultProperties())
        .containsEntry(ConnectionPropertyNames.VERSION, "3.0+");
  }

  @Test
  void describeConnectionOmitsTheDefaultVersion() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://octopus.example.com");
    params.put(ConnectionPropertyNames.VERSION, "3.0+");

    assertThat(provider.describeConnection(descriptorWith(params)))
        .isEqualTo("Octopus URL: https://octopus.example.com");
  }

  @Test
  void describeConnectionShowsTheVersionOnItsOwnLineWhenNotTheDefault() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://octopus.example.com");
    params.put(ConnectionPropertyNames.VERSION, "2019.1");

    assertThat(provider.describeConnection(descriptorWith(params)))
        .isEqualTo("Octopus URL: https://octopus.example.com\nVersion: 2019.1");
  }

  @Test
  void describeConnectionIncludesSpaceOnItsOwnLineWhenSet() {
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, "https://octopus.example.com");
    params.put(ConnectionPropertyNames.VERSION, "3.0+");
    params.put(ConnectionPropertyNames.SPACE_NAME, "My Space");

    assertThat(provider.describeConnection(descriptorWith(params)))
        .isEqualTo("Octopus URL: https://octopus.example.com\nSpace Name: My Space");
  }

  @Test
  void describeConnectionFallsBackWhenUrlMissing() {
    assertThat(provider.describeConnection(descriptorWith(new HashMap<>())))
        .isEqualTo("Octopus URL: (no URL)");
  }
}
