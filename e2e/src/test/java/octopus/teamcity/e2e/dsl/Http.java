package octopus.teamcity.e2e.dsl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Minimal HttpURLConnection wrapper shared by the e2e DSL clients ({@link TeamCityRest}, {@link
 * OctopusProvisioning}). Redirects are not followed — TeamCity briefly 302-redirects authenticated
 * calls during startup and callers rely on seeing that.
 */
final class Http {

  private static final int CONNECT_TIMEOUT_MILLIS = (int) Duration.ofSeconds(10).toMillis();

  private Http() {}

  /** A completed HTTP response (status + body). */
  static final class Response {
    private final int statusCode;
    private final String body;

    private Response(final int statusCode, final String body) {
      this.statusCode = statusCode;
      this.body = body;
    }

    int statusCode() {
      return statusCode;
    }

    String body() {
      return body;
    }
  }

  /**
   * Sends a request and returns the raw response. {@code headers} are set as-is. A null {@code
   * body} sends no request body; when a body is present, {@code contentType} (if non-null) is its
   * Content-Type.
   */
  static Response send(
      final String method,
      final String url,
      final Map<String, String> headers,
      final String contentType,
      final String body)
      throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setInstanceFollowRedirects(false);
    connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
    connection.setRequestMethod(method);
    for (final Map.Entry<String, String> header : headers.entrySet()) {
      connection.setRequestProperty(header.getKey(), header.getValue());
    }
    if (body != null) {
      if (contentType != null) {
        connection.setRequestProperty("Content-Type", contentType);
      }
      connection.setDoOutput(true);
      try (OutputStream output = connection.getOutputStream()) {
        output.write(body.getBytes(StandardCharsets.UTF_8));
      }
    }
    final int statusCode = connection.getResponseCode();
    final InputStream stream =
        statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
    final String responseBody = stream == null ? "" : readAll(stream);
    connection.disconnect();
    return new Response(statusCode, responseBody);
  }

  private static String readAll(final InputStream stream) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      final byte[] chunk = new byte[8192];
      int bytesRead;
      while ((bytesRead = stream.read(chunk)) != -1) {
        buffer.write(chunk, 0, bytesRead);
      }
      return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
  }
}
