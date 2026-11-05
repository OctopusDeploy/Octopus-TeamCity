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
 * <p>A step (or the connection it inherits from) can identify a space either by id - {@code
 * Spaces-123} - or by name. The id is preferred because it survives the space being renamed, which
 * a name does not. The Go CLI accepts either for {@code --space}: it prefers a name match, then
 * falls back to an id.
 */
public final class SpaceSelection {

  /** The space used when a step names none. */
  public static final String DEFAULT_SPACE = "Default";

  private SpaceSelection() {}

  /**
   * The value to pass to {@code --space}, preferring the space id over the space name, or an empty
   * string when neither is set.
   */
  public static String resolve(final Map<String, String> params) {
    final OctopusConstants constants = OctopusConstants.Instance;
    final String spaceId = trimToEmpty(params.get(constants.getSpaceId()));
    if (!spaceId.isEmpty()) {
      return spaceId;
    }
    return trimToEmpty(params.get(constants.getSpaceName()));
  }

  /** As {@link #resolve}, but falls back to {@link #DEFAULT_SPACE} when no space is set. */
  public static String resolveOrDefault(final Map<String, String> params) {
    final String space = resolve(params);
    return space.isEmpty() ? DEFAULT_SPACE : space;
  }

  /** Whether a space was identified at all, by either id or name. */
  public static boolean isSet(final Map<String, String> params) {
    return !resolve(params).isEmpty();
  }

  private static String trimToEmpty(final String value) {
    return value == null ? "" : value.trim();
  }
}
