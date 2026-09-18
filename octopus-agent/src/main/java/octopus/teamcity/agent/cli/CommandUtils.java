package octopus.teamcity.agent.cli;

import java.util.Optional;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import octopus.teamcity.common.OverwriteMode;
import org.apache.commons.lang3.StringUtils;

public class CommandUtils {
  private static final JsonParser JSON_PARSER = new JsonParser();
  private static final Pattern SPACE_ID = Pattern.compile("Spaces-\\d+");
  private static final int LOGGED_OUTPUT_LIMIT = 500;

  protected static Optional<String> getReleaseVersion(String output) {
    return readString(asJsonObject(output), "Version");
  }

  protected static Optional<String> getReleaseId(String output) {
    return readString(asJsonObject(output), "ID");
  }

  protected static Optional<String> getSpaceId(String output) {
    return readString(asJsonObject(output), "Id");
  }

  protected static Optional<String> getServerTaskId(String output) {
    JsonArray json = asJsonArray(output);
    if (json == null || json.size() == 0 || !json.get(0).isJsonObject()) {
      return Optional.empty();
    }

    return readString(json.get(0).getAsJsonObject(), "ServerTaskId");
  }

  /** Enough of a response to tell what the CLI said instead, without flooding a build log. */
  protected static String loggableOutput(String output) {
    if (output == null) {
      return "";
    }

    String trimmed = output.trim();
    return trimmed.length() <= LOGGED_OUTPUT_LIMIT
        ? trimmed
        : trimmed.substring(0, LOGGED_OUTPUT_LIMIT) + "... (truncated)";
  }

  protected static boolean isSpaceId(String space) {
    return space != null && SPACE_ID.matcher(space.trim()).matches();
  }

  private static Optional<String> readString(JsonObject json, String field) {
    if (json == null) {
      return Optional.empty();
    }

    JsonElement value = json.get(field);
    if (value == null || !value.isJsonPrimitive()) {
      return Optional.empty();
    }

    String text = value.getAsString();
    return StringUtils.isBlank(text) ? Optional.empty() : Optional.of(text);
  }

  private static JsonArray asJsonArray(String output) {
    JsonElement json = asJson(output);
    return json != null && json.isJsonArray() ? json.getAsJsonArray() : null;
  }

  private static JsonObject asJsonObject(String output) {
    JsonElement json = asJson(output);
    return json != null && json.isJsonObject() ? json.getAsJsonObject() : null;
  }

  private static JsonElement asJson(String output) {
    if (StringUtils.isBlank(output)) {
      return null;
    }

    try {
      return JSON_PARSER.parse(output);
    } catch (JsonParseException e) {
      return null;
    }
  }

  public static String getOverwriteMode(OverwriteMode overwriteMode) {
    switch (overwriteMode) {
      case FailIfExists:
        return "fail";
      case IgnoreIfExists:
        return "ignore";
      default:
        return "overwrite";
    }
  }

  public static String getVersion(String releaseNumber, String autoCreatedReleaseNumber) {
    if (StringUtils.isNotBlank(releaseNumber)) {
      return releaseNumber;
    }
    return StringUtils.isNotBlank(autoCreatedReleaseNumber) ? autoCreatedReleaseNumber : "";
  }
}
