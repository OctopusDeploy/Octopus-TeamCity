package octopus.teamcity.e2e.dsl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Resolves the teamcity-oidc-plugin zip the e2e stack installs alongside our plugin.
 *
 * <p>By default it downloads the <em>latest</em> public release from GitHub on demand, caching it
 * by asset name (i.e. by version) under the temp dir so repeated runs reuse it. Set the {@code
 * OIDC_PLUGIN_ZIP} environment variable to a local zip to skip the download (offline runs, or to
 * pin a specific version).
 */
public final class OidcPluginFixture {
  private static final String LATEST_RELEASE_API =
      "https://api.github.com/repos/OctopusDeploy/teamcity-oidc-plugin/releases/latest";
  private static final String USER_AGENT = "octopus-teamcity-e2e";
  private static final int CONNECT_TIMEOUT_MILLIS = (int) Duration.ofSeconds(15).toMillis();
  private static final int READ_TIMEOUT_MILLIS = (int) Duration.ofMinutes(2).toMillis();

  private OidcPluginFixture() {}

  /** Local path to the OIDC plugin zip, downloading the latest release on demand if needed. */
  public static Path resolve() throws IOException {
    final String override = System.getenv("OIDC_PLUGIN_ZIP");
    if (override != null && !override.isEmpty()) {
      final Path local = Paths.get(override);
      if (!Files.isRegularFile(local)) {
        throw new IllegalStateException(
            "OIDC_PLUGIN_ZIP does not point at an existing file: " + override);
      }
      return local;
    }

    final Asset asset = latestZipAsset();
    final Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "octopus-tc-oidc-plugin");
    Files.createDirectories(cacheDir);
    final Path cached = cacheDir.resolve(asset.name);
    if (Files.isRegularFile(cached) && Files.size(cached) > 0) {
      return cached;
    }
    download(asset.downloadUrl, cached);
    return cached;
  }

  private static Asset latestZipAsset() throws IOException {
    final HttpURLConnection connection = open(LATEST_RELEASE_API);
    connection.setRequestProperty("Accept", "application/vnd.github+json");
    final String token = System.getenv("GITHUB_TOKEN");
    if (token != null && !token.isEmpty()) {
      connection.setRequestProperty("Authorization", "Bearer " + token);
    }
    final int status = connection.getResponseCode();
    if (status != 200) {
      throw new IOException(
          "GitHub API "
              + LATEST_RELEASE_API
              + " returned "
              + status
              + ": "
              + errorBody(connection));
    }
    final String body = readUtf8(connection.getInputStream());
    final JsonObject release = JsonParser.parseString(body).getAsJsonObject();
    final JsonArray assets = release.getAsJsonArray("assets");
    for (final JsonElement element : assets) {
      final JsonObject asset = element.getAsJsonObject();
      final String name = asset.get("name").getAsString();
      if (name.endsWith(".zip")) {
        return new Asset(name, asset.get("browser_download_url").getAsString());
      }
    }
    throw new IllegalStateException(
        "No .zip asset on the latest teamcity-oidc-plugin release ("
            + release.get("tag_name")
            + ")");
  }

  private static void download(final String url, final Path destination) throws IOException {
    final HttpURLConnection connection = open(url);
    connection.setInstanceFollowRedirects(true); // release asset URLs 302 to a storage host
    final int status = connection.getResponseCode();
    if (status != 200) {
      throw new IOException("Downloading " + url + " returned " + status);
    }
    final Path partial = Files.createTempFile(destination.getParent(), "oidc-plugin", ".part");
    try (InputStream in = connection.getInputStream()) {
      Files.copy(in, partial, StandardCopyOption.REPLACE_EXISTING);
    }
    Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
  }

  private static HttpURLConnection open(final String url) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestProperty("User-Agent", USER_AGENT);
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    connection.setReadTimeout(READ_TIMEOUT_MILLIS);
    return connection;
  }

  private static String readUtf8(final InputStream in) throws IOException {
    try (InputStream stream = in) {
      final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
      final byte[] chunk = new byte[8192];
      int read;
      while ((read = stream.read(chunk)) != -1) {
        buffer.write(chunk, 0, read);
      }
      return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private static String errorBody(final HttpURLConnection connection) {
    try (InputStream error = connection.getErrorStream()) {
      return error == null ? "(no body)" : readUtf8(error);
    } catch (final IOException e) {
      return "(unreadable error body: " + e.getMessage() + ")";
    }
  }

  private static final class Asset {
    private final String name;
    private final String downloadUrl;

    private Asset(final String name, final String downloadUrl) {
      this.name = name;
      this.downloadUrl = downloadUrl;
    }
  }
}
