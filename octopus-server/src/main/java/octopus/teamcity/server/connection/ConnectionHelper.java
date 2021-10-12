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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;

public class ConnectionHelper {

  public static Map<String, OAuthConnectionDescriptor> getAvailableOctopusConnections(
      final OAuthConnectionsManager oauthConnectionManager,
      final ProjectManager projectManager,
      final User user) {
    return projectManager.getProjects().stream()
        .filter(p -> user.isPermissionGrantedForProject(p.getProjectId(), Permission.VIEW_PROJECT))
        .map(p -> oauthConnectionManager.getAvailableConnectionsOfType(p, OctopusConnection.TYPE))
        .flatMap(Collection::stream)
        .filter(distinctByKey(OAuthConnectionDescriptor::getId))
        .collect(Collectors.toMap(OAuthConnectionDescriptor::getId, Function.identity()));
  }

  private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
