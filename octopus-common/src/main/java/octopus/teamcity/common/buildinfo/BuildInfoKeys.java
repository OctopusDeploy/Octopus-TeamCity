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

public class BuildInfoKeys {

  // Note: key strings must be unique across all operations (thus the "bi" prefix)
  public enum Keys {
    PACKAGE_IDS("octopus_bi_packageid"),
    PACKAGE_VERSION("octopus_bi_packageversion"),
    OVERWRITE_MODE("octopus_bi_overwritemode");

    private final String keyString;

    Keys(final String keyString) {
      this.keyString = keyString;
    }

    public String getKeyString() {
      return keyString;
    }
  }

  public BuildInfoKeys() {}

  public String getPackageIdKey() {
    return Keys.PACKAGE_IDS.keyString;
  }

  public String getPackageVersionKey() {
    return Keys.PACKAGE_VERSION.keyString;
  }

  public String getOverwriteModeKey() {
    return Keys.OVERWRITE_MODE.keyString;
  }
}
