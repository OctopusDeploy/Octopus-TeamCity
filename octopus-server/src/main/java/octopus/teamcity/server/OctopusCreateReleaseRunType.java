package octopus.teamcity.server;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OctopusCreateReleaseRunType extends RunType {
    private final PluginDescriptor pluginDescriptor;

    public OctopusCreateReleaseRunType(final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
        runTypeRegistry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return OctopusConstants.CREATE_RELEASE_RUNNER_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "OctopusDeploy: Create release";
    }

    @Override
    public String getDescription() {
        return "Creates and, optionally, deploys releases in Octopus Deploy";
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        final OctopusConstants c = new OctopusConstants();
        return new PropertiesProcessor() {
            private void checkNotEmpty(@NotNull final Map<String, String> properties,
                                       @NotNull final String key,
                                       @NotNull final String message,
                                       @NotNull final Collection<InvalidProperty> res) {
                if (jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(properties.get(key))) {
                    res.add(new InvalidProperty(key, message));
                }
            }

            @Override
            @NotNull
            public Collection<InvalidProperty> process(@Nullable final Map<String, String> p) {
                final Collection<InvalidProperty> result = new ArrayList<InvalidProperty>();
                if (p == null) return result;

                checkNotEmpty(p, c.getApiKey(), "API key must be specified", result);
                checkNotEmpty(p, c.getServerKey(), "Server must be specified", result);
                checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);

                return result;
            }
        };
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("editOctopusCreateRelease.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("viewOctopusCreateRelease.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        final Map<String, String> map = new HashMap<String, String>();
        return map;
    }
}
