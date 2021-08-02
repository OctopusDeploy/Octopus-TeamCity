package octopus.teamcity.agent;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import octopus.teamcity.common.Commit;

public class OctopusBuildInformationBuilder {

  public OctopusBuildInformation build(
      final String vcsType,
      final String vcsRoot,
      final String vcsCommitNumber,
      final String branch,
      final String commitsJson,
      final String serverUrl,
      final String buildId,
      final String buildNumber) {

    final OctopusBuildInformation buildInformation = new OctopusBuildInformation();

    final Gson gson = new GsonBuilder().create();

    buildInformation.Commits =
        gson.fromJson(commitsJson, new TypeToken<List<Commit>>() {}.getType());
    buildInformation.Branch = branch;
    buildInformation.BuildNumber = buildNumber;
    buildInformation.BuildUrl = serverUrl + "/viewLog.html?buildId=" + buildId;
    buildInformation.VcsType = vcsType;
    buildInformation.VcsRoot = vcsRoot;
    buildInformation.VcsCommitNumber = vcsCommitNumber;

    return buildInformation;
  }
}
