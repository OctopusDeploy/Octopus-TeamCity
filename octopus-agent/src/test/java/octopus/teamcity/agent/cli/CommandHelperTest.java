package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandHelperTest {

  /** Every command sets --project, --space and --output-format from the step's own fields. */
  private static void assertStepKeepsItsOwnArguments(final String[] command) {
    final List<String> args = Arrays.asList(command);
    assertThat(args).containsSequence("--project", "MyProject");
    assertThat(args).containsSequence("--space", "MySpace");
    assertThat(args).containsSequence("--output-format", "json");
    assertThat(Collections.frequency(args, "--project")).isEqualTo(1);
    assertThat(Collections.frequency(args, "--space")).isEqualTo(1);
    assertThat(Collections.frequency(args, "--output-format")).isEqualTo(1);
    assertThat(args).doesNotContain("-p", "-s", "-f", "Other", "table");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "-f table",
        "--output-format table",
        "-p Other",
        "--project Other",
        "-s Other",
        "--space Other"
      })
  void commandsDropAdditionalArgumentsTheStepAlreadySetsForItself(
      final String additionalArguments) {
    final OctopusConstants constants = OctopusConstants.Instance;
    final Map<String, String> params = new HashMap<>();
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getSpaceName(), "MySpace");
    params.put(constants.getDeployToKey(), "Dev");
    params.put(constants.getRunbookNameKey(), "MyRunbook");
    params.put(constants.getCommandLineArgumentsKey(), additionalArguments);

    assertStepKeepsItsOwnArguments(CommandHelper.createRelease(params).buildCommand());
    assertStepKeepsItsOwnArguments(CommandHelper.deployRelease(params, "1.0.0"));
    assertStepKeepsItsOwnArguments(CommandHelper.runbookRun(params));
  }

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
  void runbookRunCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;

    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getRunbookNameKey(), "Rebuild indexes");
    params.put(constants.getRunbookSnapshotKey(), "Snapshot ABC");
    params.put(constants.getDeployToKey(), "Env1,Env2");
    params.put(constants.getTenantsKey(), "TenantA");
    params.put(constants.getTenantTagsKey(), "TagX");
    params.put(constants.getCommandLineArgumentsKey(), "--variable Name:Value");

    String[] command = CommandHelper.runbookRun(params);

    assertThat(command)
        .containsExactly(
            "runbook",
            "run",
            "--space",
            "Default",
            "--project",
            "MyProject",
            "--name",
            "Rebuild indexes",
            "--snapshot",
            "Snapshot ABC",
            "--environment",
            "Env1",
            "--environment",
            "Env2",
            "--tenant",
            "TenantA",
            "--tenant-tag",
            "TagX",
            "--output-format",
            "json",
            "--variable",
            "Name:Value",
            "--no-prompt");
  }

  @Test
  void runbookRunCommandDropsAdditionalArgsTheStepAlreadySets() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;

    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getRunbookNameKey(), "Rebuild indexes");
    params.put(constants.getDeployToKey(), "Env1");
    params.put(
        constants.getCommandLineArgumentsKey(), "--environment Sneaky --force-package-download");

    String[] command = CommandHelper.runbookRun(params);

    assertThat(command).doesNotContain("Sneaky");
    assertThat(command).contains("--force-package-download");
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
  void loginCommandUsesOidcWhenSourceIsOidc() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKeySourceKey(), ConnectionPropertyNames.API_KEY_SOURCE_OIDC);
    params.put(constants.getOidcServiceAccountIdKey(), "Spaces-SA-123");
    params.put(constants.getOidcIdTokenKey(), "the-jwt-value");

    String[] command = CommandHelper.login(params).buildCommand();

    assertThat(command)
        .contains(
            "login",
            "--server",
            "https://octo.example",
            "--service-account-id",
            "Spaces-SA-123",
            "--id-token",
            "the-jwt-value",
            "--no-prompt");
    assertThat(command).doesNotContain("--api-key");
  }

  @Test
  void loginCommandUsesApiKeyForNonOidcSource() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");

    String[] command = CommandHelper.login(params).buildCommand();

    assertThat(command).contains("--api-key", "API-KEY-123");
    assertThat(command).doesNotContain("--service-account-id", "--id-token");
  }

  private static Set<String> ignoring(String... args) {
    return new LinkedHashSet<>(Arrays.asList(args));
  }

  @Test
  void sanitizeCommandExtraArgsRemovesForbiddenArgAndValue() {
    List<String> args =
        Arrays.asList("project", "MyProject", "channel", "Release", "version", "1.0.0");

    List<String> result = CommandHelper.sanitizeCommandArgs(args, ignoring("channel"));

    assertThat(result).containsExactly("project", "MyProject", "version", "1.0.0");
  }

  @Test
  void sanitizeCommandExtraArgsRemovesMultipleForbiddenArgs() {
    List<String> args =
        Arrays.asList(
            "project", "MyProject", "channel", "Release", "tenant", "TenantA", "version", "1.0.0");

    List<String> result = CommandHelper.sanitizeCommandArgs(args, ignoring("channel", "tenant"));

    assertThat(result).containsExactly("project", "MyProject", "version", "1.0.0");
  }

  @Test
  void sanitizeCommandExtraArgsKeepsArgsWhenNoForbiddenPresent() {
    List<String> args = Arrays.asList("project", "MyProject", "version", "1.0.0");

    List<String> result = CommandHelper.sanitizeCommandArgs(args, ignoring("channel"));

    assertThat(result).containsExactly("project", "MyProject", "version", "1.0.0");
  }

  @Test
  void sanitizeCommandExtraArgsDoesNotConsumeArgumentAfterAValuelessSwitch() {
    List<String> args = Arrays.asList("--ignore-existing", "--variable", "ImageTag:1.5.2");

    List<String> result =
        CommandHelper.sanitizeCommandArgs(
            args, CommandHelper.createReleaseAdditionalArgumentsToBeIgnored);

    assertThat(result).containsExactly("--variable", "ImageTag:1.5.2");
  }

  @Test
  void sanitizeCommandExtraArgsDoesNotLeaveAStrayValueBehind() {
    List<String> args = Arrays.asList("--update-variables", "--channel", "Beta");

    List<String> result =
        CommandHelper.sanitizeCommandArgs(
            args, CommandHelper.deployReleaseAdditionalArgumentsToBeIgnored);

    assertThat(result).containsExactly("--channel", "Beta");
  }

  @Test
  void sanitizeCommandExtraArgsMatchesArgumentNamesExactly() {
    List<String> args = Arrays.asList("--var", "Something", "--e", "SomethingElse");

    List<String> result =
        CommandHelper.sanitizeCommandArgs(
            args, CommandHelper.deployReleaseAdditionalArgumentsToBeIgnored);

    assertThat(result).containsExactly("--var", "Something", "--e", "SomethingElse");
  }

  @Test
  void sanitizeCommandExtraArgsRemovesForbiddenArgWithAnInlineValue() {
    List<String> args = Arrays.asList("--variable=ImageTag:1.5.2", "--channel", "Beta");

    List<String> result =
        CommandHelper.sanitizeCommandArgs(
            args, CommandHelper.deployReleaseAdditionalArgumentsToBeIgnored);

    assertThat(result).containsExactly("--channel", "Beta");
  }

  @Test
  void createReleaseWithDeployToForwardsTheShortPromptedVariableFormToTheDeployCommand() {
    // -v is --variable on "release deploy" but --version on "release create". Left on the create
    // command the CLI rejects the release number: "The release number 'ImageTag:1.5.2' does not
    // appear to be a valid version number".
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getDeployToKey(), "Dev");
    params.put(constants.getCommandLineArgumentsKey(), "-v ImageTag:1.5.2");

    assertThat(CommandHelper.createRelease(params).buildCommand())
        .doesNotContain("-v", "ImageTag:1.5.2");
    assertThat(CommandHelper.deployRelease(params, "1.0.0"))
        .containsSequence("-v", "ImageTag:1.5.2");
  }

  @Test
  void createReleaseWithDeployToForwardsPromptedVariablesToTheDeployCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getDeployToKey(), "Dev");
    params.put(constants.getCommandLineArgumentsKey(), "--variable ImageTag:1.5.2");

    assertThat(CommandHelper.createRelease(params).buildCommand())
        .doesNotContain("--variable", "ImageTag:1.5.2");
    assertThat(CommandHelper.deployRelease(params, "1.0.0"))
        .containsSequence("--variable", "ImageTag:1.5.2");
  }
}
