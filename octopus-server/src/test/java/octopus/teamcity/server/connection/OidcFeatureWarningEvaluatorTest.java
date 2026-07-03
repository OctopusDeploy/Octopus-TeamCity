package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import octopus.teamcity.common.connection.ConnectionPropertyNames;
import octopus.teamcity.server.connection.OidcFeatureWarningEvaluator.BuildFeature;
import octopus.teamcity.server.connection.OidcFeatureWarningEvaluator.Warning;
import org.junit.jupiter.api.Test;

class OidcFeatureWarningEvaluatorTest {
  private static final String OIDC = ConnectionPropertyNames.API_KEY_SOURCE_OIDC;

  private static BuildFeature feature(final String connectionId, final String tokenVariableName) {
    return new BuildFeature(connectionId, tokenVariableName);
  }

  @Test
  void nonOidcSourceIsNone() {
    assertThat(evaluateWith("key", "conn-1", "", Collections.emptyList())).isEqualTo(Warning.NONE);
  }

  @Test
  void oidcWithNoFeaturesIsFeatureMissing() {
    assertThat(evaluateWith(OIDC, "conn-1", "", Collections.emptyList()))
        .isEqualTo(Warning.FEATURE_MISSING);
  }

  @Test
  void oidcWithNoMatchingFeatureIsFeatureMissing() {
    assertThat(evaluateWith(OIDC, "conn-1", "", Arrays.asList(feature("other", ""))))
        .isEqualTo(Warning.FEATURE_MISSING);
  }

  @Test
  void inlineFeatureWithoutConnectionIdDoesNotMatch() {
    assertThat(evaluateWith(OIDC, "conn-1", "", Arrays.asList(feature("", "jwt.token"))))
        .isEqualTo(Warning.FEATURE_MISSING);
  }

  @Test
  void matchingFeatureBlankOverrideConnectorBlankIsNone() {
    assertThat(evaluateWith(OIDC, "conn-1", "", Arrays.asList(feature("conn-1", ""))))
        .isEqualTo(Warning.NONE);
  }

  @Test
  void matchingFeatureOverrideEqualsReferencedIsNone() {
    assertThat(evaluateWith(OIDC, "conn-1", "", Arrays.asList(feature("conn-1", "jwt.token"))))
        .isEqualTo(Warning.NONE);
  }

  @Test
  void matchingFeatureOverrideDiffersConnectorBlankIsMismatch() {
    assertThat(evaluateWith(OIDC, "conn-1", "", Arrays.asList(feature("conn-1", "custom"))))
        .isEqualTo(Warning.TOKEN_MISMATCH);
  }

  @Test
  void connectorVariableSetFeatureNoOverrideIsNone() {
    assertThat(evaluateWith(OIDC, "conn-1", "foo", Arrays.asList(feature("conn-1", ""))))
        .isEqualTo(Warning.NONE);
  }

  @Test
  void connectorVariableSetFeatureOverrideDiffersIsMismatch() {
    assertThat(evaluateWith(OIDC, "conn-1", "foo", Arrays.asList(feature("conn-1", "bar"))))
        .isEqualTo(Warning.TOKEN_MISMATCH);
  }

  @Test
  void multipleMatchingFeaturesOnePublishesReferencedIsNone() {
    assertThat(
            evaluateWith(
                OIDC,
                "conn-1",
                "foo",
                Arrays.asList(feature("conn-1", "bar"), feature("conn-1", ""))))
        .isEqualTo(Warning.NONE);
  }

  @Test
  void expectedTokenVariableFallsBackToDefault() {
    assertThat(OidcFeatureWarningEvaluator.expectedTokenVariable("")).isEqualTo("jwt.token");
    assertThat(OidcFeatureWarningEvaluator.expectedTokenVariable("  ")).isEqualTo("jwt.token");
    assertThat(OidcFeatureWarningEvaluator.expectedTokenVariable("foo")).isEqualTo("foo");
  }

  @Test
  void warningAttributeValues() {
    assertThat(Warning.NONE.attributeValue()).isEqualTo("none");
    assertThat(Warning.FEATURE_MISSING.attributeValue()).isEqualTo("feature-missing");
    assertThat(Warning.TOKEN_MISMATCH.attributeValue()).isEqualTo("token-mismatch");
  }

  private static Warning evaluateWith(
      final String apiKeySource,
      final String oidcConnectionId,
      final String connectorTokenVariableName,
      final List<BuildFeature> features) {
    return OidcFeatureWarningEvaluator.evaluate(
        apiKeySource, oidcConnectionId, connectorTokenVariableName, features);
  }
}
