package octopus.teamcity.agent.cli;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import octopus.teamcity.common.OverwriteMode;
import org.apache.commons.lang3.StringUtils;

public class CommandUtils {
  private static final JsonParser JSON_PARSER = new JsonParser();

  protected static String getReleaseVersion(String output) {
    JsonObject json = JSON_PARSER.parse(output).getAsJsonObject();
    return json.get("Version").getAsString();
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
