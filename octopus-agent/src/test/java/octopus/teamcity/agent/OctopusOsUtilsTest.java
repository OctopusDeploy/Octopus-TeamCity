
package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusOsUtilsTest {

    @Test
    void getBinaryOsFolder_returnsWindowsOs_whenSystemIsWindows() {
        // Arrange
        AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
        BuildAgentConfiguration agentConfiguration = mock(BuildAgentConfiguration.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
        when(agentConfiguration.getSystemInfo().isWindows()).thenReturn(true);

        // Act
        String result = OctopusOsUtils.getBinaryOsFolder(runningBuild);

        // Assert
        assertThat(result).isEqualTo("windows-os");
    }

    @Test
    void getBinaryOsFolder_returnsMacOs_whenSystemIsMac() {
        // Arrange
        AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
        BuildAgentConfiguration agentConfiguration = mock(BuildAgentConfiguration.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
        when(agentConfiguration.getSystemInfo().isWindows()).thenReturn(false);
        when(agentConfiguration.getSystemInfo().isMac()).thenReturn(true);

        // Act
        String result = OctopusOsUtils.getBinaryOsFolder(runningBuild);

        // Assert
        assertThat(result).isEqualTo("mac-os");
    }

    @Test
    void getBinaryOsFolder_returnsUnixOs_whenSystemIsUnix() {
        // Arrange
        AgentRunningBuild runningBuild = mock(AgentRunningBuild.class);
        BuildAgentConfiguration agentConfiguration = mock(BuildAgentConfiguration.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);

        when(runningBuild.getAgentConfiguration()).thenReturn(agentConfiguration);
        when(agentConfiguration.getSystemInfo().isWindows()).thenReturn(false);
        when(agentConfiguration.getSystemInfo().isMac()).thenReturn(false);

        // Act
        String result = OctopusOsUtils.getBinaryOsFolder(runningBuild);

        // Assert
        assertThat(result).isEqualTo("unix-os");
    }
}