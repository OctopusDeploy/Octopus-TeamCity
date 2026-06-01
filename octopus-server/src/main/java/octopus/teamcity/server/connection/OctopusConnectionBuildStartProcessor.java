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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunnerContext;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

public class OctopusConnectionBuildStartProcessor implements BuildStartContextProcessor {
  private static final OctopusConstants C = new OctopusConstants();
  private static final ConnectionPropertyNames CONN = new ConnectionPropertyNames();
  // Runner types that carry Octopus server credentials and therefore support connections.
  // Deliberately excludes PACK_PACKAGE (local packaging only, no server connection) and
  // GENERIC (not a registered run type on this branch).
  private static final Set<String> OCTOPUS_RUNNER_TYPES =
      new HashSet<>(
          Arrays.asList(
              OctopusConstants.CREATE_RELEASE_RUNNER_TYPE,
              OctopusConstants.DEPLOY_RELEASE_RUNNER_TYPE,
              OctopusConstants.PROMOTE_RELEASE_RUNNER_TYPE,
              OctopusConstants.PUSH_PACKAGE_RUNNER_TYPE,
              OctopusConstants.METADATA_RUNNER_TYPE));

  private final Logger logger = Loggers.SERVER;
  private final OctopusConnectionsManager connectionsManager;

  public OctopusConnectionBuildStartProcessor(
      final ExtensionHolder extensionHolder, final OctopusConnectionsManager connectionsManager) {
    this.connectionsManager = connectionsManager;
    extensionHolder.registerExtension(
        BuildStartContextProcessor.class, this.getClass().getName(), this);
  }

  @Override
  public void updateParameters(final BuildStartContext buildStartContext) {
    final SBuildType buildType = buildStartContext.getBuild().getBuildType();
    final SProject project = buildType == null ? null : buildType.getProject();
    if (project == null) {
      return;
    }

    for (final SRunnerContext runner : buildStartContext.getRunnerContexts()) {
      if (!OCTOPUS_RUNNER_TYPES.contains(runner.getType())) {
        continue;
      }
      final Map<String, String> stepParams = runner.getParameters();
      final String connectionId = stepParams.get(C.getConnectionIdKey());
      if (StringUtil.isEmptyOrSpaces(connectionId)) {
        continue;
      }

      final Optional<OAuthConnectionDescriptor> resolved =
          connectionsManager.resolve(project, connectionId);
      if (!resolved.isPresent()) {
        logger.warn(
            "Octopus connection '"
                + connectionId
                + "' referenced by build step could not be resolved");
        continue;
      }

      final Map<String, String> connParams = resolved.get().getParameters();
      setIfPresent(runner, C.getServerKey(), connParams.get(CONN.getServerUrlPropertyName()));
      setIfPresent(runner, C.getApiKey(), connParams.get(CONN.getApiKeyPropertyName()));
      setIfPresent(runner, C.getOctopusVersion(), connParams.get(CONN.getVersionPropertyName()));

      // Space precedence: keep the step's value if it set one; otherwise use the connection's.
      if (StringUtil.isEmptyOrSpaces(stepParams.get(C.getSpaceName()))) {
        setIfPresent(runner, C.getSpaceName(), connParams.get(CONN.getSpaceNamePropertyName()));
      }
    }
  }

  private void setIfPresent(final SRunnerContext runner, final String key, final String value) {
    if (value != null) {
      runner.addRunnerParameter(key, value);
    }
  }
}
