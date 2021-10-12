package octopus.teamcity.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.commonstep.StepTypeConstants;
import octopus.teamcity.server.generic.BuildStepCollection;
import octopus.teamcity.server.generic.GenericParameterProcessor;
import octopus.teamcity.server.generic.OctopusBuildStep;

public class OctopusGenericRunType extends RunType {
  private final PluginDescriptor pluginDescriptor;

  public OctopusGenericRunType(
      final String enableStepVnext,
      final RunTypeRegistry runTypeRegistry,
      final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
    if (!StringUtil.isEmpty(enableStepVnext) && Boolean.parseBoolean(enableStepVnext)) {
      runTypeRegistry.registerRunType(this);
    }
  }

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

    final String stepType = parameters.get(StepTypeConstants.STEP_TYPE);
    if (stepType == null) {
      return "No build step type specified\n";
    }

    final BuildStepCollection buildStepCollection = new BuildStepCollection();
    final Optional<OctopusBuildStep> buildStep = buildStepCollection.getStepTypeByName(stepType);

    if (!buildStep.isPresent()) {
      return "No build command corresponds to supplied build step name\n";
    }

    return String.format(
        "%s\n%s\n",
        buildStep.get().getDescription(), buildStep.get().describeParameters(parameters));
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new GenericParameterProcessor();
  }

  @Override
  public String getEditRunnerParamsJspFilePath() {
    // as this doesn't point to a specific file (just a controller) - the pathing need not conform
    // to literal filepath
    return pluginDescriptor.getPluginResourcesPath("editOctopusGeneric.html");
  }

  @Override
  public String getViewRunnerParamsJspFilePath() {
    return pluginDescriptor.getPluginResourcesPath(
        "v2" + File.separator + "viewOctopusGeneric.jsp");
  }

  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return new HashMap<>();
  }
}
