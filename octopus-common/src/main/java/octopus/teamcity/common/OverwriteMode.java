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

import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.StringJoiner;

public enum OverwriteMode {
  FailIfExists("Fail If Exists"),
  OverwriteExisting("Overwrite Existing"),
  IgnoreIfExists("Ignore if Exists");

  private final String humanReadable;

  OverwriteMode(final String humanReadable) {
    this.humanReadable = humanReadable;
  }

  public String getHumanReadable() {
    return humanReadable;
  }

  public static OverwriteMode fromString(final String value) {
    return stream(OverwriteMode.values())
        .filter(om -> om.toString().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(value + " is not a valid Overwrite mode"));
  }

  public static String validEntryString() {
    final StringJoiner joiner = new StringJoiner(", ");
    Arrays.stream(OverwriteMode.values()).forEach(e -> joiner.add(e.toString()));
    return joiner.toString();
  }
}
