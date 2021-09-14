package octopus.teamcity.server;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.commonstep.CommonStepUserData;
import octopus.teamcity.server.generic.BuildStepCollection;
import octopus.teamcity.server.generic.OctopusBuildStep;
import octopus.teamcity.server.generic.OctopusBuildStepPropertiesProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OctopusGenericRunType extends RunType {
  private final PluginDescriptor pluginDescriptor;

  public OctopusGenericRunType(
      final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
    runTypeRegistry.registerRunType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return OctopusConstants.GENERIC_RUNNER_TYPE;
  }

  @Override
  public String getDisplayName() {
    return "OctopusDeploy";
  }

  @Override
  public String getDescription() {
    return "Execute an operation against an OctopusDeploy server";
  }

  @Override
  public String describeParameters(final Map<String, String> parameters) {
    // NOTE: This is only called once the values in the map have been validated as being "within
    // bounds"
    final CommonStepUserData commonStepUserData = new CommonStepUserData(parameters);

    final String stepType = commonStepUserData.getStepType();
    if (commonStepUserData.getStepType().isEmpty()) {
      return "No build step specified";
    }

    final BuildStepCollection buildStepCollection = new BuildStepCollection();

    final Optional<OctopusBuildStep> buildStep =
        buildStepCollection.getSubSteps().stream()
            .filter(cmd -> cmd.getName().equals(stepType))
            .findFirst();

    if (!buildStep.isPresent()) {
      return "No build command corresponds to supplied build step name";
    }

    final StringBuilder builder = new StringBuilder(buildStep.get().getDescription());
    builder.append("\n");
    try {
      final String commonStepDescription = describeCommonParameters(commonStepUserData);
      builder.append(commonStepDescription);
      builder.append("\n");
    } catch (final MalformedURLException e) {
      return "Failed to parse provided URL - contact octopus support ("
          + e.getLocalizedMessage()
          + ")";
    }

    builder.append(buildStep.get().describeParameters(parameters));
    return builder.toString();
  }

  private String describeCommonParameters(final CommonStepUserData commonStepUserData)
      throws MalformedURLException {
    final StringBuilder builder = new StringBuilder();
    builder.append("Server: ");
    builder.append(commonStepUserData.getServerUrl().toString());
    builder.append("\n");

    final Optional<String> space = commonStepUserData.getSpaceName();
    builder.append("Space: ");
    builder.append(space.isPresent() ? "<default space>" : space);
    builder.append("\n");

    if (commonStepUserData.getProxyRequired()) {
      builder.append("Use Proxy: true\n");
      builder.append("Proxy Server: ");
      builder.append(commonStepUserData.getProxyServerUrl());
      builder.append("\n");
      builder.append("Username: ");
      builder.append(commonStepUserData.getProxyUsername());
      builder.append("\n");
      builder.append("Password: *****");
    } else {
      builder.append("Use Proxy: false\n");
    }

    return builder.toString();
  }

  @Nullable
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new OctopusBuildStepPropertiesProcessor();
  }

  @Nullable
  @Override
  public String getEditRunnerParamsJspFilePath() {
    return pluginDescriptor.getPluginResourcesPath(
        "v2" + File.separator + "editOctopusGeneric.jsp");
  }

  @Nullable
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return pluginDescriptor.getPluginResourcesPath(
        "v2" + File.separator + "viewOctopusGeneric.jsp");
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return new HashMap<>();
  }
}
