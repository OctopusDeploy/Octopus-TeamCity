package octopus.teamcity.common.runbookrun;

public class RunbookRunPropertyNames {

  public static final String RUNBOOK_NAME = "octopus_rr_runbook_name";
  public static final String PROJECT_NAME = "octopus_rr_project_name";
  public static final String ENVIRONMENT_NAMES = "octopus_rr_environment_names";
  public static final String SNAPSHOT_NAME = "octopus_rr_snapshot_name";

  public String getRunbookNamePropertyName() {
    return RUNBOOK_NAME;
  }

  public String getProjectNamePropertyName() {
    return PROJECT_NAME;
  }

  public String getEnvironmentNamesPropertyName() {
    return ENVIRONMENT_NAMES;
  }

  public String getSnapshotNamePropertyName() {
    return SNAPSHOT_NAME;
  }
}
