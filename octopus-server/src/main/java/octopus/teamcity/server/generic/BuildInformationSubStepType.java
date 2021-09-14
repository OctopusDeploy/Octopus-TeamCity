package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.buildinfo.BuildInfoPropertyNames;
import octopus.teamcity.common.buildinfo.BuildInfoUserData;

public class BuildInformationSubStepType extends SubStepType {

  private final BuildInfoPropertyNames KEYS = new BuildInfoPropertyNames();

  public BuildInformationSubStepType() {
    super(
        "build-information",
        "Create Build Information entry",
        "editBuildInformationParameters.jsp",
        "viewBuildInformationParameters.jsp");
  }

  @Override
  public List<InvalidProperty> validateProperties(final Map<String, String> properties) {
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

    final StringBuilder builder = new StringBuilder();
    builder.append("Package Ids: ");
    builder.append(String.join(",", userData.getPackageIds()));
    builder.append("\n");
    builder.append("Version: ");
    builder.append(userData.getPackageVersion());
    builder.append("\n");
    builder.append("Overwrite Mode: ");
    builder.append(userData.getOverwriteMode().getHumanReadable());
    return builder.toString();
  }
}
