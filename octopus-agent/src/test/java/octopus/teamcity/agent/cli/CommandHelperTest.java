package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class CommandHelperTest {

  private void setArtifactsCollections(Object proc, List<ArtifactsCollection> collections)
      throws Exception {
    Field f = proc.getClass().getDeclaredField("artifactsCollections");
    f.setAccessible(true);
    f.set(proc, collections);
  }

  @Test
  void loginCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");

    String[] command = CommandHelper.login(params).buildCommand();

    assertThat(command)
        .contains(
            "login", "--server", "https://octo.example", "--api-key", "API-KEY-123", "--no-prompt");
  }

  @Test
  void deployCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;

    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getReleaseNumberKey(), "1.2.3");
    params.put(constants.getDeployToKey(), "Env1,Env2");
    params.put(constants.getTenantsKey(), "TenantA");
    params.put(constants.getTenantTagsKey(), "TagX,TagY");
    params.put(constants.getCommandLineArgumentsKey(), "arg1 arg2");

    String[] command = CommandHelper.deployRelease(params, null);

    assertThat(command)
        .contains(
            "release",
            "deploy",
            "--project",
            "MyProject",
            "--version",
            "1.2.3",
            "--environment",
            "Env1",
            "--environment",
            "Env2",
            "--tenant",
            "TenantA",
            "--tenant-tag",
            "TagX",
            "--tenant-tag",
            "TagY",
            "--output-format",
            "json",
            "arg1",
            "arg2",
            "--no-prompt");
  }

  @Test
  void waitCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;

    params.put(constants.getDeploymentTimeout(), "600");
    params.put(constants.getCancelDeploymentOnTimeout(), "true");
    params.put(constants.getSpaceName(), "MySpace");

    String[] command = CommandHelper.wait(params, "task-42");

    assertThat(command)
        .containsExactly(
            "task",
            "wait",
            "task-42",
            "--space",
            "MySpace",
            "--progress",
            "--timeout",
            "600",
            "--cancel-on-timeout",
            "--output-format",
            "json",
            "--no-prompt");
  }

  @Test
  void createReleaseCommandWithDeploy() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getReleaseNumberKey(), "1.2.3");
    params.put(constants.getDeployToKey(), "EnvA");
    params.put(constants.getWaitForDeployments(), "true");

    String[] login = CommandHelper.login(params).buildCommand();
    assertThat(login)
        .contains(
            "login", "--server", "https://octo.example", "--api-key", "API-KEY-123", "--no-prompt");

    String[] deploy = CommandHelper.deployRelease(params, null);
    assertThat(deploy)
        .contains(
            "release",
            "deploy",
            "--project",
            "MyProject",
            "--environment",
            "EnvA",
            "--output-format",
            "json",
            "--no-prompt");

    String[] wait = CommandHelper.wait(params, "task-123");
    assertThat(wait).contains("task", "wait", "task-123", "--output-format", "json", "--no-prompt");
  }

  @Test
  void deployReleaseCommandsSequence() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-456");
    params.put(constants.getProjectNameKey(), "AnotherProject");
    params.put(constants.getDeployToKey(), "EnvX,EnvY");

    String[] login = CommandHelper.login(params).buildCommand();
    assertThat(login).contains("login", "--server", "https://octo.example");

    String[] deploy = CommandHelper.deployRelease(params, null);
    assertThat(deploy)
        .contains(
            "release",
            "deploy",
            "--project",
            "AnotherProject",
            "--environment",
            "EnvX",
            "--environment",
            "EnvY");
  }

  @Test
  void packNugetPackageCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackageIdKey(), "com.example.pkg");
    params.put(constants.getPackageFormatKey(), "nuget");
    params.put(constants.getPackageVersionKey(), "1.0.0");
    params.put(constants.getPackageSourcePathKey(), "/src");
    params.put(constants.getPackageOutputPathKey(), "/out");
    params.put(constants.getCommandLineArgumentsKey(), "--flag1 --flag2=value");

    OctopusCommandBuilder packCommandBuilder = CommandHelper.packPackage(params);

    assertThat(packCommandBuilder.buildCommand())
        .containsExactly(
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
            "--no-prompt");
  }

  @Test
  void packZipPackageCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackageIdKey(), "com.example.pkg");
    params.put(constants.getPackageFormatKey(), "zip");
    params.put(constants.getPackageVersionKey(), "2.0.0");
    params.put(constants.getPackageSourcePathKey(), "/src");
    params.put(constants.getPackageOutputPathKey(), "/out");

    OctopusCommandBuilder packCommandBuilder = CommandHelper.packPackage(params);

    assertThat(packCommandBuilder.buildCommand())
        .contains(
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
            "--no-prompt");
  }

  @Test
  void pushPackageUploadWithDefaultOverwrite() throws Exception {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackagePathsKey(), "**/*");

    File f = new File("/tmp/my-package.1.0.0.nupkg");
    Map<File, String> fileMap = new HashMap<>();
    fileMap.put(f, "path");

    ArtifactsCollection ac = mock(ArtifactsCollection.class);
    when(ac.getFilePathMap()).thenReturn(fileMap);

    List<ArtifactsCollection> collections = new ArrayList<>();
    collections.add(ac);

    PushPackageBuildProcess proc =
        new PushPackageBuildProcess(
            mock(AgentRunningBuild.class), mock(BuildRunnerContext.class), null);
    setArtifactsCollections(proc, collections);

    OctopusCommandBuilder pushPackageCommand = CommandHelper.pushPackage(params, collections);

    assertThat(pushPackageCommand.buildCommand())
        .contains(
            "package",
            "upload",
            "--package",
            f.getAbsolutePath(),
            "--overwrite-mode",
            "fail",
            "--no-prompt");
  }

  @Test
  void sanitizeCommandExtraArgsRemovesForbiddenArgAndValue() {
    List<String> args =
        Arrays.asList("project", "MyProject", "channel", "Release", "version", "1.0.0");

    List<String> result = CommandHelper.sanitizeCommandArgs(args, "channel");

    assertThat(result).containsExactly("project", "MyProject", "version", "1.0.0");
  }

  @Test
  void sanitizeCommandExtraArgsRemovesMultipleForbiddenArgs() {
    List<String> args =
        Arrays.asList(
            "project", "MyProject", "channel", "Release", "tenant", "TenantA", "version", "1.0.0");

    List<String> result = CommandHelper.sanitizeCommandArgs(args, "channel, tenant");

    assertThat(result).containsExactly("project", "MyProject", "version", "1.0.0");
  }

  @Test
  void sanitizeCommandExtraArgsKeepsArgsWhenNoForbiddenPresent() {
    List<String> args = Arrays.asList("project", "MyProject", "version", "1.0.0");

    List<String> result = CommandHelper.sanitizeCommandArgs(args, "channel");

    assertThat(result).containsExactly("project", "MyProject", "version", "1.0.0");
  }
}
