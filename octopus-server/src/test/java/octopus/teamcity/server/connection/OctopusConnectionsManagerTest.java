package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusConnectionsManagerTest {
  @Mock private OAuthConnectionsManager teamcityOAuth;
  @Mock private ProjectManager projectManager;
  @Mock private SProject project;
  @Mock private User user;

  private OctopusConnectionsManager manager;

  @BeforeEach
  void setUp() {
    manager = new OctopusConnectionsManager(teamcityOAuth, projectManager);
  }

  private OAuthConnectionDescriptor connection(final String id) {
    final OAuthConnectionDescriptor descriptor =
        org.mockito.Mockito.mock(OAuthConnectionDescriptor.class);
    when(descriptor.getId()).thenReturn(id);
    return descriptor;
  }

  @Test
  void listAvailableDedupesById() {
    when(projectManager.getProjects()).thenReturn(Collections.singletonList(project));
    when(project.getProjectId()).thenReturn("p1");
    when(user.isPermissionGrantedForProject("p1", Permission.VIEW_PROJECT)).thenReturn(true);
    final OAuthConnectionDescriptor first = connection("PROJECT_EXT_1");
    final OAuthConnectionDescriptor duplicate = connection("PROJECT_EXT_1");
    when(teamcityOAuth.getAvailableConnectionsOfType(project, OctopusConnection.TYPE))
        .thenReturn(Arrays.asList(first, duplicate));

    final List<OAuthConnectionDescriptor> result = manager.listAvailableConnections(user);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("PROJECT_EXT_1");
  }

  @Test
  void listAvailableSkipsProjectsUserCannotView() {
    when(projectManager.getProjects()).thenReturn(Collections.singletonList(project));
    when(project.getProjectId()).thenReturn("p1");
    when(user.isPermissionGrantedForProject("p1", Permission.VIEW_PROJECT)).thenReturn(false);

    assertThat(manager.listAvailableConnections(user)).isEmpty();
  }

  @Test
  void resolveReturnsConnectionById() {
    final OAuthConnectionDescriptor connection =
        org.mockito.Mockito.mock(OAuthConnectionDescriptor.class);
    when(teamcityOAuth.findConnectionById(project, "PROJECT_EXT_1")).thenReturn(connection);

    assertThat(manager.resolve(project, "PROJECT_EXT_1")).contains(connection);
  }

  @Test
  void resolveEmptyForBlankId() {
    assertThat(manager.resolve(project, "  ")).isEmpty();
    assertThat(manager.resolve(project, null)).isEmpty();
  }

  @Test
  void resolveEmptyWhenNotFound() {
    when(teamcityOAuth.findConnectionById(project, "missing")).thenReturn(null);
    assertThat(manager.resolve(project, "missing")).isEmpty();
  }
}
