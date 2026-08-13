package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusConnectionUiDataTest {

  @Mock private OctopusConnectionsManager connectionsManager;
  @Mock private ProjectManager projectManager;
  @Mock private SUser user;

  @BeforeEach
  void setUp() {
    // Constructing the bean captures the collaborators into the class's static fields.
    new OctopusConnectionUiData(connectionsManager, projectManager);
  }

  private OAuthConnectionDescriptor connection(
      final String id, final String name, final String url, final String space) {
    final OAuthConnectionDescriptor descriptor = mock(OAuthConnectionDescriptor.class);
    when(descriptor.getId()).thenReturn(id);
    when(descriptor.getConnectionDisplayName()).thenReturn(name);
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, url);
    params.put(ConnectionPropertyNames.SPACE_NAME, space);
    when(descriptor.getParameters()).thenReturn(params);
    return descriptor;
  }

  @Test
  void mapsConnectionFieldsForTheView() {
    final OAuthConnectionDescriptor descriptor =
        connection("PROJECT_EXT_1", "Prod", "https://octo", "Spaces-1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final List<Map<String, String>> result = OctopusConnectionUiData.availableConnections(user);

    assertThat(result).hasSize(1);
    final Map<String, String> view = result.get(0);
    assertThat(view.get("id")).isEqualTo("PROJECT_EXT_1");
    assertThat(view.get("displayName")).isEqualTo("Prod");
    assertThat(view.get("url")).isEqualTo("https://octo");
    assertThat(view.get("space")).isEqualTo("Spaces-1");
  }

  @Test
  void doesNotExposeTheApiKey() {
    final OAuthConnectionDescriptor descriptor =
        connection("PROJECT_EXT_1", "Prod", "https://octo", "Spaces-1");
    // Even if the descriptor carried a secret, the view map must not contain it.
    descriptor.getParameters().put(ConnectionPropertyNames.API_KEY, "API-SECRETVALUE");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final Map<String, String> view = OctopusConnectionUiData.availableConnections(user).get(0);

    assertThat(view.values()).doesNotContain("API-SECRETVALUE");
    assertThat(view).doesNotContainKey(ConnectionPropertyNames.API_KEY);
  }

  @Test
  void returnsEmptyWhenNoUser() {
    assertThat(OctopusConnectionUiData.availableConnections((SUser) null)).isEmpty();
  }

  @Test
  void editConnectionUrlPointsAtCurrentProjectConnectionsTab() {
    final SBuildType buildType = mock(SBuildType.class);
    final SProject project = mock(SProject.class);
    when(projectManager.findBuildTypeByExternalId("Rtest_Build")).thenReturn(buildType);
    when(buildType.getProject()).thenReturn(project);
    when(project.getExternalId()).thenReturn("Rtest");

    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");
    when(request.getParameter("id")).thenReturn("buildType:Rtest_Build");

    assertThat(OctopusConnectionUiData.editConnectionUrl(request))
        .isEqualTo("/admin/editProject.html?projectId=Rtest&tab=oauthConnections");
  }

  @Test
  void editConnectionUrlFallsBackWhenProjectCannotBeResolved() {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");
    when(request.getParameter("id")).thenReturn(null);

    assertThat(OctopusConnectionUiData.editConnectionUrl(request))
        .isEqualTo("/admin/editProject.html?tab=oauthConnections");
  }

  private OAuthConnectionDescriptor oidcConnection(final String id, final String oidcConnectionId) {
    final OAuthConnectionDescriptor descriptor = mock(OAuthConnectionDescriptor.class);
    when(descriptor.getId()).thenReturn(id);
    when(descriptor.getConnectionDisplayName()).thenReturn("OIDC conn");
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.API_KEY_SOURCE, ConnectionPropertyNames.API_KEY_SOURCE_OIDC);
    params.put(ConnectionPropertyNames.OIDC_CONNECTION_ID, oidcConnectionId);
    when(descriptor.getParameters()).thenReturn(params);
    return descriptor;
  }

  private SBuildFeatureDescriptor oidcFeature(
      final String connectionId, final String tokenVariableName) {
    final SBuildFeatureDescriptor feature = mock(SBuildFeatureDescriptor.class);
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.OIDC_BUILD_FEATURE_CONNECTION_ID_PARAM, connectionId);
    params.put(ConnectionPropertyNames.OIDC_CONNECTOR_TOKEN_VARIABLE_NAME, tokenVariableName);
    when(feature.getParameters()).thenReturn(params);
    return feature;
  }

  private OAuthConnectionDescriptor connector(final String tokenVariableName) {
    final OAuthConnectionDescriptor descriptor = mock(OAuthConnectionDescriptor.class);
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.OIDC_CONNECTOR_TOKEN_VARIABLE_NAME, tokenVariableName);
    when(descriptor.getParameters()).thenReturn(params);
    return descriptor;
  }

  @Test
  void oidcConnectionWithNoFeatureWarnsFeatureMissing() {
    final SBuildType buildType = mock(SBuildType.class);
    final SProject project = mock(SProject.class);
    when(buildType.getProject()).thenReturn(project);
    when(buildType.getBuildFeaturesOfType(ConnectionPropertyNames.OIDC_BUILD_FEATURE_TYPE))
        .thenReturn(Collections.emptyList());
    when(connectionsManager.resolve(project, "CONNECTOR_1")).thenReturn(java.util.Optional.empty());
    final OAuthConnectionDescriptor descriptor = oidcConnection("OCT_1", "CONNECTOR_1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final Map<String, String> view =
        OctopusConnectionUiData.availableConnections(user, buildType).get(0);

    assertThat(view.get("oidcWarning")).isEqualTo("feature-missing");
    assertThat(view.get("oidcExpectedTokenVariable")).isEqualTo("jwt.token");
  }

  @Test
  void oidcConnectionWithMatchingFeatureWarnsNone() {
    final SBuildType buildType = mock(SBuildType.class);
    final SProject project = mock(SProject.class);
    when(buildType.getProject()).thenReturn(project);
    final SBuildFeatureDescriptor feature = oidcFeature("CONNECTOR_1", "");
    when(buildType.getBuildFeaturesOfType(ConnectionPropertyNames.OIDC_BUILD_FEATURE_TYPE))
        .thenReturn(Arrays.asList(feature));
    final OAuthConnectionDescriptor connector = connector("");
    when(connectionsManager.resolve(project, "CONNECTOR_1"))
        .thenReturn(java.util.Optional.of(connector));
    final OAuthConnectionDescriptor descriptor = oidcConnection("OCT_1", "CONNECTOR_1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final Map<String, String> view =
        OctopusConnectionUiData.availableConnections(user, buildType).get(0);

    assertThat(view.get("oidcWarning")).isEqualTo("none");
  }

  @Test
  void oidcConnectionWithMismatchedTokenVariableWarnsTokenMismatch() {
    final SBuildType buildType = mock(SBuildType.class);
    final SProject project = mock(SProject.class);
    when(buildType.getProject()).thenReturn(project);
    final SBuildFeatureDescriptor feature = oidcFeature("CONNECTOR_1", "custom.var");
    when(buildType.getBuildFeaturesOfType(ConnectionPropertyNames.OIDC_BUILD_FEATURE_TYPE))
        .thenReturn(Arrays.asList(feature));
    final OAuthConnectionDescriptor connector = connector("");
    when(connectionsManager.resolve(project, "CONNECTOR_1"))
        .thenReturn(java.util.Optional.of(connector));
    final OAuthConnectionDescriptor descriptor = oidcConnection("OCT_1", "CONNECTOR_1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final Map<String, String> view =
        OctopusConnectionUiData.availableConnections(user, buildType).get(0);

    assertThat(view.get("oidcWarning")).isEqualTo("token-mismatch");
    assertThat(view.get("oidcExpectedTokenVariable")).isEqualTo("jwt.token");
  }

  @Test
  void nonOidcConnectionHasNoWarning() {
    final SBuildType buildType = mock(SBuildType.class);
    when(buildType.getProject()).thenReturn(mock(SProject.class));
    when(buildType.getBuildFeaturesOfType(ConnectionPropertyNames.OIDC_BUILD_FEATURE_TYPE))
        .thenReturn(Collections.emptyList());
    final OAuthConnectionDescriptor descriptor =
        connection("OCT_1", "Prod", "https://octo", "Spaces-1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final Map<String, String> view =
        OctopusConnectionUiData.availableConnections(user, buildType).get(0);

    assertThat(view.get("oidcWarning")).isEqualTo("none");
    assertThat(view.get("oidcExpectedTokenVariable")).isEqualTo("");
  }

  @Test
  void unresolvedBuildTypeSuppressesWarning() {
    final OAuthConnectionDescriptor descriptor = oidcConnection("OCT_1", "CONNECTOR_1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(descriptor));

    final Map<String, String> view =
        OctopusConnectionUiData.availableConnections(user, (SBuildType) null).get(0);

    assertThat(view.get("oidcWarning")).isEqualTo("none");
  }

  @Test
  void buildFeaturesUrlPointsAtBuildFeaturesTab() {
    final SBuildType buildType = mock(SBuildType.class);
    when(projectManager.findBuildTypeByExternalId("Rtest_Build")).thenReturn(buildType);
    when(buildType.getExternalId()).thenReturn("Rtest_Build");

    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getContextPath()).thenReturn("");
    when(request.getParameter("id")).thenReturn("buildType:Rtest_Build");

    assertThat(OctopusConnectionUiData.buildFeaturesUrl(request))
        .isEqualTo("/admin/editBuildFeatures.html?id=buildType:Rtest_Build");
  }

  @Test
  void buildFeaturesUrlEmptyWhenUnresolved() {
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("id")).thenReturn(null);

    assertThat(OctopusConnectionUiData.buildFeaturesUrl(request)).isEqualTo("");
  }
}
