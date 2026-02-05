package octopus.teamcity.agent.cli;

import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import static octopus.teamcity.agent.OctopusCommandBuilder.splitCommaSeparatedValues;
import static octopus.teamcity.agent.OctopusCommandBuilder.splitSpaceSeparatedValues;

public class CommandHelper {

    public static String[] deploy(Map<String, String> params, String autoCreatedReleaseNumber) {
        final OctopusConstants constants = OctopusConstants.Instance;
        final ArrayList<String> commands = new ArrayList<>();
        final String spaceName = params.get(constants.getSpaceName());
        final String commandLineArguments = params.get(constants.getCommandLineArgumentsKey());
        final String releaseNumber = params.get(constants.getReleaseNumberKey());
        final String deployTo = params.get(constants.getDeployToKey());
        final String tenants = params.get(constants.getTenantsKey());
        final String tenantTags = params.get(constants.getTenantTagsKey());
        final String projectName = params.get(constants.getProjectNameKey());

        commands.add("release");
        commands.add("deploy");

        if (StringUtils.isNotBlank(spaceName)) {
            commands.add("--space");
            commands.add(spaceName);
        }

        commands.add("--project");
        commands.add(projectName);

        String version = CommandUtils.getVersion(releaseNumber, autoCreatedReleaseNumber);
        if (!version.isEmpty()) {
            commands.add("--version");
            commands.add(version);
        }

        for (String env : splitCommaSeparatedValues(deployTo)) {
            commands.add("--environment");
            commands.add(env);
        }

        for (String tenant : splitCommaSeparatedValues(tenants)) {
            commands.add("--tenant");
            commands.add(tenant);
        }

        for (String tenantTag : splitCommaSeparatedValues(tenantTags)) {
            commands.add("--tenant-tag");
            commands.add(tenantTag);
        }

        commands.add("--output-format");
        commands.add("json");

        if (StringUtils.isNotBlank(commandLineArguments)) {
            commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
        }

        commands.add("--no-prompt");
        return commands.toArray(new String[0]);
    }

    public static OctopusCommandBuilder login(Map<String, String> params) {
        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final OctopusConstants constants = OctopusConstants.Instance;
                final ArrayList<String> commands = new ArrayList<>();
                final String serverUrl = params.get(constants.getServerKey());
                final String apiKey = params.get(constants.getApiKey());

                commands.add("login");

                commands.add("--server");
                commands.add(serverUrl);

                commands.add("--api-key");
                commands.add(apiKey);

                commands.add("--no-prompt");
                return commands.toArray(new String[0]);
            }
        };
    }

    public static String[] wait(Map<String, String> params, String taskId) {
        final OctopusConstants constants = OctopusConstants.Instance;
        final ArrayList<String> commands = new ArrayList<>();
        final String deploymentTimeout = params.get(constants.getDeploymentTimeout());
        final String cancelDeploymentTimeout = params.get(constants.getCancelDeploymentOnTimeout());
        final String spaceName = params.get(constants.getSpaceName());
        commands.add("task");
        commands.add("wait");

        commands.add(taskId);
        if (StringUtils.isNotBlank(spaceName)) {
            commands.add("--space");
            commands.add(spaceName);
        }

        commands.add("--progress");

        if (StringUtils.isNotBlank(deploymentTimeout)) {
            commands.add("--timeout");
            commands.add(deploymentTimeout);
        }

        if (StringUtils.isNotBlank(cancelDeploymentTimeout)) {
            commands.add("--cancel-on-timeout");
        }

        commands.add("--output-format");
        commands.add("json");

        commands.add("--no-prompt");
        return commands.toArray(new String[0]);
    }
}

