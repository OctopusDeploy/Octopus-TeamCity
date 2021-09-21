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

import java.util.List;
import java.util.Map;

import com.intellij.openapi.util.text.StringUtil;

public class BaseUserData {
  protected final Map<String, String> params;

  public BaseUserData(final Map<String, String> params) {
    this.params = params;
  }

  protected String fetchRaw(final String key) {
    final String mapContent = params.get(key);
    if (mapContent == null) {
      throw new IllegalArgumentException("Property map does not contain an entry for " + key);
    }
    return mapContent;
  }

  protected List<String> fetchRawFromNewlineDelimited(final String key) {
    return StringUtil.split(fetchRaw(key), "\n");
  }
}
