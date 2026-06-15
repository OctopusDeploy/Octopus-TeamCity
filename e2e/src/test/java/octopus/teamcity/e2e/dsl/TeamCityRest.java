package octopus.teamcity.e2e.dsl;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal TeamCity REST client for the integration tests. Stateless (no cookies) + Basic auth via
 * the super-user token, which is sufficient for the REST API and needs no CSRF token.
 */
public final class TeamCityRest {

  private final String baseUrl;
  private final String authHeader;

  public TeamCityRest(final String baseUrl, final String authHeader) {
    this.baseUrl = baseUrl;
    this.authHeader = authHeader;
  }

  // --- low-level ----------------------------------------------------------

  /** Authenticated request to an absolute URL; {@code accept} sets the Accept header. */
  private Http.Response request(
      final String method,
      final String url,
      final String contentType,
      final String accept,
      final String body)
      throws IOException {
    final Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Authorization", authHeader);
    headers.put("Accept", accept);
    return Http.send(method, url, headers, contentType, body);
  }

  private Http.Response send(
      final String method, final String path, final String contentType, final String body)
      throws Exception {
    final Http.Response resp =
        request(method, baseUrl + path, contentType, "application/json", body);
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException(
          method + " " + path + " -> " + resp.statusCode() + ": " + resp.body());
    }
    return resp;
  }

  private static String jsonField(final String json, final String field) {
    final Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
    if (!m.find()) {
      throw new IllegalStateException("field '" + field + "' not found in: " + json);
    }
    return m.group(1);
  }

  /**
   * Polls the authenticated REST {@code /server} endpoint until it returns 200. The super-user
   * token is logged a moment before REST is actually serving requests (startup briefly
   * 302-redirects authenticated calls), so callers must wait for a real 200 before provisioning.
   */
  public void waitUntilReady(final Duration timeout) throws Exception {
    final long deadline = System.currentTimeMillis() + timeout.toMillis();
    int lastStatus = -1;
    while (System.currentTimeMillis() < deadline) {
      final Http.Response resp =
          request("GET", baseUrl + "/httpAuth/app/rest/server", null, "application/json", null);
      lastStatus = resp.statusCode();
      if (lastStatus == 200) {
        return;
      }
      TimeUnit.SECONDS.sleep(3);
    }
    throw new IllegalStateException(
        "TeamCity REST API did not become ready within "
            + timeout
            + " (last status "
            + lastStatus
            + ")");
  }

  // --- provisioning -------------------------------------------------------

  public void createProject(final String id, final String name) throws Exception {
    createProject(id, name, "_Root");
  }

  /** Creates a project under an explicit parent (for inheritance tests). */
  public void createProject(final String id, final String name, final String parentId)
      throws Exception {
    send(
        "POST",
        "/httpAuth/app/rest/projects",
        "application/json",
        "{\"id\":\""
            + id
            + "\",\"name\":\""
            + name
            + "\",\"parentProject\":{\"id\":\""
            + parentId
            + "\"}}");
  }

  /**
   * Creates an OctopusConnection OAuth connection as a projectFeature; returns the generated
   * connection id (PROJECT_EXT_*).
   */
  public String createOctopusConnection(
      final String projectId,
      final String displayName,
      final String octopusUrl,
      final String apiKey,
      final String version,
      final String space)
      throws Exception {
    final String json =
        "{"
            + "\"type\":\"OAuthProvider\",\"properties\":{\"property\":["
            + prop("providerType", "OctopusConnection")
            + ","
            + prop("displayName", displayName)
            + ","
            + prop("octopus_host", octopusUrl)
            + ","
            + prop("secure:octopus_apikey", apiKey)
            + ","
            + prop("octopus_version", version)
            + ","
            + prop("octopus_space_name", space)
            + "]}}";
    final Http.Response resp =
        send(
            "POST",
            "/httpAuth/app/rest/projects/" + projectId + "/projectFeatures",
            "application/json",
            json);
    return jsonField(resp.body(), "id");
  }

  private static String prop(final String name, final String value) {
    return "{\"name\":\"" + name + "\",\"value\":\"" + value.replace("\"", "\\\"") + "\"}";
  }

  public void createBuildType(final String id, final String name, final String projectId)
      throws Exception {
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes",
        "application/json",
        "{\"id\":\""
            + id
            + "\",\"name\":\""
            + name
            + "\",\"project\":{\"id\":\""
            + projectId
            + "\"}}");
  }

  /** Adds a Build Information (octopus.metadata) step referencing a connection. */
  public void addBuildInfoStepUsingConnection(
      final String buildTypeId,
      final String connectionId,
      final String packageId,
      final String packageVersion)
      throws Exception {
    final String json =
        "{"
            + "\"name\":\"Publish build information\",\"type\":\"octopus.metadata\","
            + "\"properties\":{\"property\":["
            + prop("octopus_connection_id", connectionId)
            + ","
            + prop("octopus_packageid", packageId)
            + ","
            + prop("octopus_packageversion", packageVersion)
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /**
   * Adds an empty Create release (octopus.create.release) step; enough for the edit page to render.
   * Returns the generated runner id (e.g. {@code RUNNER_1}).
   */
  public String addCreateReleaseStep(final String buildTypeId) throws Exception {
    final String json =
        "{\"name\":\"Create release\",\"type\":\"octopus.create.release\","
            + "\"properties\":{\"property\":[]}}";
    final Http.Response resp =
        send(
            "POST",
            "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
            "application/json",
            json);
    return jsonField(resp.body(), "id");
  }

  /** Adds an empty step of the given runner type; returns the generated runner id (RUNNER_n). */
  public String addEmptyStep(final String buildTypeId, final String type, final String name)
      throws Exception {
    final Http.Response resp =
        send(
            "POST",
            "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
            "application/json",
            "{\"name\":\""
                + name
                + "\",\"type\":\""
                + type
                + "\",\"properties\":{\"property\":[]}}");
    return jsonField(resp.body(), "id");
  }

  /** Adds a built-in command-line step that runs the given shell script. */
  public void addCommandLineStep(final String buildTypeId, final String name, final String script)
      throws Exception {
    final String json =
        "{\"name\":\""
            + name
            + "\",\"type\":\"simpleRunner\",\"properties\":{\"property\":["
            + prop("use.custom.script", "true")
            + ","
            + prop("script.content", script)
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /** Adds a Pack (octopus.pack.package) step. Pack is purely local — no Octopus server needed. */
  public void addPackStep(
      final String buildTypeId,
      final String packageId,
      final String packageFormat,
      final String packageVersion,
      final String sourcePath,
      final String outputPath)
      throws Exception {
    final String json =
        "{\"name\":\"Pack\",\"type\":\"octopus.pack.package\",\"properties\":{\"property\":["
            + prop("octopus_version", "3.0+")
            + ","
            + prop("octopus_packageid", packageId)
            + ","
            + prop("octopus_packageformat", packageFormat)
            + ","
            + prop("octopus_packageversion", packageVersion)
            + ","
            + prop("octopus_packagesourcepath", sourcePath)
            + ","
            + prop("octopus_packageoutputpath", outputPath)
            + ","
            + prop("octopus_publishartifacts", "true")
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /** Adds a Push (octopus.push.package) step that references a connection. */
  public void addPushStepUsingConnection(
      final String buildTypeId, final String connectionId, final String packagePaths)
      throws Exception {
    final String json =
        "{\"name\":\"Push package\",\"type\":\"octopus.push.package\",\"properties\":{\"property\":["
            + prop("octopus_connection_id", connectionId)
            + ","
            + prop("octopus_packagepaths", packagePaths)
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /**
   * Adds a Build Information (octopus.metadata) step with inline URL/API key (no connection) — the
   * backward-compatible manual configuration.
   */
  public void addBuildInfoStepInline(
      final String buildTypeId,
      final String octopusUrl,
      final String apiKey,
      final String packageId,
      final String packageVersion)
      throws Exception {
    final String json =
        "{\"name\":\"Publish build information\",\"type\":\"octopus.metadata\","
            + "\"properties\":{\"property\":["
            + prop("octopus_host", octopusUrl)
            + ","
            + prop("secure:octopus_apikey", apiKey)
            + ","
            + prop("octopus_version", "3.0+")
            + ","
            + prop("octopus_packageid", packageId)
            + ","
            + prop("octopus_packageversion", packageVersion)
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /** Adds a Create release (octopus.create.release) step referencing a connection. */
  public void addCreateReleaseStepUsingConnection(
      final String buildTypeId,
      final String connectionId,
      final String projectName,
      final String releaseNumber)
      throws Exception {
    final String json =
        "{\"name\":\"Create release\",\"type\":\"octopus.create.release\","
            + "\"properties\":{\"property\":["
            + prop("octopus_connection_id", connectionId)
            + ","
            + prop("octopus_project_name", projectName)
            + ","
            + prop("octopus_releasenumber", releaseNumber)
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /**
   * Adds a Deploy release (octopus.deploy.release) step referencing a connection. Waits for the
   * deployment so the build fails if the deployment fails.
   */
  public void addDeployReleaseStepUsingConnection(
      final String buildTypeId,
      final String connectionId,
      final String projectName,
      final String releaseNumber,
      final String deployTo)
      throws Exception {
    final String json =
        "{\"name\":\"Deploy release\",\"type\":\"octopus.deploy.release\","
            + "\"properties\":{\"property\":["
            + prop("octopus_connection_id", connectionId)
            + ","
            + prop("octopus_project_name", projectName)
            + ","
            + prop("octopus_releasenumber", releaseNumber)
            + ","
            + prop("octopus_deployto", deployTo)
            + ","
            + prop("octopus_waitfordeployments", "true")
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /**
   * Adds a Promote release (octopus.promote.release) step referencing a connection. Waits for the
   * deployment so the build fails if the promotion fails.
   */
  public void addPromoteReleaseStepUsingConnection(
      final String buildTypeId,
      final String connectionId,
      final String projectName,
      final String promoteFrom,
      final String deployTo)
      throws Exception {
    final String json =
        "{\"name\":\"Promote release\",\"type\":\"octopus.promote.release\","
            + "\"properties\":{\"property\":["
            + prop("octopus_connection_id", connectionId)
            + ","
            + prop("octopus_project_name", projectName)
            + ","
            + prop("octopus_promotefrom", promoteFrom)
            + ","
            + prop("octopus_deployto", deployTo)
            + ","
            + prop("octopus_waitfordeployments", "true")
            + "]}}";
    send(
        "POST",
        "/httpAuth/app/rest/buildTypes/" + buildTypeId + "/steps",
        "application/json",
        json);
  }

  /** Lists the names of a finished build's artifacts (top-level children). */
  public String listBuildArtifacts(final String buildId) throws Exception {
    return send(
            "GET", "/httpAuth/app/rest/builds/id:" + buildId + "/artifacts/children", null, null)
        .body();
  }

  /** Queues a build; returns its id. */
  public String triggerBuild(final String buildTypeId) throws Exception {
    final Http.Response resp =
        send(
            "POST",
            "/httpAuth/app/rest/buildQueue",
            "application/json",
            "{\"buildType\":{\"id\":\"" + buildTypeId + "\"}}");
    final Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(resp.body());
    if (!m.find()) {
      throw new IllegalStateException("no build id in: " + resp.body());
    }
    return m.group(1);
  }

  /** Polls until the build is finished; returns its status (e.g. SUCCESS, FAILURE). */
  public String waitForBuildFinished(final String buildId, final Duration timeout)
      throws Exception {
    final long deadline = System.currentTimeMillis() + timeout.toMillis();
    while (System.currentTimeMillis() < deadline) {
      final String body = send("GET", "/httpAuth/app/rest/builds/id:" + buildId, null, null).body();
      if (body.contains("\"state\":\"finished\"")) {
        final Matcher m = Pattern.compile("\"status\"\\s*:\\s*\"([A-Z]+)\"").matcher(body);
        return m.find() ? m.group(1) : "UNKNOWN";
      }
      TimeUnit.SECONDS.sleep(5);
    }
    throw new IllegalStateException("Build " + buildId + " did not finish within " + timeout);
  }

  public String downloadBuildLog(final String buildId) throws Exception {
    return send("GET", "/httpAuth/downloadBuildLog.html?buildId=" + buildId, null, null).body();
  }

  // --- agents -------------------------------------------------------------

  public void authorizeAllAgents() throws Exception {
    final long deadline = System.currentTimeMillis() + Duration.ofMinutes(3).toMillis();
    while (System.currentTimeMillis() < deadline) {
      final String body =
          send("GET", "/httpAuth/app/rest/agents?locator=authorized:false", null, null).body();
      final Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body);
      if (m.find()) {
        final String agentId = m.group(1);
        // The /authorized field endpoint replies in text/plain, so we must accept text/plain —
        // asking for application/json gets a 406. Verify the status so a failed authorization
        // doesn't pass silently (otherwise the agent stays unauthorized and never goes idle).
        final Http.Response authorized =
            request(
                "PUT",
                baseUrl + "/httpAuth/app/rest/agents/id:" + agentId + "/authorized",
                "text/plain",
                "text/plain",
                "true");
        if (authorized.statusCode() < 200 || authorized.statusCode() >= 300) {
          throw new IllegalStateException(
              "Authorizing agent "
                  + agentId
                  + " failed: "
                  + authorized.statusCode()
                  + ": "
                  + authorized.body());
        }
        return;
      }
      TimeUnit.SECONDS.sleep(5);
    }
    throw new IllegalStateException("No unauthorized agent appeared");
  }

  public void waitForAgentIdle() throws Exception {
    final long deadline = System.currentTimeMillis() + Duration.ofMinutes(5).toMillis();
    while (System.currentTimeMillis() < deadline) {
      final String body =
          send(
                  "GET",
                  "/httpAuth/app/rest/agents?locator=authorized:true,connected:true,enabled:true",
                  null,
                  null)
              .body();
      if (body.contains("\"id\"")) {
        return;
      }
      TimeUnit.SECONDS.sleep(5);
    }
    throw new IllegalStateException("No idle agent appeared");
  }
}
