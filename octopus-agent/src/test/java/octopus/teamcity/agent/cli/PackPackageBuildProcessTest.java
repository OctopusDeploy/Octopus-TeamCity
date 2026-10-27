package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackPackageBuildProcessTest {

  @TempDir File workingDirectory;

  private void createCommandWithOutputPath(final String outputPath) throws RunBuildException {
    final AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    final BuildRunnerContext context = mock(BuildRunnerContext.class);
    final Map<String, String> params = new HashMap<>();
    params.put(OctopusConstants.Instance.getPackageOutputPathKey(), outputPath);
    when(context.getRunnerParameters()).thenReturn(params);
    when(context.getWorkingDirectory()).thenReturn(workingDirectory);
    when(runningBuild.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));

    new PackPackageBuildProcess(runningBuild, context, mock(ExtensionHolder.class)).createCommand();
  }

  @Test
  void createsARelativeOutputFolderInTheWorkingDirectory() throws RunBuildException {
    createCommandWithOutputPath("packed/nested");

    assertThat(new File(workingDirectory, "packed/nested")).isDirectory();
  }

  @Test
  void createsAnAbsoluteOutputFolder() throws RunBuildException {
    final File outputFolder = new File(workingDirectory, "absolute/packed");

    createCommandWithOutputPath(outputFolder.getAbsolutePath());

    assertThat(outputFolder).isDirectory();
  }

  @Test
  void acceptsAnOutputFolderThatAlreadyExists() throws RunBuildException {
    assertThat(new File(workingDirectory, "packed").mkdir()).isTrue();

    createCommandWithOutputPath("packed");

    assertThat(new File(workingDirectory, "packed")).isDirectory();
  }
}
