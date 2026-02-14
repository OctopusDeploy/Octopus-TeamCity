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

package octopus.teamcity.agent.cli;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusBuildInformation;
import octopus.teamcity.agent.OctopusBuildInformationBuilder;
import octopus.teamcity.agent.OctopusBuildInformationWriter;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.BuildId;
import org.jetbrains.teamcity.rest.TeamCityInstance;
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static octopus.teamcity.agent.BuildInfoUtils.createJsonCommitHistory;

public class BuildInformationBuildProcess extends CLIBuildProcess {

    private final File checkoutDir;
    private final Map<String, String> sharedConfigParameters;
    private final String teamCityServerUrl;

    public BuildInformationBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
        super(runningBuild, context);

        checkoutDir = runningBuild.getCheckoutDirectory();
        sharedConfigParameters = runningBuild.getSharedConfigParameters();
        teamCityServerUrl = runningBuild.getAgentConfiguration().getServerUrl();
    }

    @Override
    public void processOutput(String output, int exitCode) {
        logger.message("Exit code: " + exitCode);
    }

    @Override
    protected String getLogMessage() {
        return "Pushing build information to Octopus server";
    }

    @Override
    protected List<OctopusCommandBuilder> createCommand() {
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;
        final Boolean verboseLogging = Boolean.parseBoolean(parameters.get(constants.getVerboseLoggingKey()));
        final String dataFile = Paths.get(checkoutDir.getPath(), "octopus.buildinfo").toAbsolutePath().toString();

        try {
            AgentRunningBuild build = getContext().getBuild();

            final OctopusBuildInformationBuilder builder = new OctopusBuildInformationBuilder();

            final String buildIdString = Long.toString(build.getBuildId());
            final TeamCityInstance teamCityServer = TeamCityInstanceFactory.httpAuth(teamCityServerUrl, build.getAccessUser(), build.getAccessCode());
            final Build restfulBuild = teamCityServer.build(new BuildId(buildIdString));

            final String buildNumber = restfulBuild.getBuildNumber();
            String buildUrlString = sharedConfigParameters.get("externalBuildUrl");
            if (buildUrlString == null) {
                // if the Global settings don't have a Server URL then fall back to using the agent's
                // configuration for the server's URL
                buildUrlString = teamCityServerUrl + "/viewLog.html?buildId=" + buildNumber;
            }

            final OctopusBuildInformation buildInformation = builder.build(sharedConfigParameters.get("octopus_vcstype"),
                    sharedConfigParameters.get("vcsroot.url"),
                    sharedConfigParameters.get("build.vcs.number"),
                    restfulBuild.getBranch().getName(),
                    createJsonCommitHistory(restfulBuild), buildUrlString, buildNumber);

            if (verboseLogging) {
                logger.message("Creating " + dataFile);
            }

            final OctopusBuildInformationWriter writer = new OctopusBuildInformationWriter(logger, verboseLogging);
            writer.writeToFile(buildInformation, dataFile, StandardCharsets.UTF_8);

        } catch (Exception ex) {
            logger.error("Error processing comment messages " + ex);
            return null;
        }
        List<OctopusCommandBuilder> commands = new ArrayList<>();

        commands.add(CommandHelper.login(parameters));
        commands.add(CommandHelper.buildInformation(parameters, verboseLogging, dataFile, logger));
        return commands;
    }
}
