/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class OctopusPackPackageRunType extends RunType {
    private final PluginDescriptor pluginDescriptor;

    public OctopusPackPackageRunType(final RunTypeRegistry runTypeRegistry, final PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
        runTypeRegistry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return OctopusConstants.PACK_PACKAGE_RUNNER_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "OctopusDeploy: Pack";
    }

    @Override
    public String getDescription() {
        return "Packages files (.nupkg, .zip, .tar.gz, etc.)";
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

                checkNotEmpty(p, c.getPackageIdKey(), "Package ID must be specified", result);
                checkNotEmpty(p, c.getPackageFormatKey(), "Package format must be specified", result);
                checkNotEmpty(p, c.getPackageVersionKey(), "Package version be specified", result);
                checkNotEmpty(p, c.getPackageSourcePathKey(), "Source path must be specified", result);
                checkNotEmpty(p, c.getPackageOutputPathKey(), "Output path must be specified", result);

                return result;
            }
        };
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("editOctopusPackPackage.jsp");
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return pluginDescriptor.getPluginResourcesPath("viewOctopusPackPackage.jsp");
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        final Map<String, String> map = new HashMap<String, String>();
        return map;
    }
}
