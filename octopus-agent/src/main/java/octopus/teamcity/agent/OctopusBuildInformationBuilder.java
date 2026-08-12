package octopus.teamcity.agent;

public class OctopusBuildInformationBuilder {

  public OctopusBuildInformation build(
      final String vcsType,
      final String vcsRoot,
      final String vcsCommitNumber,
      final String branch,
      final CommitHistory commitHistory,
      final String externalBuildUrl,
      final String buildNumber) {

    final OctopusBuildInformation buildInformation = new OctopusBuildInformation();

    buildInformation.Commits = commitHistory.getCommits();
    buildInformation.IncompleteDataWarning = commitHistory.getIncompleteDataWarning();
    buildInformation.Branch = branch;
    buildInformation.BuildNumber = buildNumber;
    buildInformation.BuildUrl = externalBuildUrl;
    buildInformation.VcsType = vcsType;
    buildInformation.VcsRoot = vcsRoot;
    buildInformation.VcsCommitNumber = vcsCommitNumber;

    return buildInformation;
  }
}
