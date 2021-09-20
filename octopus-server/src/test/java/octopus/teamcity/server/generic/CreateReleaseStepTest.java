package octopus.teamcity.server.generic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.createrelease.CreateReleasePropertyNames;
import org.junit.jupiter.api.Test;

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
    assertThat(invalidProperties).isNotNull();
    assertThat(invalidProperties).hasSize(0);
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullProjectName() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, null);

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties).isNotNull();
    assertThat(invalidProperties).hasSize(1);
    assertThat(invalidProperties.get(0).getPropertyName())
        .isEqualTo(CreateReleasePropertyNames.PROJECT_NAME);
    assertThat(invalidProperties.get(0).getInvalidReason())
        .isEqualTo("Project name must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyProjectName() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties).isNotNull();
    assertThat(invalidProperties).hasSize(1);
    assertThat(invalidProperties.get(0).getPropertyName())
        .isEqualTo(CreateReleasePropertyNames.PROJECT_NAME);
    assertThat(invalidProperties.get(0).getInvalidReason())
        .isEqualTo("Project name must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullPackageVersion() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, null);

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties).isNotNull();
    assertThat(invalidProperties).hasSize(1);
    assertThat(invalidProperties.get(0).getPropertyName())
        .isEqualTo(CreateReleasePropertyNames.PACKAGE_VERSION);
    assertThat(invalidProperties.get(0).getInvalidReason())
        .isEqualTo("Package version must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyPackageVersion() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties).isNotNull();
    assertThat(invalidProperties).hasSize(1);
    assertThat(invalidProperties.get(0).getPropertyName())
        .isEqualTo(CreateReleasePropertyNames.PACKAGE_VERSION);
    assertThat(invalidProperties.get(0).getInvalidReason())
        .isEqualTo("Package version must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsMultipleInvalidProperty() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, "");
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties).isNotNull();
    assertThat(invalidProperties).hasSize(2);
    assertThat(invalidProperties.get(0).getPropertyName())
        .isEqualTo(CreateReleasePropertyNames.PROJECT_NAME);
    assertThat(invalidProperties.get(0).getInvalidReason())
        .isEqualTo("Project name must be specified and cannot be whitespace.");
    assertThat(invalidProperties.get(1).getPropertyName())
        .isEqualTo(CreateReleasePropertyNames.PACKAGE_VERSION);
    assertThat(invalidProperties.get(1).getInvalidReason())
        .isEqualTo("Package version must be specified and cannot be whitespace.");
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
}
