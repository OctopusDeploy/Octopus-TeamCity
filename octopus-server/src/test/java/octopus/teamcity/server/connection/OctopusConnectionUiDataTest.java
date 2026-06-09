package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.ProjectManager;
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
      final String id,
      final String name,
      final String url,
      final String version,
      final String space) {
    final OAuthConnectionDescriptor d = mock(OAuthConnectionDescriptor.class);
    when(d.getId()).thenReturn(id);
    when(d.getConnectionDisplayName()).thenReturn(name);
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, url);
    params.put(ConnectionPropertyNames.VERSION, version);
    params.put(ConnectionPropertyNames.SPACE_NAME, space);
    when(d.getParameters()).thenReturn(params);
    return d;
  }

  @Test
  void mapsConnectionFieldsForTheView() {
    final OAuthConnectionDescriptor d =
        connection("PROJECT_EXT_1", "Prod", "https://octo", "3.0+", "Spaces-1");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(d));

    final List<Map<String, String>> result = OctopusConnectionUiData.availableConnections(user);

    assertThat(result).hasSize(1);
    final Map<String, String> view = result.get(0);
    assertThat(view.get("id")).isEqualTo("PROJECT_EXT_1");
    assertThat(view.get("displayName")).isEqualTo("Prod");
    assertThat(view.get("url")).isEqualTo("https://octo");
    assertThat(view.get("version")).isEqualTo("3.0+");
    assertThat(view.get("space")).isEqualTo("Spaces-1");
  }

  @Test
  void doesNotExposeTheApiKey() {
    final OAuthConnectionDescriptor d =
        connection("PROJECT_EXT_1", "Prod", "https://octo", "3.0+", "Spaces-1");
    // Even if the descriptor carried a secret, the view map must not contain it.
    d.getParameters().put(ConnectionPropertyNames.API_KEY, "API-SECRETVALUE");
    when(connectionsManager.listAvailableConnections(user)).thenReturn(Arrays.asList(d));

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
}
