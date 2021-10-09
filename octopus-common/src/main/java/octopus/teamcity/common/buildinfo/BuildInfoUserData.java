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
import octopus.teamcity.common.BaseUserData;
import octopus.teamcity.common.OverwriteMode;
import octopus.teamcity.common.commonstep.CommonStepUserData;

public class BuildInfoUserData extends CommonStepUserData {

  private static final BuildInfoPropertyNames KEYS = new BuildInfoPropertyNames();

  public BuildInfoUserData(final Map<String, String> params) {
    super(params);
  }

  public List<String> getPackageIds() {
    final String rawInput = fetchRaw(KEYS.getPackageIdPropertyName());
    return StringUtil.split(rawInput, "\n");
  }

  public String getPackageVersion() {
    return fetchRaw(KEYS.getPackageVersionPropertyName());
  }

  public OverwriteMode getOverwriteMode() {
    final String rawInput = fetchRaw(KEYS.getOverwriteModePropertyName());
    return OverwriteMode.fromString(rawInput);
  }
}
