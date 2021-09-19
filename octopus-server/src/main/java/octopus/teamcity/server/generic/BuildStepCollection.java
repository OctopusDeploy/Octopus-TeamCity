package octopus.teamcity.server.generic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import octopus.teamcity.common.OverwriteMode;

public class BuildStepCollection implements Serializable {

  private final List<OctopusBuildStep> octopusBuildSteps =
      Stream.of(new BuildInformationStep(), new PushPackageStep()).collect(Collectors.toList());

  public BuildStepCollection() {}

  public List<OctopusBuildStep> getSubSteps() {
    return octopusBuildSteps;
  }

  public Map<String, String> getOverwriteModes() {
    return Stream.of(OverwriteMode.values())
        .collect(Collectors.toMap(Enum<OverwriteMode>::toString, OverwriteMode::getHumanReadable));
  }
}
