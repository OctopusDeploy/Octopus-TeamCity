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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OctopusCreateReleaseRunTypeValidationTest {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  private Collection<InvalidProperty> invalidProperties(final Map<String, String> properties) {
    final OctopusCreateReleaseRunType runType =
        new OctopusCreateReleaseRunType(mock(RunTypeRegistry.class), mock(PluginDescriptor.class));
    return runType.getRunnerPropertiesProcessor().process(properties);
  }

  private Collection<String> validate(final Map<String, String> properties) {
    return invalidProperties(properties).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  private Map<String, String> withMandatoryNonCredentialFields(
      final Map<String, String> properties) {
    properties.put(CONSTANTS.getProjectNameKey(), "MyProject");
    return properties;
  }

  @Test
  void connectionOnlyIsValidForServerAndKey() {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
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

  private Map<String, String> withAdditionalArguments(final String additionalArguments) {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    properties.put(CONSTANTS.getCommandLineArgumentsKey(), additionalArguments);
    return properties;
  }

  @Test
  void promptedVariableWithoutDeployToIsInvalid() {
    assertThat(validate(withAdditionalArguments("--variable ImageTag:1.5.2")))
        .contains(CONSTANTS.getCommandLineArgumentsKey());
  }

  @Test
  void promptedVariableWithAnInlineValueAndWithoutDeployToIsInvalid() {
    assertThat(validate(withAdditionalArguments("--progress --variable=ImageTag:1.5.2")))
        .contains(CONSTANTS.getCommandLineArgumentsKey());
  }

  @Test
  void promptedVariableWithDeployToIsValid() {
    final Map<String, String> properties = withAdditionalArguments("--variable ImageTag:1.5.2");
    properties.put(CONSTANTS.getDeployToKey(), "Dev");
    assertThat(validate(properties)).doesNotContain(CONSTANTS.getCommandLineArgumentsKey());
  }

  @Test
  void otherArgumentsEndingInVariableAreValid() {
    assertThat(validate(withAdditionalArguments("--update-variables --progress")))
        .doesNotContain(CONSTANTS.getCommandLineArgumentsKey());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "--deployTo Development",
        "--deployto=Development",
        "-deployTo Development",
        "/DEPLOYTO Development",
        "--variable ImageTag:1.5.2 --deployTo Development"
      })
  void legacyDeployToArgumentIsInvalid(final String additionalArguments) {
    final Collection<InvalidProperty> errors =
        invalidProperties(withAdditionalArguments(additionalArguments));
    assertThat(errors)
        .extracting(InvalidProperty::getInvalidReason)
        .containsExactly(
            "The Octopus CLI has no --deployTo argument. Enter the environments to deploy to in"
                + " 'Deploy to' instead.");
  }

  @Test
  void noAdditionalArgumentsIsValid() {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    assertThat(validate(properties)).doesNotContain(CONSTANTS.getCommandLineArgumentsKey());
  }
}
