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

package octopus.teamcity.common;

import jetbrains.buildServer.agent.Constants;

public class OctopusConstants {
    public final static OctopusConstants Instance = new OctopusConstants();

    public String getServerKey() {
        return "octopus_host";
    }

    public String getApiKey() {
        return Constants.SECURE_PROPERTY_PREFIX + "octopus_apikey";
    }

    public String getOctopusVersion() {
        return "octopus_version";
    }

    public String getVersion1() {
        return "1.0+";
    }

    public String getVersion2() {
        return "2.0+";
    }

    public String[] getOctopusVersions() {
        return new String[] { getVersion2(), getVersion1() };
    }

    public String getProjectNameKey() {
        return "octopus_project_name";
    }

    public String getRunOctoPack() {
        return "octopus_run_octopack";
    }

    public String getOctoPackPackageVersion() {
        return "octopus_octopack_package_version";
    }

    public String getOctoPackPublishPackageToHttp() {
        return "octopus_octopack_publish_package_to_http";
    }

    public String getOctoPackPublishApiKey() {
        return "octopus_octopack_publish_api_key";
    }

    public String getOctoPackAppendToPackageId() {
        return "octopus_octopack_append_to_package_id";
    }

    public String getDeployToKey() {
        return "octopus_deployto";
    }
    public String getPromoteFromKey() {
        return "octopus_promotefrom";
    }

    public String getCommandLineArgumentsKey() {
        return "octopus_additionalcommandlinearguments";
    }

    public String getReleaseNumberKey() {
        return "octopus_releasenumber";
    }

    public String getWaitForDeployments() {
        return "octopus_waitfordeployments";
    }
    public String getShowProgress() {
        return "octopus_progress";
    }

    public static final String CREATE_RELEASE_RUNNER_TYPE = "octopus.create.release";
    public static final String DEPLOY_RELEASE_RUNNER_TYPE = "octopus.deploy.release";
    public static final String PROMOTE_RELEASE_RUNNER_TYPE = "octopus.promote.release";
}
