package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusOsUtilsTest {

  @ParameterizedTest
  @CsvSource({"amd64, windows-os-amd64", "x86_64, windows-os-amd64", "aarch64, windows-os-arm64"})
  void getBinaryOsFolder_returnsWindowsFolder_whenSystemIsWindows(
      String jvmArchitecture, String expectedFolder) throws Exception {
    AgentRunningBuild runningBuild = runningBuildOn(true, false);

    String result = OctopusOsUtils.getBinaryOsFolder(runningBuild, jvmArchitecture);

    assertThat(result).isEqualTo(expectedFolder);
  }

  @ParameterizedTest
  @CsvSource({"x86_64, mac-os-amd64", "aarch64, mac-os-arm64", "arm64, mac-os-arm64"})
  void getBinaryOsFolder_returnsMacFolder_whenSystemIsMac(
      String jvmArchitecture, String expectedFolder) throws Exception {
    AgentRunningBuild runningBuild = runningBuildOn(false, true);

    String result = OctopusOsUtils.getBinaryOsFolder(runningBuild, jvmArchitecture);

    assertThat(result).isEqualTo(expectedFolder);
  }

  @ParameterizedTest
  @CsvSource({"amd64, unix-os-amd64", "AMD64, unix-os-amd64", "aarch64, unix-os-arm64"})
  void getBinaryOsFolder_returnsUnixFolder_whenSystemIsUnix(
      String jvmArchitecture, String expectedFolder) throws Exception {
    AgentRunningBuild runningBuild = runningBuildOn(false, false);

    String result = OctopusOsUtils.getBinaryOsFolder(runningBuild, jvmArchitecture);

    assertThat(result).isEqualTo(expectedFolder);
  }

  @Test
  void getBinaryOsFolder_failsWithTheArchitecture_whenArchitectureIsUnsupported() {
    AgentRunningBuild runningBuild = runningBuildOn(false, false);

    assertThatThrownBy(() -> OctopusOsUtils.getBinaryOsFolder(runningBuild, "x86"))
        .isInstanceOf(RunBuildException.class)
        .hasMessageContaining("'x86'");
  }

  private static AgentRunningBuild runningBuildOn(boolean isWindows, boolean isMac) {
    AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
    BuildAgentConfiguration agentConfiguration =
        mock(BuildAgentConfiguration.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

    when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
    when(agentConfiguration.getSystemInfo().isWindows()).thenReturn(isWindows);
    if (!isWindows) {
      when(agentConfiguration.getSystemInfo().isMac()).thenReturn(isMac);
    }
    return runningBuild;
  }
}
