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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import octopus.teamcity.common.Commit;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.Change;

public class BuildInfoUtils {
  public static String createJsonCommitHistory(final Build build) {
    final List<Change> changes = build.fetchChanges();

    final List<Commit> commits = new ArrayList<>();
    for (Change change : changes) {

      final Commit c = new Commit();
      c.Id = change.getVersion();
      c.Comment = change.getComment();

      commits.add(c);
    }

    final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    return gson.toJson(commits);
  }
}
