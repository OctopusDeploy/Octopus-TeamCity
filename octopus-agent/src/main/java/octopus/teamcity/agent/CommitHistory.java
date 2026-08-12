/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package octopus.teamcity.agent;

import java.util.List;

import octopus.teamcity.common.Commit;

public class CommitHistory {

  private final List<Commit> commits;
  private final String incompleteDataWarning;

  public CommitHistory(final List<Commit> commits, final String incompleteDataWarning) {
    this.commits = commits;
    this.incompleteDataWarning = incompleteDataWarning;
  }

  public List<Commit> getCommits() {
    return commits;
  }

  public String getIncompleteDataWarning() {
    return incompleteDataWarning;
  }
}
