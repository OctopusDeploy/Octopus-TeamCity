package octopus.teamcity.server.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    String message = step.describeParameters(buildPropertiesMap());
    assertEquals("Project name: Project-1\nPackage version: 1.0.0", message);
  }

  @Test
  public void validatePropertiesReturnsEmptyInvalidPropertiesListWithValidProperties() {
    List<InvalidProperty> invalidProperties = step.validateProperties(buildPropertiesMap());
    assertNotNull(invalidProperties);
    assertEquals(0, invalidProperties.size());
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullProjectName() {
    Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, null);

    List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertNotNull(invalidProperties);
    assertEquals(1, invalidProperties.size());
    assertEquals(
        "Project name must be specified and cannot be whitespace.",
        invalidProperties.get(0).getInvalidReason());
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyProjectName() {
    Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, "");

    List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertNotNull(invalidProperties);
    assertEquals(1, invalidProperties.size());
    assertEquals(
        "Project name must be specified and cannot be whitespace.",
        invalidProperties.get(0).getInvalidReason());
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullPackageVersion() {
    Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, null);

    List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertNotNull(invalidProperties);
    assertEquals(1, invalidProperties.size());
    assertEquals(
        "Package version must be specified and cannot be whitespace.",
        invalidProperties.get(0).getInvalidReason());
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyPackageVersion() {
    Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, "");

    List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertNotNull(invalidProperties);
    assertEquals(1, invalidProperties.size());
    assertEquals(
        "Package version must be specified and cannot be whitespace.",
        invalidProperties.get(0).getInvalidReason());
  }

  @Test
  public void validatePropertiesReturnsMultipleInvalidProperty() {
    Map<String, String> properties = buildPropertiesMap();
    properties.put(CreateReleasePropertyNames.PROJECT_NAME, "");
    properties.put(CreateReleasePropertyNames.PACKAGE_VERSION, "");

    List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertNotNull(invalidProperties);
    assertEquals(2, invalidProperties.size());
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
