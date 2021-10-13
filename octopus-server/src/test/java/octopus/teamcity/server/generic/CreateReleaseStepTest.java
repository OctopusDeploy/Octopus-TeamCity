package octopus.teamcity.server.generic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.common.createrelease.CreateReleasePropertyNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CreateReleaseStepTest {

  private final CreateReleaseStep step = new CreateReleaseStep();

  @Test
  public void describePropertiesProducesExpectedOutput() {
    final String message = step.describeParameters(buildPropertiesMap());
    assertThat("Project name: Project-1\nPackage version: 1.0.0").isEqualTo(message);
  }

  @Test
  public void validatePropertiesReturnsEmptyInvalidPropertiesListWithValidProperties() {
    final List<InvalidProperty> invalidProperties = step.validateProperties(buildPropertiesMap());
    assertThat(invalidProperties).isNotNull().hasSize(0);
  }

  @ParameterizedTest
  @MethodSource("provideNullAndEmptyTestArguments")
  public void validatePropertiesReturnsInvalidPropertyOnNullAndEmptyParams(
      String paramKey, String testCondition, String invalidReason) {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(paramKey, testCondition);

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(paramKey, invalidReason);
  }

  @Test
  public void validatePropertiesReturnsMultipleInvalidProperty() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, "");
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(2)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            CreateReleasePropertyNames.PROJECT_NAME,
            "Project name must be specified and cannot be whitespace.",
            CreateReleasePropertyNames.PACKAGE_VERSION,
            "Package version must be specified and cannot be whitespace.");
  }

  private Map<String, String> buildPropertiesMap() {
    final Map<String, String> validMap = new HashMap<>();
    // Mandatory/validated
    validMap.put(CreateReleasePropertyNames.PROJECT_NAME, "Project-1");
    validMap.put(CreateReleasePropertyNames.PACKAGE_VERSION, "1.0.0");
    // Optional/un-validated
    validMap.put(CreateReleasePropertyNames.RELEASE_VERSION, "2.0.0");
    validMap.put(CreateReleasePropertyNames.CHANNEL_NAME, "Channel-1");
    validMap.put(CreateReleasePropertyNames.PACKAGES, "stepName:PackageName:Version");
    return validMap;
  }

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideNullAndEmptyTestArguments() {
    return Stream.of(
        Arguments.of(
            CreateReleasePropertyNames.PROJECT_NAME,
            null,
            "Project name must be specified and cannot be whitespace."),
        Arguments.of(
            CreateReleasePropertyNames.PROJECT_NAME,
            "",
            "Project name must be specified and cannot be whitespace."),
        Arguments.of(
            CreateReleasePropertyNames.PACKAGE_VERSION,
            null,
            "Package version must be specified and cannot be whitespace."),
        Arguments.of(
            CreateReleasePropertyNames.PACKAGE_VERSION,
            "",
            "Package version must be specified and cannot be whitespace."));
  }
}
