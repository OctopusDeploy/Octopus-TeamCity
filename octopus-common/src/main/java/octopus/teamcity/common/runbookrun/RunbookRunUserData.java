package octopus.teamcity.common.runbookrun;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import octopus.teamcity.common.BaseUserData;

public class RunbookRunUserData extends BaseUserData {

  private static final RunbookRunPropertyNames KEYS = new RunbookRunPropertyNames();

  public RunbookRunUserData(Map<String, String> params) {
    super(params);
  }

  public String getRunbookName() {
    return fetchRaw(KEYS.getRunbookNamePropertyName());
  }

  public String getProjectName() {
    return fetchRaw(KEYS.getProjectNamePropertyName());
  }

  public List<String> getEnvironmentNames() {
    return fetchRawFromNewlineDelimited(KEYS.getEnvironmentNamesPropertyName());
  }

  public Optional<String> getSnapshotName() {
    return Optional.ofNullable(fetchRaw(KEYS.getSnapshotNamePropertyName()));
  }
}
