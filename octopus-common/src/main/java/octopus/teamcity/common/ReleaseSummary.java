package octopus.teamcity.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * What a step records about the release it created, so the build overview can link to it after the
 * build is over. It travels as a hidden build artifact, which means it is served by TeamCity's own
 * artifact machinery and is cleaned up along with the build that produced it.
 */
public final class ReleaseSummary {
  public static final String ARTIFACT_DIRECTORY = ".teamcity/octopus";

  private static final String ARTIFACT_PREFIX = "release-";
  private static final String ARTIFACT_SUFFIX = ".properties";

  private static final String URL_KEY = "url";
  private static final String VERSION_KEY = "version";

  private final String url;
  private final String version;

  /**
   * One file per step, so a build that creates several releases records all of them rather than
   * having each step overwrite the last.
   */
  public static String artifactNameFor(final String stepId) {
    return ARTIFACT_PREFIX + stepId.replaceAll("[^A-Za-z0-9_.-]", "_") + ARTIFACT_SUFFIX;
  }

  public static boolean isSummary(final String artifactName) {
    return artifactName.startsWith(ARTIFACT_PREFIX) && artifactName.endsWith(ARTIFACT_SUFFIX);
  }

  public ReleaseSummary(final String url, final String version) {
    this.url = url;
    this.version = version;
  }

  public String getUrl() {
    return url;
  }

  public String getVersion() {
    return version;
  }

  public void writeTo(final OutputStream destination) throws IOException {
    final Properties properties = new Properties();
    properties.setProperty(URL_KEY, url);
    if (version != null) {
      properties.setProperty(VERSION_KEY, version);
    }
    properties.store(destination, "The Octopus Deploy release this build created");
  }

  /** Empty for anything that does not name a release, so a stale or truncated file is ignored. */
  public static Optional<ReleaseSummary> readFrom(final InputStream source) throws IOException {
    final Properties properties = new Properties();
    properties.load(source);

    final String url = properties.getProperty(URL_KEY);
    if (url == null || url.trim().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(new ReleaseSummary(url, properties.getProperty(VERSION_KEY)));
  }
}
