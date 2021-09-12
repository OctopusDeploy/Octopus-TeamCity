package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.OverwriteMode;
import octopus.teamcity.common.buildinfo.BuildInfoKeys;

public class BuildInformationSubStepType extends SubStepType {

  private final BuildInfoKeys KEYS = new BuildInfoKeys();

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

    final String packageId = properties.getOrDefault(KEYS.getPackageIdKey(), "");
    if (packageId.isEmpty()) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackageIdKey(), "Package IDs must be specified, and cannot be whitespace."));
    }

    final String packageVersion = properties.getOrDefault(KEYS.getPackageVersionKey(), "");
    if (packageVersion.isEmpty()) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackageVersionKey(),
              "Package Version must be specified, and cannot be whitespace"));
    }

    validateOverwriteMode(properties, KEYS.getOverwriteModeKey()).ifPresent(failedProperties::add);

    return failedProperties;
  }

  @Override
  public String describeParameters(final Map<String, String> parameters) {
    final String packageId = parameters.get(KEYS.getPackageIdKey());
    final String packageVersion = parameters.get(KEYS.getPackageVersionKey());
    final OverwriteMode overWrite =
        OverwriteMode.valueOf(parameters.get(KEYS.getOverwriteModeKey()));
    final StringBuilder builder = new StringBuilder();
    builder.append("Package Ids: ");
    builder.append(packageId.replace("\n", ","));
    builder.append("\n");
    builder.append("Version: ");
    builder.append(packageVersion);
    builder.append("\n");
    builder.append("Overwrite Mode: ");
    builder.append(overWrite.getHumanReadable());
    return builder.toString();
  }
}
