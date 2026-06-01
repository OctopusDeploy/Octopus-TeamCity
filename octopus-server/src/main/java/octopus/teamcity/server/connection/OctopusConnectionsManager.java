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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OctopusConnectionsManager {
  private final OAuthConnectionsManager teamcityOAuth;
  private final ProjectManager projectManager;

  public OctopusConnectionsManager(
      final OAuthConnectionsManager teamcityOAuth, final ProjectManager projectManager) {
    this.teamcityOAuth = teamcityOAuth;
    this.projectManager = projectManager;
  }

  @NotNull
  public List<OAuthConnectionDescriptor> listAvailableConnections(@NotNull final User user) {
    return projectManager.getProjects().stream()
        .filter(p -> user.isPermissionGrantedForProject(p.getProjectId(), Permission.VIEW_PROJECT))
        .map(p -> teamcityOAuth.getAvailableConnectionsOfType(p, OctopusConnection.TYPE))
        .flatMap(Collection::stream)
        .filter(distinctByKey(OAuthConnectionDescriptor::getId))
        .collect(Collectors.toList());
  }

  @NotNull
  public Optional<OAuthConnectionDescriptor> resolve(
      @NotNull final SProject project, @Nullable final String connectionId) {
    if (StringUtil.isEmptyOrSpaces(connectionId)) {
      return Optional.empty();
    }
    return Optional.ofNullable(teamcityOAuth.findConnectionById(project, connectionId));
  }

  private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
    final Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
