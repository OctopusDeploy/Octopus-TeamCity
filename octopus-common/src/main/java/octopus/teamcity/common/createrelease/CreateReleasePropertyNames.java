package octopus.teamcity.common.createrelease;

public class CreateReleasePropertyNames {

  public static final String PROJECT_NAME = "octopus_cr_project_name";
  public static final String PACKAGE_VERSION = "octopus_cr_package_version";
  public static final String RELEASE_VERSION = "octopus_cr_release_version";
  public static final String CHANNEL_NAME = "octopus_cr_channel_name";
  public static final String PACKAGES = "octopus_cr_packages";

  public String getProjectNamePropertyName() {
    return PROJECT_NAME;
  }

  public String getPackageVersionPropertyName() {
    return PACKAGE_VERSION;
  }

  public String getReleaseVersionPropertyName() {
    return RELEASE_VERSION;
  }

  public String getChannelNamePropertyName() {
    return CHANNEL_NAME;
  }

  public String getPackagesPropertyName() {
    return PACKAGES;
  }
}
