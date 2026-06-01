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
    final Map<String, String> p = new HashMap<>();
    p.put(ConnectionPropertyNames.SERVER_URL, "https://octopus.example.com");
    p.put(ConnectionPropertyNames.API_KEY, "API-XXXXXXXXXXXXXXXXXXXXXXXX");
    p.put(ConnectionPropertyNames.VERSION, "3.0+");
    return p;
  }

  private List<String> invalidKeys(final Map<String, String> p) {
    return processor.process(p).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  @Test
  void validPropertiesProduceNoErrors() {
    assertThat(processor.process(validProps())).isEmpty();
  }

  @Test
  void missingServerUrlIsInvalid() {
    final Map<String, String> p = validProps();
    p.remove(ConnectionPropertyNames.SERVER_URL);
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void nonHttpServerUrlIsInvalid() {
    final Map<String, String> p = validProps();
    p.put(ConnectionPropertyNames.SERVER_URL, "ftp://octopus.example.com");
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void malformedServerUrlIsInvalid() {
    final Map<String, String> p = validProps();
    p.put(ConnectionPropertyNames.SERVER_URL, "not a url");
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void missingApiKeyIsInvalid() {
    final Map<String, String> p = validProps();
    p.remove(ConnectionPropertyNames.API_KEY);
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.API_KEY);
  }

  @Test
  void unknownVersionIsInvalid() {
    final Map<String, String> p = validProps();
    p.put(ConnectionPropertyNames.VERSION, "banana");
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.VERSION);
  }
}
