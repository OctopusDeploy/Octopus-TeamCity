/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.agent.buildinformation;

import com.octopus.sdk.operation.buildinformation.Commit;

import java.util.ArrayList;
import java.util.List;

import jetbrains.buildServer.agent.AgentRunningBuild;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.BuildId;
import org.jetbrains.teamcity.rest.Change;
import org.jetbrains.teamcity.rest.TeamCityInstance;
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory;

public class BuildVcsData implements BaseBuildVcsData {

  private final Build buildDataFetcher;

  public BuildVcsData(final Build buildDataFetcher) {
    this.buildDataFetcher = buildDataFetcher;
  }

  public static BuildVcsData create(final AgentRunningBuild runningBuild) {
    final String teamCityServerUrl = runningBuild.getAgentConfiguration().getServerUrl();
    final TeamCityInstance teamCityServer =
        TeamCityInstanceFactory.httpAuth(
            teamCityServerUrl, runningBuild.getAccessUser(), runningBuild.getAccessCode());

    final String buildIdString = Long.toString(runningBuild.getBuildId());
    final Build buildDataFetcher = teamCityServer.build(new BuildId(buildIdString));

    return new BuildVcsData(buildDataFetcher);
  }

  @Override
  public List<Commit> getCommits() {
    final List<Change> changes = buildDataFetcher.fetchChanges();

    final List<Commit> commits = new ArrayList<>();
    for (Change change : changes) {
      commits.add(new Commit(change.getVersion(), change.getComment()));
    }

    return commits;
  }

  @Override
  public String getBranchName() {
    return buildDataFetcher.getBranch().getName();
  }
}
