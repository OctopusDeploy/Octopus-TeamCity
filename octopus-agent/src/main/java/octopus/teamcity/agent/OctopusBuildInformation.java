package octopus.teamcity.agent;

import java.util.List;

import octopus.teamcity.common.Commit;

public class OctopusBuildInformation {
  public String BuildEnvironment;
  public String Branch;
  public String BuildNumber;
  public String BuildUrl;
  public String VcsType;
  public String VcsRoot;
  public String VcsCommitNumber;

  public List<Commit> Commits;

  public OctopusBuildInformation() {
    BuildEnvironment = "TeamCity";
  }
}
