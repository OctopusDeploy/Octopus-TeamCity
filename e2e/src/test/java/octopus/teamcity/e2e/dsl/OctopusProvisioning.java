package octopus.teamcity.e2e.dsl;

import com.octopus.sdk.api.EnvironmentApi;
import com.octopus.sdk.api.ProjectApi;
import com.octopus.sdk.api.ProjectGroupApi;
import com.octopus.sdk.domain.Project;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.environment.EnvironmentResource;
import com.octopus.sdk.model.project.ProjectResource;
import com.octopus.sdk.model.space.SpaceHome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Provisions Octopus state the deployment-lifecycle e2e tests need (project with a deployment
 * process, environments). The SDK 0.0.6 can create projects/lifecycles/groups/environments but
 * cannot author a deployment process, so the (single, run-on-server) script step is added via the
 * Octopus REST API directly.
 */
public final class OctopusProvisioning {

  private OctopusProvisioning() {}

  /**
   * Creates an Octopus project with a dedicated lifecycle and a single inline-script step that runs
   * on the Octopus Server — so releases are viable and deployable without any deployment targets.
   *
   * <p>The lifecycle gets one phase per entry in {@code deployableEnvironmentIds}, in order, each
   * permitting that environment as an optional deployment target, so a release can deploy straight
   * to the test's environment(s) (and promote through them). It deliberately does NOT reuse the
   * shared Default Lifecycle: that lifecycle sequences a release through every environment on the
   * (suite-wide shared) Octopus in creation order, so environments from other tests block this
   * test's deploy. SDK 0.0.6 can't author a lifecycle's phases, so it is created via the REST API
   * (as the deployment process below also is). Pass an empty list for a project whose releases are
   * only created, never deployed.
   */
  public static void createProjectWithServerScriptStep(
      final OctopusClient client,
      final SpaceHome spaceHome,
      final String octopusBaseUrl,
      final String apiKey,
      final String projectName,
      final List<String> deployableEnvironmentIds)
      throws Exception {
    final String lifecycleId =
        createLifecycleWithPhases(
            octopusBaseUrl,
            apiKey,
            spaceHome.getLifecyclesLink(),
            projectName + "-lifecycle",
            deployableEnvironmentIds);
    final String projectGroupId =
        ProjectGroupApi.create(client, spaceHome).getAll().get(0).getProperties().getId();
    // ProjectResource(name, lifecycleId, projectGroupId).
    final Project created =
        ProjectApi.create(client, spaceHome)
            .create(new ProjectResource(projectName, lifecycleId, projectGroupId));

    String deploymentProcessId = created.getProperties().getDeploymentProcessId();
    if (deploymentProcessId == null) {
      deploymentProcessId = "deploymentprocess-" + created.getProperties().getId();
    }
    addInlineScriptStep(
        octopusBaseUrl, apiKey, spaceHome.getDeploymentProcessesLink() + "/" + deploymentProcessId);
  }

  /** Creates an environment; returns its id (e.g. {@code Environments-1}). */
  public static String createEnvironment(
      final OctopusClient client, final SpaceHome spaceHome, final String name) throws Exception {
    return EnvironmentApi.create(client, spaceHome)
        .create(new EnvironmentResource(name))
        .getProperties()
        .getId();
  }

  /**
   * Creates a lifecycle (via REST — the SDK can't author phases) with one phase per environment, in
   * order, each permitting that environment as an optional deployment target. Returns its id.
   */
  private static String createLifecycleWithPhases(
      final String octopusBaseUrl,
      final String apiKey,
      final String lifecyclesLink,
      final String name,
      final List<String> environmentIds)
      throws Exception {
    final JsonArray phases = new JsonArray();
    int phaseNumber = 1;
    for (final String environmentId : environmentIds) {
      final JsonArray optionalTargets = new JsonArray();
      optionalTargets.add(environmentId);
      final JsonObject phase = new JsonObject();
      phase.addProperty("Name", "Phase " + phaseNumber++);
      phase.add("OptionalDeploymentTargets", optionalTargets);
      phase.add("AutomaticDeploymentTargets", new JsonArray());
      phases.add(phase);
    }
    final JsonObject lifecycle = new JsonObject();
    lifecycle.addProperty("Name", name);
    lifecycle.add("Phases", phases);

    final Response resp =
        octopusRequest("POST", octopusBaseUrl + lifecyclesLink, apiKey, lifecycle.toString());
    if (resp.statusCode < 200 || resp.statusCode >= 300) {
      throw new IllegalStateException("create lifecycle -> " + resp.statusCode + ": " + resp.body);
    }
    return JsonParser.parseString(resp.body).getAsJsonObject().get("Id").getAsString();
  }

  private static void addInlineScriptStep(
      final String octopusBaseUrl, final String apiKey, final String deploymentProcessLink)
      throws Exception {
    // Links may carry a URI template suffix (e.g. "{?...}"); strip it.
    final String path =
        deploymentProcessLink.contains("{")
            ? deploymentProcessLink.substring(0, deploymentProcessLink.indexOf('{'))
            : deploymentProcessLink;
    final String url = octopusBaseUrl + path;

    final Response get = octopusRequest("GET", url, apiKey, null);
    if (get.statusCode != 200) {
      throw new IllegalStateException(
          "GET deployment process -> " + get.statusCode + ": " + get.body);
    }

    final JsonObject process = JsonParser.parseString(get.body).getAsJsonObject();
    process.add("Steps", scriptStep());

    final Response put = octopusRequest("PUT", url, apiKey, process.toString());
    if (put.statusCode < 200 || put.statusCode >= 300) {
      throw new IllegalStateException(
          "PUT deployment process -> " + put.statusCode + ": " + put.body);
    }
  }

  /** Holds a completed HTTP response (status + body). */
  private static final class Response {
    private final int statusCode;
    private final String body;

    private Response(final int statusCode, final String body) {
      this.statusCode = statusCode;
      this.body = body;
    }
  }

  private static Response octopusRequest(
      final String method, final String url, final String apiKey, final String body)
      throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod(method);
    connection.setRequestProperty("X-Octopus-ApiKey", apiKey);
    if (body != null) {
      connection.setRequestProperty("Content-Type", "application/json");
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

  private static JsonArray scriptStep() {
    final JsonObject props = new JsonObject();
    // Run on the Octopus Server so the step needs no deployment targets / target roles.
    props.addProperty("Octopus.Action.RunOnServer", "true");
    props.addProperty("Octopus.Action.Script.ScriptSource", "Inline");
    props.addProperty("Octopus.Action.Script.Syntax", "Bash");
    props.addProperty("Octopus.Action.Script.ScriptBody", "echo Hello from the e2e deployment");

    final JsonObject action = new JsonObject();
    action.addProperty("Name", "Run a script");
    action.addProperty("ActionType", "Octopus.Script");
    action.add("Properties", props);
    action.add("Environments", new JsonArray());
    action.add("ExcludedEnvironments", new JsonArray());
    action.add("Channels", new JsonArray());
    action.add("TenantTags", new JsonArray());

    final JsonArray actions = new JsonArray();
    actions.add(action);

    final JsonObject step = new JsonObject();
    step.addProperty("Name", "Run a script");
    step.addProperty("Condition", "Success");
    step.addProperty("StartTrigger", "StartAfterPrevious");
    step.addProperty("PackageRequirement", "LetOctopusDecide");
    step.add("Properties", new JsonObject());
    step.add("Actions", actions);

    final JsonArray steps = new JsonArray();
    steps.add(step);
    return steps;
  }
}
