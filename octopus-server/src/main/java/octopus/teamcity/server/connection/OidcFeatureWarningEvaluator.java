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

package octopus.teamcity.server.connection;

import java.util.List;

import octopus.teamcity.common.connection.ConnectionPropertyNames;

/**
 * Decides which advisory OIDC warning (if any) a step edit form should show for a selected
 * connection, from plain data. Kept free of TeamCity types so the branching logic is unit-tested in
 * isolation; {@link OctopusConnectionUiData} supplies the inputs.
 *
 * <p>Supported OIDC setup: the Octopus connection references a connector, and an {@code
 * oidc-plugin} build feature references that same connector via {@code connection_id}. An inline
 * feature (no {@code connection_id}) is not a supported Octopus OIDC path, so it never satisfies
 * the check.
 */
public final class OidcFeatureWarningEvaluator {

  /** The warning to display; {@link #attributeValue()} is the DOM data-attribute form. */
  public enum Warning {
    NONE("none"),
    FEATURE_MISSING("feature-missing"),
    TOKEN_MISMATCH("token-mismatch");

    private final String attributeValue;

    Warning(final String attributeValue) {
      this.attributeValue = attributeValue;
    }

    public String attributeValue() {
      return attributeValue;
    }
  }

  /** Minimal view of one {@code oidc-plugin} build feature on the build configuration. */
  public static final class BuildFeature {
    private final String connectionId;
    private final String tokenVariableName;

    public BuildFeature(final String connectionId, final String tokenVariableName) {
      this.connectionId = connectionId == null ? "" : connectionId;
      this.tokenVariableName = tokenVariableName == null ? "" : tokenVariableName;
    }

    public String getConnectionId() {
      return connectionId;
    }

    public String getTokenVariableName() {
      return tokenVariableName;
    }
  }

  private OidcFeatureWarningEvaluator() {}

  public static Warning evaluate(
      final String apiKeySource,
      final String oidcConnectionId,
      final String connectorTokenVariableName,
      final List<BuildFeature> oidcFeatures) {
    if (!ConnectionPropertyNames.API_KEY_SOURCE_OIDC.equals(apiKeySource)) {
      return Warning.NONE;
    }

    final String referenced = expectedTokenVariable(connectorTokenVariableName);
    boolean anyMatchingFeature = false;
    for (final BuildFeature feature : oidcFeatures) {
      if (isBlank(feature.getConnectionId())
          || !feature.getConnectionId().trim().equals(oidcConnectionId)) {
        continue;
      }
      anyMatchingFeature = true;
      final String published =
          isBlank(feature.getTokenVariableName()) ? referenced : feature.getTokenVariableName();
      if (published.equals(referenced)) {
        return Warning.NONE;
      }
    }

    return anyMatchingFeature ? Warning.TOKEN_MISMATCH : Warning.FEATURE_MISSING;
  }

  public static String expectedTokenVariable(final String connectorTokenVariableName) {
    return isBlank(connectorTokenVariableName)
        ? ConnectionPropertyNames.OIDC_DEFAULT_TOKEN_VARIABLE
        : connectorTokenVariableName;
  }

  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }
}
