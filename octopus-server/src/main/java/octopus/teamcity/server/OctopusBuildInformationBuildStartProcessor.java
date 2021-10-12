package octopus.teamcity.server;

import java.util.List;
import java.util.Map;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.server.connection.ConnectionHelper;
import org.jetbrains.annotations.NotNull;

public class OctopusBuildInformationBuildStartProcessor implements BuildStartContextProcessor {

  private final ExtensionHolder extensionHolder;
  private final OAuthConnectionsManager oAuthConnectionsManager;
  private final ProjectManager projectManager;

  public OctopusBuildInformationBuildStartProcessor(
      @NotNull final ExtensionHolder extensionHolder,
      final OAuthConnectionsManager oAuthConnectionsManager,
      final ProjectManager projectManager) {
    this.extensionHolder = extensionHolder;
    this.oAuthConnectionsManager = oAuthConnectionsManager;
    this.projectManager = projectManager;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext buildStartContext) {

    final SRunningBuild build = buildStartContext.getBuild();
    final List<VcsRootInstanceEntry> vcsRoots = build.getVcsRootEntries();

    if (vcsRoots.size() != 0) {
      boolean buildContainsBuildInformationStep =
          buildStartContext.getRunnerContexts().stream()
              .anyMatch(rc -> rc.getRunType() instanceof OctopusBuildInformationRunType);

      if (buildContainsBuildInformationStep) {
        final VcsRootInstanceEntry vcsRoot = vcsRoots.get(0);
        String vcsType = "Unknown";
        if (vcsRoot.getVcsName().contains("git")) {
          vcsType = "Git";
        }
        buildStartContext.addSharedParameter("octopus_vcstype", vcsType);
      }
    }

    final SUser user = buildStartContext.getBuild().getTriggeredBy().getUser();
    // For each OctopusGenericBuildStep in the build, inject the OAuthParameters to the runner map
    buildStartContext.getRunnerContexts().stream()
        .filter(rc -> rc.getRunType() instanceof OctopusGenericRunType)
        .forEach(
            context -> {
              final String connectionId =
                  context.getParameters().get(CommonStepPropertyNames.CONNECTION_ID);
              final Map<String, String> connectionParams =
                  getConnectionParametersForConnection(connectionId, user);
              connectionParams.forEach(context::addRunnerParameter);
            });
  }

  private Map<String, String> getConnectionParametersForConnection(
      final String connectionId, final SUser user) {

    final List<OAuthConnectionDescriptor> allConnections =
        ConnectionHelper.getAvailableOctopusConnections(
            oAuthConnectionsManager, projectManager, user);

    final OAuthConnectionDescriptor connectionDescriptor =
        allConnections.stream()
            .filter(c -> c.getId().equals(connectionId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No Octopus connection '"
                            + connectionId
                            + "' exists for the current "
                            + "project"));

    return connectionDescriptor.getParameters();
  }

  public void register() {
    extensionHolder.registerExtension(
        BuildStartContextProcessor.class, this.getClass().getName(), this);
  }
}
