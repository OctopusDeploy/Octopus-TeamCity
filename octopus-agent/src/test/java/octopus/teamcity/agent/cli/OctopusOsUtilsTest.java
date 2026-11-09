package octopus.teamcity.agent.cli;

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

  @ParameterizedTest
  @CsvSource({
    "Windows Server 2022, amd64",
    "Windows 11, aarch64",
    "Mac OS X, x86_64",
    "Mac OS X, aarch64",
    "Linux, amd64",
    "Linux, AMD64",
    "Linux, arm64"
  })
  void canRunCli_isTrue_whenOsAndArchitectureAreSupported(String osName, String jvmArchitecture) {
    assertThat(OctopusOsUtils.canRunCli(osName, jvmArchitecture)).isTrue();
  }

  @ParameterizedTest
  @CsvSource({"x86", "i386", "ppc64le", "s390x", "arm", "''"})
  void canRunCli_isFalse_whenArchitectureIsUnsupported(String jvmArchitecture) {
    assertThat(OctopusOsUtils.canRunCli("Linux", jvmArchitecture)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({"FreeBSD", "OpenBSD", "SunOS", "AIX", "''"})
  void canRunCli_isFalse_whenOsIsUnsupported(String osName) {
    assertThat(OctopusOsUtils.canRunCli(osName, "amd64")).isFalse();
  }

  @Test
  void canRunCli_isFalse_whenOsOrArchitectureIsUnknown() {
    assertThat(OctopusOsUtils.canRunCli(null, "amd64")).isFalse();
    assertThat(OctopusOsUtils.canRunCli("Linux", null)).isFalse();
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
