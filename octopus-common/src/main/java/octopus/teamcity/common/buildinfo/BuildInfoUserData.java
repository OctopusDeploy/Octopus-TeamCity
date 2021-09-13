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

package octopus.teamcity.common.buildinfo;

import java.util.List;
import java.util.Map;

import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OverwriteMode;

public class BuildInfoUserData {

  private static final BuildInfoKeys KEYS = new BuildInfoKeys();
  private final Map<String, String> params;

  public BuildInfoUserData(final Map<String, String> params) {
    this.params = params;
  }

  public List<String> getPackageIds() {
    final String rawData = params.get(KEYS.getPackageIdKey());
    return StringUtil.split(rawData, "\n");
  }

  public String getPackageVersion() {
    return params.get(KEYS.getPackageVersionKey());
  }

  public OverwriteMode getOverwriteMode() {
    return OverwriteMode.fromString(params.get(KEYS.getOverwriteModeKey()));
  }
}
