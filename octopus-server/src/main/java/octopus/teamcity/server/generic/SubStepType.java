package octopus.teamcity.server.generic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.OverwriteMode;

public abstract class SubStepType implements Serializable {

  private final String name;
  private final String description;
  private final String editPage;
  private final String viewPage;

  public SubStepType() {
    name = "unset";
    description = "unset";
    editPage = "unset";
    viewPage = "unset";
  }

  public SubStepType(
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

  public abstract Collection<InvalidProperty> validateProperties(
      final Map<String, String> properties);

  public abstract String describeParameters(final Map<String, String> parameters);

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
