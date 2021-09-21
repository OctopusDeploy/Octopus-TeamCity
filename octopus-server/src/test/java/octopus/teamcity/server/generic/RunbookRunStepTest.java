package octopus.teamcity.server.generic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.runbookrun.RunbookRunPropertyNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
    properties.remove(RunbookRunPropertyNames.RUNBOOK_NAME);
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

  @SuppressWarnings("unused")
  private static Stream<Arguments> provideNullAndEmptyTestArguments() {
    return Stream.of(
        Arguments.of(
            RunbookRunPropertyNames.RUNBOOK_NAME,
            null,
            "Runbook name must be specified and cannot be whitespace."),
        Arguments.of(
            RunbookRunPropertyNames.RUNBOOK_NAME,
            "",
            "Runbook name must be specified and cannot be whitespace."),
        Arguments.of(
            RunbookRunPropertyNames.PROJECT_NAME,
            null,
            "Project name must be specified and cannot be whitespace."),
        Arguments.of(
            RunbookRunPropertyNames.PROJECT_NAME,
            "",
            "Project name must be specified and cannot be whitespace."),
        Arguments.of(
            RunbookRunPropertyNames.ENVIRONMENT_NAMES,
            null,
            "At least one environment name must be specified."),
        Arguments.of(
            RunbookRunPropertyNames.ENVIRONMENT_NAMES,
            "",
            "At least one environment name must be specified."),
        Arguments.of(
            RunbookRunPropertyNames.ENVIRONMENT_NAMES,
            "env1\n \nenv3",
            "An environment name cannot be whitespace."));
  }
}
