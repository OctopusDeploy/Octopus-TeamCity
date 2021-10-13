package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import octopus.teamcity.common.buildinfo.BuildInfoUserData;

public class BuildInformationStep extends OctopusBuildStep {

  private final BuildInfoPropertyNames KEYS = new BuildInfoPropertyNames();

  public BuildInformationStep() {
    super(
        "build-information",
        "Create Build Information entry",
        "editBuildInformationParameters.jsp",
        "viewBuildInformationParameters.jsp");
  }

  @Override
  protected List<InvalidProperty> validateBuildSpecificProperties(
      final Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    final String packageId = properties.getOrDefault(KEYS.getPackageIdPropertyName(), "");
    if (packageId.isEmpty()) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackageIdPropertyName(),
              "Package IDs must be specified, and cannot be whitespace."));
    }

    final String packageVersion = properties.getOrDefault(KEYS.getPackageVersionPropertyName(), "");
    if (packageVersion.isEmpty()) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackageVersionPropertyName(),
              "Package Version must be specified, and cannot be whitespace"));
    }

    validateOverwriteMode(properties, KEYS.getOverwriteModePropertyName())
        .ifPresent(failedProperties::add);

    return failedProperties;
  }

  @Override
  public String describeParameters(final Map<String, String> parameters) {
    final BuildInfoUserData userData = new BuildInfoUserData(parameters);
    return "Package Ids: " + String.join(", ", userData.getPackageIds());
  }
}
