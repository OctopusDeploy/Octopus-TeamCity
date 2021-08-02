/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.agent;

import java.util.Map;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

public class OctopusBuildListener extends AgentLifeCycleAdapter {
  public OctopusBuildListener(
      @NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
    agentDispatcher.addListener(this);
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    Map<String, String> params = runner.getRunnerParameters();

    BuildProgressLogger logger = runner.getBuild().getBuildLogger();

    OctopusConstants c = new OctopusConstants();

    String runOctoPackString = params.get(c.getRunOctoPack());
    String octoPackVersion = params.get(c.getOctoPackPackageVersion());
    String publishUrl = params.get(c.getOctoPackPublishPackageToHttp());
    String publishApiKey = params.get(c.getOctoPackPublishApiKey());
    String appendToPackageId = params.get(c.getOctoPackAppendToPackageId());

    if (StringUtil.isEmptyOrSpaces(runOctoPackString)) {
      return;
    }

    logger.message("Enabling OctoPack");
    runner.addSystemProperty("RunOctoPack", "true");
    runner.addSystemProperty("OctoPackPackageVersion", octoPackVersion);

    if (!StringUtil.isEmptyOrSpaces(appendToPackageId)) {
      runner.addSystemProperty("OctoPackAppendToPackageId", appendToPackageId);
    }

    if (!StringUtil.isEmptyOrSpaces(publishUrl)) {
      runner.addSystemProperty("OctoPackPublishPackageToHttp", publishUrl);
      runner.addSystemProperty("OctoPackPublishApiKey", publishApiKey);
    }
  }
}
