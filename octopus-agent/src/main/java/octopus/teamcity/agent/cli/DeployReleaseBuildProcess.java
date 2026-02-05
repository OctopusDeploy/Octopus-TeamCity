package octopus.teamcity.agent.cli;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static octopus.teamcity.agent.cli.CommandUtils.getServerTaskId;

public class DeployReleaseBuildProcess extends CLIBuildProcess {
    private String serverTaskId;

    public DeployReleaseBuildProcess(@NotNull AgentRunningBuild runningBuild, @NotNull BuildRunnerContext context) {
        super(runningBuild, context);
    }

    @Override
    public void processOutput(String output, int exitCode) {
        logger.message("Exit code: " + exitCode);
        // update release number when not define in create command as it is required for deployment command
        if (exitCode == 0) {
            if (CommandUtils.isDeployRelease(output)) {
                serverTaskId = getServerTaskId(output);
            }
        }
    }

    @Override
    protected List<OctopusCommandBuilder> createCommand() {
        final OctopusConstants constants = OctopusConstants.Instance;
        List<OctopusCommandBuilder> commands = new ArrayList<>();
        final Map<String, String> parameters = getContext().getRunnerParameters();
        final boolean wait = Boolean.parseBoolean(parameters.get(constants.getWaitForDeployments()));

        commands.add(CommandHelper.login(parameters));

        commands.add(new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                return CommandHelper.deploy(parameters, null);
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
        return commands;
    }

    @Override
    protected String getLogMessage() {
        return "Deploying Octopus Deploy release";
    }
}