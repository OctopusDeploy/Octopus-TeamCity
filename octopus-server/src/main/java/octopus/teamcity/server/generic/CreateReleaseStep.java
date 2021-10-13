package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.createrelease.CreateReleasePropertyNames;
import octopus.teamcity.common.createrelease.CreateReleaseUserData;

public class CreateReleaseStep extends OctopusBuildStep {

  private final CreateReleasePropertyNames KEYS = new CreateReleasePropertyNames();

  public CreateReleaseStep() {
    super(
        "create-release",
        "Create release",
        "editCreateReleaseParameters.jsp",
        "viewCreateReleaseParameters.jsp");
  }

  @Override
  public String describeParameters(Map<String, String> parameters) {
    final CreateReleaseUserData createReleaseUserData = new CreateReleaseUserData(parameters);
    return String.format(
        "Project name: %s\nPackage version: %s",
        createReleaseUserData.getProjectName(), createReleaseUserData.getPackageVersion());
  }

  @Override
  public List<InvalidProperty> validateProperties(Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    if (StringUtil.isEmpty(properties.getOrDefault(KEYS.getProjectNamePropertyName(), ""))) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getProjectNamePropertyName(),
              "Project name must be specified and cannot be whitespace."));
    }

    if (StringUtil.isEmpty(properties.getOrDefault(KEYS.getPackageVersionPropertyName(), ""))) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackageVersionPropertyName(),
              "Package version must be specified and cannot be whitespace."));
    }

    return failedProperties;
  }
}
