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

class OctopusCreateReleaseRunTypeValidationTest {
  private static final OctopusConstants C = new OctopusConstants();

  private Collection<String> validate(final Map<String, String> p) {
    final OctopusCreateReleaseRunType runType =
        new OctopusCreateReleaseRunType(mock(RunTypeRegistry.class), mock(PluginDescriptor.class));
    return runType.getRunnerPropertiesProcessor().process(p).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  private Map<String, String> withMandatoryNonCredentialFields(final Map<String, String> p) {
    p.put(C.getProjectNameKey(), "MyProject");
    return p;
  }

  @Test
  void connectionOnlyIsValidForServerAndKey() {
    final Map<String, String> p = withMandatoryNonCredentialFields(new HashMap<>());
    p.put(C.getConnectionIdKey(), "c1");
    final Collection<String> errors = validate(p);
    assertThat(errors).doesNotContain(C.getServerKey(), C.getApiKey());
  }

  @Test
  void manualOnlyIsValid() {
    final Map<String, String> p = withMandatoryNonCredentialFields(new HashMap<>());
    p.put(C.getServerKey(), "https://octo");
    p.put(C.getApiKey(), "API-KEY");
    assertThat(validate(p)).doesNotContain(C.getServerKey(), C.getApiKey());
  }

  @Test
  void neitherConnectionNorManualIsInvalid() {
    final Map<String, String> p = withMandatoryNonCredentialFields(new HashMap<>());
    assertThat(validate(p)).contains(C.getServerKey(), C.getApiKey());
  }
}
