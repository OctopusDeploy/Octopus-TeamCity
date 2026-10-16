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

package octopus.teamcity.server.client;

import java.io.IOException;

/** Octopus answered with a status other than 2xx. */
public class OctopusHttpStatusException extends IOException {
  private static final long serialVersionUID = 1L;

  private final int status;

  public OctopusHttpStatusException(final int status, final String message) {
    super(message);
    this.status = status;
  }

  public int getStatus() {
    return status;
  }
}
