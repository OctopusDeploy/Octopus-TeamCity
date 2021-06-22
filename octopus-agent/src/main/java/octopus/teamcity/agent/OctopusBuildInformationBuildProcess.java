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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.Commit;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.OverwriteMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.BuildId;
import org.jetbrains.teamcity.rest.Change;
import org.jetbrains.teamcity.rest.Revision;
import org.jetbrains.teamcity.rest.TeamCityInstance;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OctopusBuildInformationBuildProcess extends OctopusBuildProcess {

    private final File checkoutDir;
    private final Map<String, String> sharedConfigParameters;
    private final String teamCityServerUrl;

    public OctopusBuildInformationBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
        super(runningBuild, context);

        checkoutDir = runningBuild.getCheckoutDirectory();
        sharedConfigParameters = runningBuild.getSharedConfigParameters();
        teamCityServerUrl = runningBuild.getAgentConfiguration().getServerUrl();

    }

    @Override
    protected String getLogMessage() {
        return "Pushing build information to Octopus server";
    }

    @Override
    protected OctopusCommandBuilder createCommand() {

        final BuildProgressLogger buildLogger = getLogger();

        final Map<String, String> parameters = getContext().getRunnerParameters();
        final OctopusConstants constants = OctopusConstants.Instance;
        final Boolean verboseLogging = Boolean.parseBoolean(parameters.get(constants.getVerboseLoggingKey()));

        final String dataFile = Paths.get(checkoutDir.getPath(), "octopus.buildinfo").toAbsolutePath().toString();

        try {
            AgentRunningBuild build = getContext().getBuild();

            final OctopusBuildInformationBuilder builder = new OctopusBuildInformationBuilder();

            final String buildIdString = Long.toString(build.getBuildId());
            final TeamCityInstance teamCityServer = TeamCityInstance.httpAuth(teamCityServerUrl, build.getAccessUser(), build.getAccessCode());
            final Build restfulBuild = teamCityServer.build(new BuildId(buildIdString));

            final Optional<Revision> revision = restfulBuild.fetchRevisions().stream().findFirst();
            final String reportedBranch = restfulBuild.getBranch().getName();
            final String vcsRoot = sharedConfigParameters.get("vcsroot.url");
            final String vcsType = getVcsType(vcsRoot);

            final OctopusBuildInformation buildInformation = builder.build(
                    vcsType,
                    vcsRoot,
                    sharedConfigParameters.get("build.vcs.number"),
                    revision.map(Revision::getVcsBranchName).orElse(reportedBranch != null ? reportedBranch  : ""),
                    createJsonCommitHistory(restfulBuild),
                    teamCityServerUrl,
                    buildIdString,
                    build.getBuildNumber());

            if (verboseLogging) {
                buildLogger.message("Creating " + dataFile);
            }

            final OctopusBuildInformationWriter writer = new OctopusBuildInformationWriter(buildLogger, verboseLogging);
            writer.writeToFile(buildInformation, dataFile);

        } catch (Exception ex) {
            buildLogger.error("Error processing comment messages " + ex);
            return null;
        }

        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String serverUrl = parameters.get(constants.getServerKey());
                final String apiKey = parameters.get(constants.getApiKey());
                final String spaceName = parameters.get(constants.getSpaceName());
                final String packageIds = parameters.get(constants.getPackageIdKey());
                final String packageVersion = parameters.get(constants.getPackageVersionKey());
                final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());

                final String forcePush = parameters.get(constants.getForcePushKey());
                OverwriteMode overwriteMode = OverwriteMode.FailIfExists;
                if ("true".equals(forcePush)) {
                    overwriteMode = OverwriteMode.OverwriteExisting;
                }
                else if (OverwriteMode.IgnoreIfExists.name().equals(forcePush)) {
                    overwriteMode = OverwriteMode.IgnoreIfExists;
                }

                if (verboseLogging) {
                    buildLogger.message("ForcePush: " + forcePush);
                    buildLogger.message("OverwriteMode: " + overwriteMode.name());
                }

                commands.add("build-information");
                commands.add("--server");
                commands.add(serverUrl);
                commands.add("--apikey");
                commands.add(masked ? "SECRET" : apiKey);

                if (spaceName != null && !spaceName.isEmpty()) {
                    commands.add("--space");
                    commands.add(spaceName);
                }

                for(String packageId : StringUtil.split(packageIds, "\n")) {
                    commands.add("--package-id");
                    commands.add(packageId);
                }

                commands.add("--version");
                commands.add(packageVersion);

                commands.add("--file");
                commands.add(dataFile);

                if (overwriteMode != OverwriteMode.FailIfExists) {
                    commands.add("--overwrite-mode");
                    commands.add(overwriteMode.name());
                }

                if (commandLineArguments != null && !commandLineArguments.isEmpty()) {
                    commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
                }

                return commands.toArray(new String[commands.size()]);
            }
        };
    }

    private String createJsonCommitHistory(final Build build) {
        final List<Change> changes = build.fetchChanges();


        final List<Commit> commits = new ArrayList<>();
        for (Change change : changes) {

            final Commit c = new Commit();
            c.Id = change.getVersion();
            c.Comment = change.getComment();

            commits.add(c);
        }

        final Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
        return gson.toJson(commits);
    }

    private String getVcsType(final String vcsRoot) {
        return vcsRoot.startsWith("git") ? "git" : "unknown";
    }
}
