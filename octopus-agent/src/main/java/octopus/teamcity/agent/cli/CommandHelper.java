package octopus.teamcity.agent.cli;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import octopus.teamcity.common.OverwriteMode;
import org.apache.commons.lang3.StringUtils;

import static octopus.teamcity.agent.OctopusCommandBuilder.splitCommaSeparatedValues;
import static octopus.teamcity.agent.OctopusCommandBuilder.splitSpaceSeparatedValues;
import static octopus.teamcity.agent.cli.CommandUtils.getOverwriteMode;

public class CommandHelper {

    public static String[] deployRelease(Map<String, String> params, String autoCreatedReleaseNumber) {
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

    public static OctopusCommandBuilder createRelease(Map<String, String> parameters) {
        final OctopusConstants constants = OctopusConstants.Instance;
        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> createCommands = new ArrayList<>();
                final String spaceName = parameters.get(constants.getSpaceName());
                final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());
                final String releaseNumber = parameters.get(constants.getReleaseNumberKey());
                final String channelName = parameters.get(constants.getChannelNameKey());
                final String projectName = parameters.get(constants.getProjectNameKey());
                final String gitRef = parameters.get(constants.getGitRefKey());
                final String gitCommit = parameters.get(constants.getGitCommitKey());

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
                return createCommands.toArray(new String[0]);
            }
        };
    }

    public static OctopusCommandBuilder packPackage(Map<String, String> parameters) {
        final OctopusConstants constants = OctopusConstants.Instance;
        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String packageId = parameters.get(constants.getPackageIdKey());
                final String packageFormat = parameters.get(constants.getPackageFormatKey()).toLowerCase();
                final String packageVersion = parameters.get(constants.getPackageVersionKey());
                final String sourcePath = parameters.get(constants.getPackageSourcePathKey());
                final String outputPath = parameters.get(constants.getPackageOutputPathKey());
                final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());

                commands.add("package");

                String formatSubCommand = "nuget";
                if ("zip".equals(packageFormat.toLowerCase())) {
                    formatSubCommand = "zip";
                }

                commands.add(formatSubCommand);
                commands.add("create");

                commands.add("--id");
                commands.add(packageId);

                commands.add("--version");
                commands.add(packageVersion);

                commands.add("--base-path");
                commands.add(sourcePath);

                commands.add("--out-folder");
                commands.add(outputPath);

                if (StringUtils.isNotBlank(commandLineArguments)) {
                    commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
                }

                commands.add("--no-prompt");
                return commands.toArray(new String[commands.size()]);
            }
        };
    }

    public static OctopusCommandBuilder pushPackage(Map<String, String> parameters,List<ArtifactsCollection> artifactsCollections) {
        final OctopusConstants constants = OctopusConstants.Instance;
        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String spaceName = parameters.get(constants.getSpaceName());
                final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());
                final String forcePush = parameters.get(constants.getForcePushKey());

                OverwriteMode overwriteMode = OverwriteMode.FailIfExists;
                if ("true".equals(forcePush)) {
                    overwriteMode = OverwriteMode.OverwriteExisting;
                } else if (OverwriteMode.IgnoreIfExists.name().equals(forcePush)) {
                    overwriteMode = OverwriteMode.IgnoreIfExists;
                }

                commands.add("package");
                commands.add("upload");

                if (StringUtils.isNotBlank(spaceName)) {
                    commands.add("--space");
                    commands.add(spaceName);
                }

                for (ArtifactsCollection artifactsCollection : artifactsCollections) {
                    for (Map.Entry<File, String> fileStringEntry : artifactsCollection.getFilePathMap().entrySet()) {
                        final File source = fileStringEntry.getKey();
                        commands.add("--package");
                        commands.add(source.getAbsolutePath());
                    }
                }

                commands.add("--overwrite-mode");
                commands.add(getOverwriteMode(overwriteMode));

                if (StringUtils.isNotBlank(commandLineArguments)) {
                    commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
                }

                commands.add("--no-prompt");
                return commands.toArray(new String[0]);
            }
        };
    }

    public static OctopusCommandBuilder buildInformation(Map<String, String> parameters,
                                                         boolean verboseLogging,
                                                         final String dataFile,
                                                         BuildProgressLogger logger) {
        final OctopusConstants constants = OctopusConstants.Instance;
        return new OctopusCommandBuilder() {
            @Override
            protected String[] buildCommand(boolean masked) {
                final ArrayList<String> commands = new ArrayList<String>();
                final String spaceName = parameters.get(constants.getSpaceName());
                final String packageIds = parameters.get(constants.getPackageIdKey());
                final String packageVersion = parameters.get(constants.getPackageVersionKey());
                final String commandLineArguments = parameters.get(constants.getCommandLineArgumentsKey());

                final String forcePush = parameters.get(constants.getForcePushKey());
                OverwriteMode overwriteMode = OverwriteMode.FailIfExists;
                if ("true".equals(forcePush)) {
                    overwriteMode = OverwriteMode.OverwriteExisting;
                } else if (OverwriteMode.IgnoreIfExists.name().equals(forcePush)) {
                    overwriteMode = OverwriteMode.IgnoreIfExists;
                }

                if (verboseLogging) {
                    logger.message("ForcePush: " + forcePush);
                    logger.message("OverwriteMode: " + overwriteMode.name());
                }

                commands.add("build-information");
                commands.add("upload");

                if (StringUtils.isNotBlank(spaceName)) {
                    commands.add("--space");
                    commands.add(spaceName);
                }

                for (String packageId : StringUtil.split(packageIds, "\n")) {
                    commands.add("--package-id");
                    commands.add(packageId);
                }

                commands.add("--version");
                commands.add(packageVersion);

                commands.add("--file");
                commands.add(dataFile);

                commands.add("--overwrite-mode");
                commands.add(getOverwriteMode(overwriteMode));

                if (StringUtils.isNotBlank(commandLineArguments)) {
                    commands.addAll(splitSpaceSeparatedValues(commandLineArguments));
                }

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

