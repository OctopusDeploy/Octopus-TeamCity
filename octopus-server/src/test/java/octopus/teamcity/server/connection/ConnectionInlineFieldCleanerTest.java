package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class ConnectionInlineFieldCleanerTest {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  @Test
  void removesInlineCredentialFieldsWhenConnectionSelected() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");
    properties.put(CONSTANTS.getSpaceName(), "Default");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties)
        .doesNotContainKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
    assertThat(properties).containsEntry(CONSTANTS.getSpaceName(), "Default");
    assertThat(properties).containsEntry(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
  }

  @Test
  void leavesInlineFieldsWhenNoConnectionSelected() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties)
        .containsKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
  }

  @Test
  void leavesInlineFieldsWhenConnectionIdIsBlank() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "   ");
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties)
        .containsKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
  }

  @Test
  void toleratesConnectionSelectedWithNoInlineFields() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties).containsOnlyKeys(CONSTANTS.getConnectionIdKey());
  }
}
