package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

class OctopusOidcConnectorsUiDataTest {

  private OAuthConnectionDescriptor connector(final String id, final String name) {
    final OAuthConnectionDescriptor descriptor = mock(OAuthConnectionDescriptor.class);
    when(descriptor.getId()).thenReturn(id);
    when(descriptor.getConnectionDisplayName()).thenReturn(name);
    return descriptor;
  }

  @Test
  void listsOidcConnectorsForTheRequestProject() {
    final OAuthConnectionsManager oauth = mock(OAuthConnectionsManager.class);
    final ProjectManager projectManager = mock(ProjectManager.class);
    final SProject project = mock(SProject.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);

    final OAuthConnectionDescriptor connectorOne = connector("c1", "Prod OIDC");
    final OAuthConnectionDescriptor connectorTwo = connector("c2", "Test OIDC");

    when(request.getParameter("projectId")).thenReturn("MyProject");
    when(projectManager.findProjectByExternalId("MyProject")).thenReturn(project);
    when(oauth.getAvailableConnectionsOfType(project, ConnectionPropertyNames.OIDC_CONNECTOR_TYPE))
        .thenReturn(Arrays.asList(connectorOne, connectorTwo));

    new OctopusOidcConnectorsUiData(oauth, projectManager); // captures statics

    final List<Map<String, String>> result =
        OctopusOidcConnectorsUiData.availableConnectors(request);

    assertThat(result).hasSize(2);
    assertThat(result.get(0)).containsEntry("id", "c1").containsEntry("displayName", "Prod OIDC");
  }

  @Test
  void emptyWhenProjectCannotBeResolved() {
    final OAuthConnectionsManager oauth = mock(OAuthConnectionsManager.class);
    final ProjectManager projectManager = mock(ProjectManager.class);
    final HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getParameter("projectId")).thenReturn(null);

    new OctopusOidcConnectorsUiData(oauth, projectManager);

    assertThat(OctopusOidcConnectorsUiData.availableConnectors(request)).isEmpty();
  }
}
