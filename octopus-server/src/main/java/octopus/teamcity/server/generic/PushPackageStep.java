package octopus.teamcity.server.generic;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.pushpackage.PushPackagePropertyNames;

public class PushPackageStep extends OctopusBuildStep {

  private final PushPackagePropertyNames KEYS = new PushPackagePropertyNames();

  public PushPackageStep() {
    super(
        "push-package",
        "Push binary package",
        "editPushPackageParameters.jsp",
        "viewPushPackageParameters.jsp");
  }

  @Override
  public List<InvalidProperty> validateProperties(final Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    final String packagePaths = properties.getOrDefault(KEYS.getPackagePathsPropertyName(), "");
    if (packagePaths.isEmpty()) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getPackagePathsPropertyName(),
              "Package Paths must be specified, and cannot be whitespace."));
    }

    validateOverwriteMode(properties, KEYS.getOverwriteModePropertyName())
        .ifPresent(failedProperties::add);

    return failedProperties;
  }

  @Override
  public String describeParameters(final Map<String, String> parameters) {
    final String packagePaths = parameters.get(KEYS.getPackagePathsPropertyName());
    return "Packages: " + packagePaths.replace("\n", ", ");
  }
}
