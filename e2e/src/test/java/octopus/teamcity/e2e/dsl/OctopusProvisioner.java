package octopus.teamcity.e2e.dsl;

import com.octopus.sdk.api.EnvironmentApi;
import com.octopus.sdk.api.LifecycleApi;
import com.octopus.sdk.api.ProjectApi;
import com.octopus.sdk.api.ProjectGroupApi;
import com.octopus.sdk.domain.Project;
import com.octopus.sdk.http.OctopusClient;
import com.octopus.sdk.model.environment.EnvironmentResource;
import com.octopus.sdk.model.project.ProjectResource;
import com.octopus.sdk.model.space.SpaceHome;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Provisions Octopus state the deployment-lifecycle e2e tests need (project with a deployment
 * process, environments). The SDK 0.0.6 can create projects/lifecycles/groups/environments but
 * cannot author a deployment process, so the (single, run-on-server) script step is added via the
 * Octopus REST API directly.
 */
public final class OctopusProvisioner {

  private final OctopusClient client;
  private final SpaceHome spaceHome;
  private final String octopusBaseUrl;
  private final String apiKey;

  public OctopusProvisioner(
      final OctopusClient client,
      final SpaceHome spaceHome,
      final String octopusBaseUrl,
      final String apiKey) {
    this.client = client;
    this.spaceHome = spaceHome;
    this.octopusBaseUrl = octopusBaseUrl;
    this.apiKey = apiKey;
  }

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
  public String createProjectWithServerScriptStep(
      final String projectName, final List<String> deployableEnvironmentIds) throws Exception {
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

    return created.getProperties().getId();
  }

  /**
   * Deletes a project and its dedicated lifecycle, so tests don't leave either behind on the shared
   * Octopus. The lifecycle can only be deleted once no project uses it.
   */
  public void deleteProject(final String projectId) throws Exception {
    final ProjectApi projects = ProjectApi.create(client, spaceHome);
    final String lifecycleId =
        projects
            .getById(projectId)
            .orElseThrow(() -> new IllegalStateException("No project with id " + projectId))
            .getProperties()
            .getLifecycleId();
    projects.delete(projectId);
    LifecycleApi.create(client, spaceHome).delete(lifecycleId);
  }

