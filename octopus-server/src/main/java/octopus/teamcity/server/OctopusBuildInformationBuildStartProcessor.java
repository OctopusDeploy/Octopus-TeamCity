package octopus.teamcity.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunnerContext;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;

import java.util.List;

public class OctopusBuildInformationBuildStartProcessor implements BuildStartContextProcessor {

  private final ExtensionHolder extensionHolder;
  private final OAuthConnectionsManager oAuthConnectionsManager;
  private final Logger logger = Loggers.SERVER;

  public OctopusBuildInformationBuildStartProcessor(
      final ExtensionHolder extensionHolder,
      final OAuthConnectionsManager oAuthConnectionsManager) {
    this.extensionHolder = extensionHolder;
    this.oAuthConnectionsManager = oAuthConnectionsManager;
  }

  @Override
  public void updateParameters(final BuildStartContext buildStartContext) {

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

    final String enableStepVnext = System.getProperty("octopus.enable.step.vnext");
    if (!StringUtil.isEmpty(enableStepVnext) && Boolean.parseBoolean(enableStepVnext)) {
      insertConnectionPropertiesIntoOctopusBuildSteps(buildStartContext);
    }
  }

  private void insertConnectionPropertiesIntoOctopusBuildSteps(
      final BuildStartContext buildStartContext) {
    final SBuildType buildType = buildStartContext.getBuild().getBuildType();
    if (buildType == null) {
      logger.error(
          "Unable to find the buildType, connection data not included in buildStartContext");
      return;
    }

    final SProject project = buildStartContext.getBuild().getBuildType().getProject();

    // For each OctopusGenericBuildStep in the build, find the referenced connection, and copy
    // parameters into the runnerParams
    buildStartContext.getRunnerContexts().stream()
        .filter(rc -> rc.getRunType() instanceof OctopusGenericRunType)
        .forEach(context -> updateBuildStepWithConnectionProperties(project, context));
  }

  private void updateBuildStepWithConnectionProperties(
      final SProject project, final SRunnerContext context) {
    final String connectionId = context.getParameters().get(CommonStepPropertyNames.CONNECTION_ID);

    final OAuthConnectionDescriptor connection =
        oAuthConnectionsManager.findConnectionById(project, connectionId);

    if(connection == null) {
      throw new IllegalArgumentException(
          "No Octopus connection '" + connectionId + "' exists for the current " + "project");
    }

    connection.getParameters().forEach(context::addRunnerParameter);
  }

  public void register() {
    extensionHolder.registerExtension(
        BuildStartContextProcessor.class, this.getClass().getName(), this);
  }
}
