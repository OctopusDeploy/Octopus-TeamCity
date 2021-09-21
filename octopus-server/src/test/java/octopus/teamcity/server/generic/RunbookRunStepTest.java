package octopus.teamcity.server.generic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.runbookrun.RunbookRunPropertyNames;
import org.junit.jupiter.api.Test;

class RunbookRunStepTest {

  private final RunbookRunStep step = new RunbookRunStep();

  @Test
  public void describePropertiesProducesExpectedOutput() {
    final String message = step.describeParameters(buildPropertiesMap());
    assertThat("Runbook name: RunbookName\nProject name: ProjectName").isEqualTo(message);
  }

  @Test
  public void validatePropertiesReturnsEmptyInvalidPropertiesListWithValidProperties() {
    final List<InvalidProperty> invalidProperties = step.validateProperties(buildPropertiesMap());
    assertThat(invalidProperties).isNotNull().hasSize(0);
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullRunbookName() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.RUNBOOK_NAME, null);

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.RUNBOOK_NAME,
            "Runbook name must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyRunbookName() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.RUNBOOK_NAME, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.RUNBOOK_NAME,
            "Runbook name must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullProjectName() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.PROJECT_NAME, null);

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.PROJECT_NAME,
            "Project name must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyProjectName() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.PROJECT_NAME, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.PROJECT_NAME,
            "Project name must be specified and cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyNullEnvironmentNames() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.ENVIRONMENT_NAMES, null);

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.ENVIRONMENT_NAMES,
            "At least one environment name must be specified.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEmptyEnvironmentNames() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.ENVIRONMENT_NAMES, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.ENVIRONMENT_NAMES,
            "At least one environment name must be specified.");
  }

  @Test
  public void validatePropertiesReturnsSingleInvalidPropertyOnEnvironmentsWithWhitespaceNames() {
    Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.ENVIRONMENT_NAMES, "env1\n \nenv3");

    List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(1)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.ENVIRONMENT_NAMES, "An environment name cannot be whitespace.");
  }

  @Test
  public void validatePropertiesReturnsMultipleInvalidProperty() {
    final Map<String, String> properties = buildPropertiesMap();
    properties.put(RunbookRunPropertyNames.RUNBOOK_NAME, "");
    properties.put(RunbookRunPropertyNames.PROJECT_NAME, "");
    properties.put(RunbookRunPropertyNames.ENVIRONMENT_NAMES, "");

    final List<InvalidProperty> invalidProperties = step.validateProperties(properties);
    assertThat(invalidProperties)
        .isNotNull()
        .hasSize(3)
        .flatExtracting(InvalidProperty::getPropertyName, InvalidProperty::getInvalidReason)
        .containsExactly(
            RunbookRunPropertyNames.RUNBOOK_NAME,
            "Runbook name must be specified and cannot be whitespace.",
            RunbookRunPropertyNames.PROJECT_NAME,
            "Project name must be specified and cannot be whitespace.",
            RunbookRunPropertyNames.ENVIRONMENT_NAMES,
            "At least one environment name must be specified.");
  }

  private Map<String, String> buildPropertiesMap() {
    final Map<String, String> validMap = new HashMap<>();
    // Mandatory/validated
    validMap.put(RunbookRunPropertyNames.RUNBOOK_NAME, "RunbookName");
    validMap.put(RunbookRunPropertyNames.PROJECT_NAME, "ProjectName");
    validMap.put(RunbookRunPropertyNames.ENVIRONMENT_NAMES, "Env1\nEnv2");
    // Optional/un-validated
    validMap.put(RunbookRunPropertyNames.SNAPSHOT_NAME, "Snap-1");
    return validMap;
  }
}
