package octopus.teamcity.agent.cli;

import java.util.Optional;
import java.util.regex.Matcher;
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
  private static final Pattern WHOLE_SECONDS = Pattern.compile("\\d+");
  private static final Pattern TIMESPAN =
      Pattern.compile("(?:(\\d+)\\.)?(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2})(?:\\.(\\d{1,7}))?)?");
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

  /**
   * The step's timeout in whole seconds, as {@code task wait --timeout} expects. The form asks for
   * a .NET timespan ({@code [d.]hh:mm[:ss[.fffffff]]}); a plain number is taken as seconds.
   */
  protected static String toTimeoutSeconds(String timeout) {
    String trimmed = timeout.trim();
    if (WHOLE_SECONDS.matcher(trimmed).matches()) {
      return trimmed;
    }

    Matcher timespan = TIMESPAN.matcher(trimmed);
    if (!timespan.matches()) {
      throw new IllegalArgumentException(invalidTimeoutMessage(timeout));
    }

    long days = timespan.group(1) == null ? 0 : Long.parseLong(timespan.group(1));
    long hours = Long.parseLong(timespan.group(2));
    long minutes = Long.parseLong(timespan.group(3));
    long seconds = timespan.group(4) == null ? 0 : Long.parseLong(timespan.group(4));
    boolean hasFraction = timespan.group(5) != null && !timespan.group(5).matches("0+");
    if (hours > 23 || minutes > 59 || seconds > 59) {
      throw new IllegalArgumentException(invalidTimeoutMessage(timeout));
    }

    long totalSeconds =
        days * 86_400 + hours * 3_600 + minutes * 60 + seconds + (hasFraction ? 1 : 0);
    return Long.toString(totalSeconds);
  }

  private static String invalidTimeoutMessage(String timeout) {
    return "The time to wait '"
        + timeout
        + "' is not a timespan such as 00:10:00 or a number of seconds.";
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
