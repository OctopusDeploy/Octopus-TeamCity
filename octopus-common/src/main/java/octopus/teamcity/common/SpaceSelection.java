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

package octopus.teamcity.common;

import java.util.Map;

/**
 * Works out which space a step should act on.
 *
 * <p>The two CLI paths differ in how they treat "no space set": {@link #resolve} returns an empty
 * string so the legacy path can omit {@code --space} altogether, while {@link #resolveOrDefault}
 * substitutes {@link #DEFAULT_SPACE}, as the Go CLI path has always done.
 */
public final class SpaceSelection {

  /** The space the Go CLI path falls back to when a step names none. */
  public static final String DEFAULT_SPACE = "Default";

  private SpaceSelection() {}

  /** The value to pass to {@code --space}, or an empty string when no space is set. */
  public static String resolve(final Map<String, String> params) {
    return trimToEmpty(params.get(OctopusConstants.Instance.getSpaceName()));
  }

  /** As {@link #resolve}, but falls back to {@link #DEFAULT_SPACE} when no space is set. */
  public static String resolveOrDefault(final Map<String, String> params) {
    final String space = resolve(params);
    return space.isEmpty() ? DEFAULT_SPACE : space;
  }

  private static String trimToEmpty(final String value) {
    return value == null ? "" : value.trim();
  }
}
