package octopus.teamcity.server;

import static octopus.teamcity.server.PropertiesValidator.checkCredentialsUnlessUsingConnection;
import static octopus.teamcity.server.PropertiesValidator.checkNotEmpty;
import static octopus.teamcity.server.PropertiesValidator.checkPromptedVariablesOnlyWhenDeploying;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.server.connection.ConnectionInlineFieldCleaner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OctopusCreateReleaseRunType extends RunType {
  private final PluginDescriptor pluginDescriptor;

  public OctopusCreateReleaseRunType(
      final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
    runTypeRegistry.registerRunType(this);
  }

  @NotNull
  @Override
  public String getType() {
    return OctopusConstants.CREATE_RELEASE_RUNNER_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "OctopusDeploy: Create release";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Creates and, optionally, deploys releases in Octopus Deploy";
  }

  @Nullable
  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    final OctopusConstants c = new OctopusConstants();
    return new PropertiesProcessor() {
      @Override
      @NotNull
      public Collection<InvalidProperty> process(@Nullable final Map<String, String> p) {
        final Collection<InvalidProperty> result = new ArrayList<>();
        if (p == null) return result;

        checkCredentialsUnlessUsingConnection(p, c, result);
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
        checkPromptedVariablesOnlyWhenDeploying(p, c, result);

        if (result.isEmpty()) {
          ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(p);
        }

        return result;
      }
    };
  }

  @Nullable
  @Override
  public String getEditRunnerParamsJspFilePath() {
    return pluginDescriptor.getPluginResourcesPath("forms/editOctopusCreateReleaseForm.jsp");
  }

  @Nullable
  @Override
  public String getViewRunnerParamsJspFilePath() {
    return pluginDescriptor.getPluginResourcesPath("viewOctopusCreateRelease.jsp");
  }

  @Nullable
  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return new HashMap<>();
  }
}
