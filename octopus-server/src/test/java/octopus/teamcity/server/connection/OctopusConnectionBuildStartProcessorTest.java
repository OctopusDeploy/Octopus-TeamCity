package octopus.teamcity.server.connection;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunnerContext;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusConnectionBuildStartProcessorTest {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  @Mock private ExtensionHolder extensionHolder;
  @Mock private OctopusConnectionsManager connectionsManager;
  @Mock private BuildStartContext buildStartContext;
  @Mock private SRunnerContext runnerContext;
  @Mock private SRunningBuild build;
  @Mock private SBuildType buildType;
  @Mock private SProject project;

  private OctopusConnectionBuildStartProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new OctopusConnectionBuildStartProcessor(extensionHolder, connectionsManager);
    lenient().when(buildStartContext.getBuild()).thenReturn(build);
    lenient().when(build.getBuildType()).thenReturn(buildType);
    lenient().when(buildType.getProject()).thenReturn(project);
    // doReturn bypasses the wildcard Collection<? extends SRunnerContext> type mismatch
    doReturn(Collections.singleton(runnerContext)).when(buildStartContext).getRunnerContexts();
    lenient().when(runnerContext.getType()).thenReturn(OctopusConstants.CREATE_RELEASE_RUNNER_TYPE);
  }

  /**
   * Build a mock OAuthConnectionDescriptor outside any when() chain to avoid
   * UnfinishedStubbingException caused by nested when() calls.
   */
  private OAuthConnectionDescriptor connectionWith(
      final String url, final String key, final String version, final String space) {
    final OAuthConnectionDescriptor descriptor = mock(OAuthConnectionDescriptor.class);
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, url);
    params.put(ConnectionPropertyNames.API_KEY, key);
    params.put(ConnectionPropertyNames.VERSION, version);
    if (space != null) {
      params.put(ConnectionPropertyNames.SPACE_NAME, space);
    }
    when(descriptor.getParameters()).thenReturn(params);
    return descriptor;
  }

  @Test
  void injectsConnectionValuesWhenConnectionSelected() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    when(runnerContext.getParameters()).thenReturn(properties);

    final OAuthConnectionDescriptor descriptor =
        connectionWith("https://octo", "API-KEY", "3.0+", "Spaces-1");
    when(connectionsManager.resolve(project, "PROJECT_EXT_1")).thenReturn(Optional.of(descriptor));

    processor.updateParameters(buildStartContext);

    verify(runnerContext).addRunnerParameter(CONSTANTS.getServerKey(), "https://octo");
    verify(runnerContext).addRunnerParameter(CONSTANTS.getApiKey(), "API-KEY");
    verify(runnerContext).addRunnerParameter(CONSTANTS.getOctopusVersion(), "3.0+");
    verify(runnerContext).addRunnerParameter(CONSTANTS.getSpaceName(), "Spaces-1");
  }

  @Test
  void stepSpaceOverridesConnectionSpace() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    properties.put(CONSTANTS.getSpaceName(), "StepSpace");
    when(runnerContext.getParameters()).thenReturn(properties);

    final OAuthConnectionDescriptor descriptor =
        connectionWith("https://octo", "API-KEY", "3.0+", "ConnSpace");
    when(connectionsManager.resolve(project, "PROJECT_EXT_1")).thenReturn(Optional.of(descriptor));

    processor.updateParameters(buildStartContext);

    // Step already supplied a space, so the processor must not touch the space param at all
    // (neither overwrite with the connection's value nor rewrite the step's own value).
    verify(runnerContext, never())
        .addRunnerParameter(
            org.mockito.ArgumentMatchers.eq(CONSTANTS.getSpaceName()),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void noInjectionWhenNoConnectionId() {
    when(runnerContext.getParameters()).thenReturn(new HashMap<>());

    processor.updateParameters(buildStartContext);

    verify(runnerContext, never())
        .addRunnerParameter(
            org.mockito.ArgumentMatchers.eq(CONSTANTS.getServerKey()),
            org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void ignoresNonOctopusRunners() {
    when(runnerContext.getType()).thenReturn("some.other.runner");
    // getParameters() is never called for non-Octopus runners — do not stub it.

    processor.updateParameters(buildStartContext);

    verify(connectionsManager, never())
        .resolve(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