  /**
   * The same, for a project tests are willing to share. The Octopus in the stack runs unlicensed,
   * which caps how many projects may exist at once, so tests that only need somewhere to make a
   * release should share one rather than each spending a slot; whichever test runs first creates
   * it. Returns the project's id, whether it already existed or was just created.
   */
  public String ensureProjectWithServerScriptStep(
      final String projectName, final List<String> deployableEnvironmentIds) throws Exception {
    final Optional<Project> existing = ProjectApi.create(client, spaceHome).getByName(projectName);
    if (existing.isPresent()) {
      return existing.get().getProperties().getId();
    }

    return createProjectWithServerScriptStep(projectName, deployableEnvironmentIds);
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

    final Http.Response resp =
        octopusRequest("POST", octopusBaseUrl + lifecyclesLink, apiKey, lifecycle.toString());
    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
      throw new IllegalStateException(
          "create lifecycle -> " + resp.statusCode() + ": " + resp.body());
    }
    return JsonParser.parseString(resp.body()).getAsJsonObject().get("Id").getAsString();
  }

  /**
   * Adds a runbook with the same single run-on-server script step to an existing project, and
   * publishes a snapshot so it can be run without naming one. Returns the runbook's id.
   */
  public static String createRunbookWithServerScriptStep(
      final OctopusClient client,
      final SpaceHome spaceHome,
      final String octopusBaseUrl,
      final String apiKey,
      final String projectName,
      final String runbookName)
      throws Exception {
    final String projectId =
        ProjectApi.create(client, spaceHome)
            .getByName(projectName)
            .orElseThrow(() -> new IllegalStateException("No project named " + projectName))
            .getProperties()
            .getId();

    final JsonObject retention = new JsonObject();
    retention.addProperty("QuantityToKeep", 10);
    retention.addProperty("ShouldKeepForever", false);
    final JsonObject runbook = new JsonObject();
    runbook.addProperty("ProjectId", projectId);
    runbook.addProperty("Name", runbookName);
    runbook.addProperty("EnvironmentScope", "All");
    runbook.add("RunRetentionPolicy", retention);

    final JsonObject created =
        readJson(
            octopusRequest(
                "POST",
                octopusBaseUrl + stripUriTemplate(spaceHome.getRunbooksLink()),
                apiKey,
                runbook.toString()),
            "create runbook");
    final String runbookId = created.get("Id").getAsString();
    final String processUrl =
        octopusBaseUrl
            + stripUriTemplate(spaceHome.getRunbookProcessesLink())
            + "/"
            + created.get("RunbookProcessId").getAsString();

    final JsonObject process =
        readJson(octopusRequest("GET", processUrl, apiKey, null), "get runbook process");
    process.add("Steps", scriptStep());
    readJson(octopusRequest("PUT", processUrl, apiKey, process.toString()), "put runbook process");

    final JsonObject template =
        readJson(
            octopusRequest("GET", processUrl + "/runbookSnapshotTemplate", apiKey, null),
            "get runbook snapshot template");
    final JsonObject snapshot = new JsonObject();
    snapshot.addProperty("ProjectId", projectId);
    snapshot.addProperty("RunbookId", runbookId);
    snapshot.addProperty("Name", template.get("NextNameIncrement").getAsString());
    snapshot.add("SelectedPackages", new JsonArray());
    readJson(
        octopusRequest(
            "POST",
            octopusBaseUrl
                + stripUriTemplate(spaceHome.getRunbookSnapshotsLink())
                + "?publish=true",
            apiKey,
            snapshot.toString()),
        "publish runbook snapshot");

    return runbookId;
  }

  /** True if the runbook has been run at least once. */
  public static boolean hasRunbookRun(
      final SpaceHome spaceHome,
      final String octopusBaseUrl,
      final String apiKey,
      final String runbookId)
      throws Exception {
    final JsonObject runs =
        readJson(
            octopusRequest(
                "GET",
                octopusBaseUrl + stripUriTemplate(spaceHome.getRunbookRunsLink()) + "?take=100",
                apiKey,
                null),
            "list runbook runs");
    for (final JsonElement run : runs.getAsJsonArray("Items")) {
      if (runbookId.equals(run.getAsJsonObject().get("RunbookId").getAsString())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates an Octopus service account that accepts OIDC tokens from {@code issuer} carrying
   * exactly {@code subject}, and adds it to Octopus Administrators so it can see every space.
   * Returns the account's external id, which is the audience its tokens must be issued for.
   */
  public static String createOidcServiceAccount(
      final String octopusBaseUrl,
      final String apiKey,
      final String username,
      final String issuer,
      final String subject)
      throws Exception {
    final JsonObject user = new JsonObject();
    user.addProperty("Username", username);
    user.addProperty("DisplayName", username);
    user.addProperty("IsActive", true);
    user.addProperty("IsService", true);
    user.add("Identities", new JsonArray());
    final String userId =
        readJson(
                octopusRequest("POST", octopusBaseUrl + "/api/users", apiKey, user.toString()),
                "POST /api/users")
            .get("Id")
            .getAsString();

    final JsonObject identity = new JsonObject();
    identity.addProperty("ServiceAccountId", userId);
    identity.addProperty("Name", "TeamCity");
    identity.addProperty("Issuer", issuer);
    identity.addProperty("Subject", subject);
    readJson(
        octopusRequest(
            "POST",
            octopusBaseUrl + "/api/serviceaccounts/" + userId + "/oidcidentities/create/v1",
            apiKey,
            identity.toString()),
        "POST oidcidentities/create");

    final String teamUrl = octopusBaseUrl + "/api/teams/teams-administrators";
    final JsonObject team = readJson(octopusRequest("GET", teamUrl, apiKey, null), "GET team");
    team.getAsJsonArray("MemberUserIds").add(userId);
    readJson(octopusRequest("PUT", teamUrl, apiKey, team.toString()), "PUT team");

    return readJson(
            octopusRequest(
                "GET",
                octopusBaseUrl
                    + "/api/serviceaccounts/"
                    + userId
                    + "/oidcidentities/v1?skip=0&take=1",
                apiKey,
                null),
            "GET oidcidentities")
        .get("ExternalId")
        .getAsString();
  }

  private static void addInlineScriptStep(
      final String octopusBaseUrl, final String apiKey, final String deploymentProcessLink)
      throws Exception {
    final String url = octopusBaseUrl + stripUriTemplate(deploymentProcessLink);

    final Http.Response get = octopusRequest("GET", url, apiKey, null);
    if (get.statusCode() != 200) {
      throw new IllegalStateException(
          "GET deployment process -> " + get.statusCode() + ": " + get.body());
    }

    final JsonObject process = JsonParser.parseString(get.body()).getAsJsonObject();
    process.add("Steps", scriptStep());

    final Http.Response put = octopusRequest("PUT", url, apiKey, process.toString());
    if (put.statusCode() < 200 || put.statusCode() >= 300) {
      throw new IllegalStateException(
          "PUT deployment process -> " + put.statusCode() + ": " + put.body());
    }
  }

  /** Links may carry a URI template suffix (e.g. "{?...}"); strip it. */
  private static String stripUriTemplate(final String link) {
    return link.contains("{") ? link.substring(0, link.indexOf('{')) : link;
  }

  private static JsonObject readJson(final Http.Response response, final String what) {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IllegalStateException(
          what + " -> " + response.statusCode() + ": " + response.body());
    }
    return JsonParser.parseString(response.body()).getAsJsonObject();
  }

  private static Http.Response octopusRequest(
      final String method, final String url, final String apiKey, final String body)
      throws IOException {
    final Map<String, String> headers = new LinkedHashMap<>();
    headers.put("X-Octopus-ApiKey", apiKey);
    return Http.send(method, url, headers, "application/json", body);
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
