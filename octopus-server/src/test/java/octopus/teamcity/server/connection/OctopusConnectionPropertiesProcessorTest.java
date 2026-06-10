package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

class OctopusConnectionPropertiesProcessorTest {
  private final OctopusConnectionPropertiesProcessor processor =
      new OctopusConnectionPropertiesProcessor();

  private Map<String, String> validProps() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(ConnectionPropertyNames.SERVER_URL, "https://octopus.example.com");
    properties.put(ConnectionPropertyNames.API_KEY, "API-XXXXXXXXXXXXXXXXXXXXXXXX");
    properties.put(ConnectionPropertyNames.VERSION, "3.0+");
    return properties;
  }

  private List<String> invalidKeys(final Map<String, String> properties) {
    return processor.process(properties).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  @Test
  void validPropertiesProduceNoErrors() {
    assertThat(processor.process(validProps())).isEmpty();
  }

  @Test
  void missingServerUrlIsInvalid() {
    final Map<String, String> properties = validProps();
    properties.remove(ConnectionPropertyNames.SERVER_URL);
    assertThat(invalidKeys(properties)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void nonHttpServerUrlIsInvalid() {
    final Map<String, String> properties = validProps();
    properties.put(ConnectionPropertyNames.SERVER_URL, "ftp://octopus.example.com");
    assertThat(invalidKeys(properties)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void malformedServerUrlIsInvalid() {
    final Map<String, String> properties = validProps();
    properties.put(ConnectionPropertyNames.SERVER_URL, "not a url");
    assertThat(invalidKeys(properties)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void missingApiKeyIsInvalid() {
    final Map<String, String> properties = validProps();
    properties.remove(ConnectionPropertyNames.API_KEY);
    assertThat(invalidKeys(properties)).contains(ConnectionPropertyNames.API_KEY);
  }

  @Test
  void unknownVersionIsInvalid() {
    final Map<String, String> properties = validProps();
    properties.put(ConnectionPropertyNames.VERSION, "banana");
    assertThat(invalidKeys(properties)).contains(ConnectionPropertyNames.VERSION);
  }

  @Test
  void httpServerUrlIsValid() {
    final Map<String, String> properties = validProps();
    properties.put(ConnectionPropertyNames.SERVER_URL, "http://octopus.example.com");
    assertThat(invalidKeys(properties)).doesNotContain(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void whitespaceApiKeyIsInvalid() {
    final Map<String, String> properties = validProps();
    properties.put(ConnectionPropertyNames.API_KEY, "   ");
    assertThat(invalidKeys(properties)).contains(ConnectionPropertyNames.API_KEY);
  }
}
