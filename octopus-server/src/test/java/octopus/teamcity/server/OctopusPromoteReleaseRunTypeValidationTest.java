package octopus.teamcity.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class OctopusPromoteReleaseRunTypeValidationTest {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  private Collection<String> validate(final Map<String, String> properties) {
    final OctopusPromoteReleaseRunType runType =
        new OctopusPromoteReleaseRunType(mock(RunTypeRegistry.class), mock(PluginDescriptor.class));
    return runType.getRunnerPropertiesProcessor().process(properties).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  private Map<String, String> withMandatoryNonCredentialFields(
      final Map<String, String> properties) {
    properties.put(CONSTANTS.getProjectNameKey(), "MyProject");
    properties.put(CONSTANTS.getPromoteFromKey(), "Staging");
    properties.put(CONSTANTS.getDeployToKey(), "Production");
    return properties;
  }

  @Test
  void connectionOnlyIsValidForServerAndKey() {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    properties.put(CONSTANTS.getConnectionIdKey(), "c1");
    final Collection<String> errors = validate(properties);
    assertThat(errors).doesNotContain(CONSTANTS.getServerKey(), CONSTANTS.getApiKey());
  }

  @Test
  void manualOnlyIsValid() {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    assertThat(validate(properties))
        .doesNotContain(CONSTANTS.getServerKey(), CONSTANTS.getApiKey());
  }

  @Test
  void neitherConnectionNorManualIsInvalid() {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    assertThat(validate(properties)).contains(CONSTANTS.getServerKey(), CONSTANTS.getApiKey());
  }
}
