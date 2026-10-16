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

package octopus.teamcity.server.spaces;

import java.io.IOException;

import jetbrains.buildServer.serverSide.SProject;
import org.jetbrains.annotations.Nullable;

/** Gets an OIDC ID token for the signed-in user outside a build. */
public interface OidcIdTokens {

  /**
   * An ID token from the given OIDC connection, issued with the subject a build of {@code
   * buildTypeExternalId} would get, or scoped to {@code project} when that is {@code null}.
   *
   * @throws IOException with a message fit to show the user
   */
  String request(SProject project, String oidcConnectionId, @Nullable String buildTypeExternalId)
      throws IOException;
}
