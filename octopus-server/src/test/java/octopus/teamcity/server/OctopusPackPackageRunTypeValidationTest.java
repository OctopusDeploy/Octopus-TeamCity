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

class OctopusPackPackageRunTypeValidationTest {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  private Collection<String> validate(final Map<String, String> properties) {
    final OctopusPackPackageRunType runType =
        new OctopusPackPackageRunType(mock(RunTypeRegistry.class), mock(PluginDescriptor.class));
    return runType.getRunnerPropertiesProcessor().process(properties).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  private Map<String, String> withAllMandatoryFields(final Map<String, String> properties) {
    properties.put(CONSTANTS.getPackageIdKey(), "Acme.Web");
    properties.put(CONSTANTS.getPackageFormatKey(), "NuPkg");
    properties.put(CONSTANTS.getPackageVersionKey(), "1.0.0");
    properties.put(CONSTANTS.getPackageSourcePathKey(), "bin");
    properties.put(CONSTANTS.getPackageOutputPathKey(), "out");
    return properties;
  }

  @Test
  void allMandatoryFieldsIsValid() {
    assertThat(validate(withAllMandatoryFields(new HashMap<>()))).isEmpty();
  }

  @Test
  void emptyPropertiesReportsEveryMandatoryField() {
    assertThat(validate(new HashMap<>()))
        .containsExactlyInAnyOrder(
            CONSTANTS.getPackageIdKey(),
            CONSTANTS.getPackageFormatKey(),
            CONSTANTS.getPackageVersionKey(),
            CONSTANTS.getPackageSourcePathKey(),
            CONSTANTS.getPackageOutputPathKey());
  }

  @Test
  void missingPackageIdIsInvalid() {
    final Map<String, String> properties = withAllMandatoryFields(new HashMap<>());
    properties.remove(CONSTANTS.getPackageIdKey());
    assertThat(validate(properties)).containsExactly(CONSTANTS.getPackageIdKey());
  }

  @Test
  void blankPackageVersionIsInvalid() {
    final Map<String, String> properties = withAllMandatoryFields(new HashMap<>());
    properties.put(CONSTANTS.getPackageVersionKey(), "   ");
    assertThat(validate(properties)).containsExactly(CONSTANTS.getPackageVersionKey());
  }
}
