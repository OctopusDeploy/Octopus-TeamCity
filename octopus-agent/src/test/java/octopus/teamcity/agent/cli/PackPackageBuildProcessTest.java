package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PackPackageBuildProcessTest {

  @Test
  void createCommand_buildsNugetPackageCommand() {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackageIdKey(), "com.example.pkg");
    params.put(constants.getPackageFormatKey(), "nuget");
    params.put(constants.getPackageVersionKey(), "1.0.0");
    params.put(constants.getPackageSourcePathKey(), "/src");
    params.put(constants.getPackageOutputPathKey(), "/out");
    params.put(constants.getCommandLineArgumentsKey(), "--flag1 --flag2=value");

    when(context.getRunnerParameters()).thenReturn(params);

    PackPackageBuildProcess proc = new PackPackageBuildProcess(runningBuild, context, null);
    List<OctopusCommandBuilder> commands = proc.createCommand();

    assertThat(commands).hasSize(1);
    String[] pack = commands.get(0).buildCommand();

    assertThat(pack).containsExactly(
        "package",
        "nuget",
        "create",
        "--id",
        "com.example.pkg",
        "--version",
        "1.0.0",
        "--base-path",
        "/src",
        "--out-folder",
        "/out",
        "--flag1",
        "--flag2",
        "value",
        "--no-prompt"
    );
  }

  @Test
  void createCommand_buildsZipPackageCommandWhenFormatIsZip() {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackageIdKey(), "com.example.pkg");
    params.put(constants.getPackageFormatKey(), "zip");
    params.put(constants.getPackageVersionKey(), "2.0.0");
    params.put(constants.getPackageSourcePathKey(), "/src");
    params.put(constants.getPackageOutputPathKey(), "/out");

    when(context.getRunnerParameters()).thenReturn(params);

    PackPackageBuildProcess proc = new PackPackageBuildProcess(runningBuild, context, null);
    List<OctopusCommandBuilder> commands = proc.createCommand();

    assertThat(commands).hasSize(1);
    String[] pack = commands.get(0).buildCommand();

    assertThat(pack).contains(
        "package",
        "zip",
        "create",
        "--id",
        "com.example.pkg",
        "--version",
        "2.0.0",
        "--base-path",
        "/src",
        "--out-folder",
        "/out",
        "--no-prompt"
    );
  }
}
