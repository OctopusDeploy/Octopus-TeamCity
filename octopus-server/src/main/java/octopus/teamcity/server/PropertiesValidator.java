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

package octopus.teamcity.server;

import java.util.Collection;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import org.jetbrains.annotations.NotNull;

/** Shared validation helpers for run type {@code PropertiesProcessor}s. */
public final class PropertiesValidator {

  private PropertiesValidator() {}

  /**
   * Adds an {@link InvalidProperty} to {@code result} when the value for {@code key} is missing or
   * blank.
   */
  public static void checkNotEmpty(
      @NotNull final Map<String, String> properties,
      @NotNull final String key,
      @NotNull final String message,
      @NotNull final Collection<InvalidProperty> result) {
    if (StringUtil.isEmptyOrSpaces(properties.get(key))) {
      result.add(new InvalidProperty(key, message));
    }
  }

  /**
   * Requires the inline server URL and API key unless a reusable connection is selected (in which
   * case those credentials come from the connection at build start).
   */
  public static void checkCredentialsUnlessUsingConnection(
      @NotNull final Map<String, String> properties,
      @NotNull final OctopusConstants constants,
      @NotNull final Collection<InvalidProperty> result) {
    final boolean usingConnection =
        !StringUtil.isEmptyOrSpaces(properties.get(constants.getConnectionIdKey()));
    if (!usingConnection) {
      checkNotEmpty(properties, constants.getServerKey(), "Server must be specified", result);
      checkNotEmpty(properties, constants.getApiKey(), "API key must be specified", result);
    }
  }
}
