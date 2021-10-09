/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.agent.generic;

import com.octopus.sdk.api.OverwriteMode;
import com.octopus.sdk.http.ConnectData;
import com.octopus.sdk.http.ConnectDataBuilder;
import com.octopus.sdk.http.ProxyData;

import java.net.MalformedURLException;

import octopus.teamcity.common.connection.ConnectionUserData;

public class TypeConverters {

  public static OverwriteMode from(final octopus.teamcity.common.OverwriteMode input) {
    switch (input) {
      case OverwriteExisting:
        return OverwriteMode.OverwriteExisting;
      case IgnoreIfExists:
        return OverwriteMode.IgnoreIfExists;
      case FailIfExists:
        return OverwriteMode.FailIfExists;
    }
    throw new IllegalArgumentException("No matching output available");
  }

  /** This assumes the userData contains correctly formatted strings * */
  public static ConnectData from(final ConnectionUserData userData) throws MalformedURLException {
    final ConnectDataBuilder builder = new ConnectDataBuilder();
    builder.withOctopusServerUrl(userData.getServerUrl()).withApiKey(userData.getApiKey());
    if (userData.getProxyRequired()) {
      final ProxyData proxy =
          new ProxyData(
              userData.getProxyUrl(), userData.getProxyUsername(), userData.getProxyPassword());
      builder.withProxy(proxy);
    }

    return builder.build();
  }
}
