package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.runbookrun.RunbookRunPropertyNames;
import octopus.teamcity.common.runbookrun.RunbookRunUserData;

public class RunbookRunStep extends OctopusBuildStep {

  private final RunbookRunPropertyNames KEYS = new RunbookRunPropertyNames();

  public RunbookRunStep() {
    super(
        "runbook-run",
        "Run runbook",
        "editRunbookRunParameters.jsp",
        "viewRunbookRunParameters.jsp");
  }

  @Override
  public String describeParameters(Map<String, String> parameters) {
    final RunbookRunUserData runbookRunUserData = new RunbookRunUserData(parameters);
    return String.format(
        "Runbook name: %s\nProject name: %s",
        runbookRunUserData.getRunbookName(), runbookRunUserData.getProjectName());
  }

  @Override
  public List<InvalidProperty> validateProperties(Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    if (StringUtil.isEmpty(properties.get(KEYS.getRunbookNamePropertyName()))) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getRunbookNamePropertyName(),
              "Runbook name must be specified and cannot be whitespace."));
    }

    if (StringUtil.isEmpty(properties.get(KEYS.getProjectNamePropertyName()))) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getProjectNamePropertyName(),
              "Project name must be specified and cannot be whitespace."));
    }

    final String environmentIdentifiers = properties.get(KEYS.getEnvironmentNamesPropertyName());
    if (StringUtil.isEmpty(environmentIdentifiers)) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getEnvironmentNamesPropertyName(),
              "At least one environment name must be specified."));
    } else {
      List<String> environmentNames = StringUtil.split(environmentIdentifiers, "\n");
      for (String identifier : environmentNames) {
        if (StringUtil.isEmpty(identifier.trim())) {
          failedProperties.add(
              new InvalidProperty(
                  KEYS.getEnvironmentNamesPropertyName(),
                  "An environment name cannot be whitespace."));
          break;
        }
      }
    }

    return failedProperties;
  }
}
