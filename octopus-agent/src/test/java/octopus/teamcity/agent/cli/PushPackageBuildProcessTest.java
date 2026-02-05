package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.impl.artifacts.ArtifactsCollection;
import octopus.teamcity.agent.OctopusCommandBuilder;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PushPackageBuildProcessTest {

  private void setArtifactsCollections(Object proc, List<ArtifactsCollection> collections) throws Exception {
    Field f = proc.getClass().getDeclaredField("artifactsCollections");
    f.setAccessible(true);
    f.set(proc, collections);
  }

  @Test
  void createCommand_includesPackageUploadAndFailOverwriteModeByDefault() throws Exception {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackagePathsKey(), "**/*");

    when(context.getRunnerParameters()).thenReturn(params);

    ArtifactsCollection ac = mock(ArtifactsCollection.class);
    Map<File, String> fileMap = new HashMap<>();
    File f = new File("/tmp/my-package.1.0.0.nupkg");
    fileMap.put(f, "path");
    when(ac.getFilePathMap()).thenReturn(fileMap);

    List<ArtifactsCollection> collections = new ArrayList<>();
    collections.add(ac);

    PushPackageBuildProcess proc = new PushPackageBuildProcess(runningBuild, context, null);
    setArtifactsCollections(proc, collections);

    List<OctopusCommandBuilder> commands = proc.createCommand();

    assertThat(commands).hasSize(2);

    String[] upload = commands.get(1).buildCommand();
    assertThat(upload).contains(
        "package",
        "upload",
        "--package",
        f.getAbsolutePath(),
        "--overwrite-mode",
        "fail",
        "--no-prompt"
    );
  }

  @Test
  void createCommand_includesSpaceAndOverwriteWhenConfigured() throws Exception {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildProgressLogger logger = mock(BuildProgressLogger.class);
    BuildRunnerContext context = mock(BuildRunnerContext.class);

    when(runningBuild.getBuildLogger()).thenReturn(logger);

    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getPackagePathsKey(), "**/*");
    params.put(constants.getSpaceName(), "MySpace");
    params.put(constants.getForcePushKey(), "true");
    params.put(constants.getCommandLineArgumentsKey(), "--extra-flag --opt=val");

    when(context.getRunnerParameters()).thenReturn(params);

    ArtifactsCollection ac = mock(ArtifactsCollection.class);
    Map<File, String> fileMap = new HashMap<>();
    File f = new File("/tmp/my-package.2.0.0.nupkg");
    fileMap.put(f, "path");
    when(ac.getFilePathMap()).thenReturn(fileMap);

    List<ArtifactsCollection> collections = new ArrayList<>();
    collections.add(ac);

    PushPackageBuildProcess proc = new PushPackageBuildProcess(runningBuild, context, null);
    setArtifactsCollections(proc, collections);

    List<OctopusCommandBuilder> commands = proc.createCommand();

    assertThat(commands).hasSize(2);

    String[] upload = commands.get(1).buildCommand();
    assertThat(upload).contains(
        "package",
        "upload",
        "--space",
        "MySpace",
        "--package",
        f.getAbsolutePath(),
        "--overwrite-mode",
        "overwrite",
        "--extra-flag",
        "--opt",
        "val",
        "--no-prompt"
    );
  }
}
