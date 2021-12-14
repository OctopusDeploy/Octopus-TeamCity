package octopus.teamcity.server.generic;

import java.util.List;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunnerContext;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import octopus.teamcity.server.OctopusGenericRunType;
import octopus.teamcity.server.connection.OctopusConnection;

public class OctopusGenericRunnerBuildStartProcessor implements BuildStartContextProcessor {

  private final ExtensionHolder extensionHolder;
  private final OAuthConnectionsManager oAuthConnectionsManager;
  private final Logger logger = Loggers.SERVER;

  public OctopusGenericRunnerBuildStartProcessor(
      final ExtensionHolder extensionHolder,
      final OAuthConnectionsManager oAuthConnectionsManager) {
    this.extensionHolder = extensionHolder;
    this.oAuthConnectionsManager = oAuthConnectionsManager;
  }

  @Override
  public void updateParameters(final BuildStartContext buildStartContext) {
    try {
      if (buildStartContext.getRunnerContexts().stream()
          .anyMatch(rc -> rc.getRunType().getType().equals(OctopusConstants.GENERIC_RUNNER_TYPE))) {
        insertConnectionPropertiesIntoOctopusBuildSteps(buildStartContext);
      }
    } catch (final Throwable t) {
      logger.error("Failed to copy connection parameters into buildStartContext", t);
      throw t;
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

    final SProject project = buildType.getProject();

    final List<OAuthConnectionDescriptor> connections =
        oAuthConnectionsManager.getAvailableConnectionsOfType(project, OctopusConnection.TYPE);

    // For each OctopusGenericBuildStep in the build, find the referenced connection, and copy
    // parameters into the runnerParams
    buildStartContext.getRunnerContexts().stream()
        .filter(rc -> rc.getRunType() instanceof OctopusGenericRunType)
        .forEach(context -> updateBuildStepWithConnectionProperties(connections, context));
  }

  private void updateBuildStepWithConnectionProperties(
      final List<OAuthConnectionDescriptor> connections, final SRunnerContext context) {
    final String connectionId = context.getParameters().get(CommonStepPropertyNames.CONNECTION_ID);

    connections.stream()
        .filter(c -> c.getId().equals(connectionId))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No Octopus connection '"
                        + connectionId
                        + "' exists for the current "
                        + "project"))
        .getParameters()
        .forEach(context::addRunnerParameter);
  }

  public void register() {
    extensionHolder.registerExtension(
        BuildStartContextProcessor.class, this.getClass().getName(), this);
  }
}
