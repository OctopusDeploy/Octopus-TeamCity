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
package octopus.teamcity.server.spaces;

/**
 * Whether this server has TeamCity's connection credentials API, which exists from TeamCity
 * 2023.05. This class must not reference the credentials API; loading a class that does fails on an
 * older server, before the check can run.
 */
final class ConnectionCredentialsApi {

  static final String UNSUPPORTED_MESSAGE =
      "This TeamCity server cannot issue OIDC tokens outside a build. Enter the space name"
          + " instead.";

  private static final String CREDENTIALS_MANAGER_CLASS =
      "jetbrains.buildServer.serverSide.connections.credentials.ProjectConnectionCredentialsManager";

  private ConnectionCredentialsApi() {}

  static boolean isPresent() {
    try {
      Class.forName(
          CREDENTIALS_MANAGER_CLASS, false, ConnectionCredentialsApi.class.getClassLoader());
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }
}
