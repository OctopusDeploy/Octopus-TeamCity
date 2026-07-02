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

package octopus.teamcity.server.connection;

import java.util.Map;

import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;

/**
 * Removes a build step's inline Octopus credential parameters when the step references a reusable
 * connection. The connection supplies the server URL, API key, and version; keeping these old
 * inline values leaves stale credentials behind.
 */
public final class ConnectionInlineFieldCleaner {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  private ConnectionInlineFieldCleaner() {}

  public static void stripInlineFieldsIfUsingConnection(final Map<String, String> properties) {
    if (properties == null) {
      return;
    }
    final boolean usingConnection =
        !StringUtil.isEmptyOrSpaces(properties.get(CONSTANTS.getConnectionIdKey()));
    if (!usingConnection) {
      return;
    }
    properties.remove(CONSTANTS.getServerKey());
    properties.remove(CONSTANTS.getApiKey());
    properties.remove(CONSTANTS.getOctopusVersion());
  }
}
