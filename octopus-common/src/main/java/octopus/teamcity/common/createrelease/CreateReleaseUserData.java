package octopus.teamcity.common.createrelease;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.util.text.StringUtil;
import octopus.teamcity.common.BaseUserData;
import octopus.teamcity.common.commonstep.CommonStepUserData;

public class CreateReleaseUserData extends CommonStepUserData {

  private static final CreateReleasePropertyNames KEYS = new CreateReleasePropertyNames();

  public CreateReleaseUserData(final Map<String, String> params) {
    super(params);
  }

  public String getProjectName() {
    return fetchRaw(KEYS.getProjectNamePropertyName());
  }

  public String getPackageVersion() {
    return fetchRaw(KEYS.getPackageVersionPropertyName());
  }

  public Optional<String> getReleaseVersion() {
    return Optional.ofNullable(fetchRaw(KEYS.getReleaseVersionPropertyName()));
  }

  public Optional<String> getChannelName() {
    return Optional.ofNullable(fetchRaw(KEYS.getChannelNamePropertyName()));
  }

  public List<String> getPackages() {
    final String rawInput = fetchRaw(KEYS.getPackagesPropertyName());
    return StringUtil.split(rawInput, "\n");
  }
}
