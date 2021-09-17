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

package octopus.teamcity.common.pushpackage;

import java.util.Map;

import octopus.teamcity.common.OverwriteMode;

/**
 * Not SURE if these classes should return a TYPED version of the data ... or just a String. ATM -
 * only used on agent side to decode paramsMap ... so it can probably be typed. *
 */
public class PushPackageUserData {

  private static final PushPackageKeys KEYS = new PushPackageKeys();
  private final Map<String, String> params;

  public PushPackageUserData(final Map<String, String> params) {
    this.params = params;
  }

  public String getPackagePaths() {
    return params.get(KEYS.getPackagePathsKey());
  }

  public OverwriteMode getOverwriteMode() {
    return OverwriteMode.fromString(params.get(KEYS.getOverwriteModeKey()));
  }

  public String getPublishArtifacts() {
    return params.get(KEYS.getPublishArtifactsKey());
  }
}
