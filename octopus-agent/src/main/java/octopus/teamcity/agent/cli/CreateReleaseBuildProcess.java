package octopus.teamcity.agent.cli;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static octopus.teamcity.agent.cli.CommandUtils.getServerTaskId;

public class CreateReleaseBuildProcess extends CLIBuildProcess {
    private String autoCreatedReleaseNumber;
    private String serverTaskId;

    public CreateReleaseBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
        super(runningBuild, context);
    }

    @Override
    public void processOutput(String output, int exitCode) {
        logger.message("Exit code: " + exitCode);
        // update release number when not define in create command as it is required for deployment command
        if (exitCode == 0) {
            if (CommandUtils.isCreateReleaseCommand(output)) {
                autoCreatedReleaseNumber = CommandUtils.getReleaseVersion(output);
            } else if (CommandUtils.isDeployRelease(output)) {
                serverTaskId = getServerTaskId(output);
            }
        }
    }

    @Override
    protected List<OctopusCommandBuilder> createCommand() {
        final OctopusConstants constants = OctopusConstants.Instance;
        List<OctopusCommandBuilder> commands = new ArrayList<>();
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final String spaceName = parameters.get(constants.getSpaceName());
        final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());
        final String releaseNumber = parameters.get(constants.getReleaseNumberKey());
        final String channelName = parameters.get(constants.getChannelNameKey());
        final String deployTo = parameters.get(constants.getDeployToKey());
        final String projectName = parameters.get(constants.getProjectNameKey());
        final String gitRef = parameters.get(constants.getGitRefKey());
        final String gitCommit = parameters.get(constants.getGitCommitKey());
        final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

        commands.add(CommandHelper.login(parameters));

        commands.add(new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> createCommands = new ArrayList<>();

                createCommands.add("release");
                createCommands.add("create");

                if (StringUtils.isNotBlank(spaceName)) {
                    createCommands.add("--space");
                    createCommands.add(spaceName);
                }

                createCommands.add("--project");
                createCommands.add(projectName);

                if (StringUtils.isNotBlank(releaseNumber)) {
                    createCommands.add("--version");
                    createCommands.add(releaseNumber);
                }

                if (StringUtils.isNotBlank(channelName)) {
                    createCommands.add("--channel");
                    createCommands.add(channelName);
                }

                if (StringUtils.isNotBlank(gitRef)) {
                    createCommands.add("--git-ref");
                    createCommands.add(gitRef);
                }

                if (StringUtils.isNotBlank(gitCommit)) {
                    createCommands.add("--git-commit");
                    createCommands.add(gitCommit);
                }

                createCommands.add("--output-format");
                createCommands.add("json");

                if (StringUtils.isNotBlank(commandLineArguments)) {
                    createCommands.addAll(splitSpaceSeparatedValues(commandLineArguments));
                }

                createCommands.add("--no-prompt");
                return createCommands.toArray(new String[commands.size()]);
            }
        });

        if (StringUtils.isNotBlank(deployTo)) {
            commands.add(new OctopusCommandBuilder() {
                @Override
                protected String[] buildCommand(boolean masked) {
                    return CommandHelper.deploy(parameters, autoCreatedReleaseNumber);
                }
            });

            if (wait) {
                commands.add(new OctopusCommandBuilder() {
                    @Override
                    protected String[] buildCommand(boolean masked) {
                        return CommandHelper.wait(parameters, serverTaskId);
                    }
                });
            }
        }
        return commands;
    }

    @Override
    protected String getLogMessage() {
        return "Creating Octopus Deploy release";
    }
}