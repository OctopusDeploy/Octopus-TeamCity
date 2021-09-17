package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.OverwriteMode;
import octopus.teamcity.common.pushpackage.PushPackageKeys;
import org.apache.commons.compress.utils.Lists;

public class PushPackageSubStepType extends OctopusBuildStep {

  private final PushPackageKeys KEYS = new PushPackageKeys();

  public PushPackageSubStepType() {
    super(
        "push-package",
        "Push binary package",
        "editPushPackageParameters.jsp",
        "viewPushPackageParameters.jsp");
  }

  @Override
  public List<InvalidProperty> validateProperties(final Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    final String packagePaths = properties.getOrDefault(KEYS.getPackagePathsKey(), "");
    if (packagePaths.isEmpty()) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackagePathsKey(),
              "Package Paths must be specified, and cannot be whitespace."));
    }

    validateOverwriteMode(properties, KEYS.getOverwriteModeKey()).ifPresent(failedProperties::add);

    return failedProperties;
  }

  @Override
  public String describeParameters(final Map<String, String> parameters) {
    final String packagePaths = parameters.get(KEYS.getPackagePathsKey());
    final OverwriteMode overWrite =
        OverwriteMode.valueOf(parameters.get(KEYS.getOverwriteModeKey()));
    final StringBuilder builder = new StringBuilder();
    builder.append("Packages: ");
    builder.append(packagePaths.replace("\n", ", "));
    builder.append("\n");
    builder.append("Overwrite Mode: ");
    builder.append(overWrite.getHumanReadable());
    return builder.toString();
  }
}
