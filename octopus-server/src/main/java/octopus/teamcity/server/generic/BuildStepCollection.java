package octopus.teamcity.server.generic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import octopus.teamcity.common.OverwriteMode;

public class BuildStepCollection implements Serializable {

  private final Map<String, OctopusBuildStep> octopusBuildSteps =
      Stream.of(
              new BuildInformationStep(),
              new PushPackageStep(),
              new CreateReleaseStep(),
              new RunbookRunStep())
          .collect(Collectors.toMap(OctopusBuildStep::getName, Function.identity()));

  public BuildStepCollection() {}

  public Collection<OctopusBuildStep> getSubSteps() {
    return octopusBuildSteps.values();
  }

  public Optional<OctopusBuildStep> getStepTypeByName(final String name) {
    return Optional.ofNullable(octopusBuildSteps.get(name));
  }

  public Map<String, String> getOverwriteModes() {
    return Stream.of(OverwriteMode.values())
        .collect(Collectors.toMap(Enum<OverwriteMode>::toString, OverwriteMode::getHumanReadable));
  }
}
