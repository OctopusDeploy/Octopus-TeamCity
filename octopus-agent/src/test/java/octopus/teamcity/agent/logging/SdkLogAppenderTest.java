package octopus.teamcity.agent.logging;

import static octopus.teamcity.agent.logging.SdkLogAppender.isVerboseLogging;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import octopus.teamcity.common.commonstep.CommonStepPropertyNames;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Test;

class SdkLogAppenderTest {

  @Test
  void isVerboseLoggingReturnsInfoLevelWhenMapIsNull() {
    assertThat(Level.INFO).isEqualTo(isVerboseLogging(null));
  }

  @Test
  void isVerboseLoggingReturnsInfoLevelWhenVerboseIsNull() {
    assertThat(Level.INFO).isEqualTo(isVerboseLogging(buildParameterWithVerbose(null)));
  }

  @Test
  void isVerboseLoggingReturnsInfoLevelWhenVerboseIsEmpty() {
    assertThat(Level.INFO).isEqualTo(isVerboseLogging(buildParameterWithVerbose("")));
  }

  @Test
  void isVerboseLoggingReturnsInfoLevelWhenVerboseIsFalse() {
    assertThat(Level.INFO).isEqualTo(isVerboseLogging(buildParameterWithVerbose("false")));
  }

  @Test
  void isVerboseLoggingReturnsInfoLevelWhenVerboseIsNotParsable() {
    assertThat(Level.INFO).isEqualTo(isVerboseLogging(buildParameterWithVerbose("%&#")));
  }

  @Test
  void isVerboseLoggingReturnsDebugLevelWhenVerboseIsTrue() {
    assertThat(Level.DEBUG).isEqualTo(isVerboseLogging(buildParameterWithVerbose("true")));
  }

  Map<String, String> buildParameterWithVerbose(final String verboseValue) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(CommonStepPropertyNames.VERBOSE_LOGGING, verboseValue);
    return parameters;
  }
}
