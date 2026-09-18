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

  protected static Optional<String> getReleaseVersion(String output) {
    return readString(asJsonObject(output), "Version");
  }

  protected static Optional<String> getReleaseId(String output) {
    return readString(asJsonObject(output), "ID");
  }

  protected static Optional<String> getSpaceId(String output) {
    return readString(asJsonObject(output), "Id");
  }

  protected static String getServerTaskId(String output) {
    JsonArray json = JSON_PARSER.parse(output).getAsJsonArray();
    return json.get(0).getAsJsonObject().get("ServerTaskId").getAsString();
  }

  protected static boolean isCreateReleaseCommand(String output) {
    return output != null && output.contains("Version");
  }

  protected static boolean isDeployReleaseCommand(String output) {
    return output != null && output.contains("ServerTaskId");
  }

  /**
   * A space's own name and description are whatever its owner typed, so recognising {@code space
   * view}'s response goes by structure instead: it is the only single object the plugin asks for
   * that carries both a space id and that space's task queue state.
   */
  protected static boolean isSpaceViewCommand(String output) {
    JsonObject json = asJsonObject(output);
    return json != null
        && json.has("TaskQueue")
        && readString(json, "Id").filter(CommandUtils::isSpaceId).isPresent();
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

  private static JsonObject asJsonObject(String output) {
    if (StringUtils.isBlank(output)) {
      return null;
    }

    try {
      JsonElement json = JSON_PARSER.parse(output);
      return json.isJsonObject() ? json.getAsJsonObject() : null;
    } catch (JsonParseException e) {
      return null;
    }
  }

  protected static boolean isRunbookRunCommand(String output) {
    return output != null && output.contains("RunbookRunId");
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
