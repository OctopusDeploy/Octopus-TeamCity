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

public class PushPackageKeys {
  public static final String PACKAGE_PATHS = "octopus_pp_package_paths";
  public static final String OVERWRITE_MODE = "octopus_pp_overwrite_mode";
  public static final String USE_DELTA_COMPRESSION = "octopus_pp_use_delta_compression";
  public static final String PUBLISH_ARTIFACTS = "octopus_pp_publish_artifacts";

  public PushPackageKeys() {}

  public String getPackagePathsKey() {
    return PACKAGE_PATHS;
  }

  public String getOverwriteModeKey() {
    return OVERWRITE_MODE;
  }

  public String getDeltaComparison() {
    return USE_DELTA_COMPRESSION;
  }

  public String getPublishArtifactsKey() {
    return PUBLISH_ARTIFACTS;
  }
}
