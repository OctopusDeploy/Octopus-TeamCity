package octopus.teamcity.server;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunTypeExtension;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class OctopusBuildRunTypeExtension extends RunTypeExtension {
    private final PluginDescriptor pluginDescriptor;

    public OctopusBuildRunTypeExtension(final PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }

    @Override
    public Collection<String> getRunTypes() {
        Collection<String> items = new HashSet<String>();
        items.add("MSBuild");
        items.add("VS.Solution");
        return items;
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return p -> {
            final Collection<InvalidProperty> result = new ArrayList<>();
            return result;
        };
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("editBuildRunnerParams.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("viewBuildRunnerParams.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return Collections.emptyMap();
    }
}
