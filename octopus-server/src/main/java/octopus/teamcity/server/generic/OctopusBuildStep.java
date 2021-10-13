package octopus.teamcity.server.generic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.OverwriteMode;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;

public abstract class OctopusBuildStep implements Serializable {

  private static final CommonStepPropertyNames KEYS = new CommonStepPropertyNames();

  private final String name;
  private final String description;
  private final String editPage;
  private final String viewPage;

  public OctopusBuildStep() {
    name = "unset";
    description = "unset";
    editPage = "unset";
    viewPage = "unset";
  }

  public OctopusBuildStep(
      final String name, final String description, final String editPage, final String viewPage) {
    this.name = name;
    this.description = description;
    this.editPage = editPage;
    this.viewPage = viewPage;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getEditPage() {
    return editPage;
  }

  public String getViewPage() {
    return viewPage;
  }

  public abstract String describeParameters(final Map<String, String> parameters);

  public List<InvalidProperty> validateProperties(final Map<String, String> properties) {
    final List<InvalidProperty> failedProperties = Lists.newArrayList();

    final String spaceName = properties.getOrDefault(KEYS.getSpaceNamePropertyName(), "");
    if (StringUtil.isEmpty(spaceName)) {
      failedProperties.add(
          new InvalidProperty(
              KEYS.getSpaceNamePropertyName(),
              "Space name must be specified, and cannot be whitespace."));
    }

    failedProperties.addAll(validateBuildSpecificProperties(properties));

    return failedProperties;
  }

  protected abstract List<InvalidProperty> validateBuildSpecificProperties(
      final Map<String, String> properties);

  protected Optional<InvalidProperty> validateOverwriteMode(
      final Map<String, String> properties, final String key) {
    final String overwriteModeStr = properties.get(key);
    if (overwriteModeStr == null) {
      return Optional.of(new InvalidProperty(key, "Overwrite mode must be specified."));
    } else {
      try {
        OverwriteMode.fromString(overwriteModeStr);
      } catch (final IllegalArgumentException e) {
        return Optional.of(
            new InvalidProperty(
                key,
                "OverwriteMode does not contain a recognised a valid value ("
                    + OverwriteMode.validEntriesString()
                    + ")"));
      }
    }
    return Optional.empty();
  }
}
