package octopus.teamcity.server.generic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import octopus.teamcity.common.OverwriteMode;

public class SubStepCollection implements Serializable {

  private final List<SubStepType> subStepTypes =
      Stream.of(new BuildInformationSubStepType()).collect(Collectors.toList());

  public SubStepCollection() {}

  public List<SubStepType> getSubSteps() {
    return subStepTypes;
  }

  public Map<String, String> getOverwriteModes() {
    return Stream.of(OverwriteMode.values())
        .collect(Collectors.toMap(Enum::toString, OverwriteMode::getHumanReadable));
  }
}
