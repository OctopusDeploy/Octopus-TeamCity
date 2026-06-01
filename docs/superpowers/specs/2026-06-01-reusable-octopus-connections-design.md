# Reusable Octopus connections — design

Date: 2026-06-01
Branch: `feat/reusable-octopus-connections`

## Problem

Every Octopus build step in the TeamCity plugin asks the user to re-enter the
**Octopus URL**, **API key**, and optional **Space name** (and, for most steps,
the **Octopus version**). A team with many build configurations must repeat and
maintain these in every step, and rotating an API key means editing every step.

## Goal

Let an administrator define the Octopus server connection details **once** as a
reusable, project-level connection and reference it from many build steps. The
feature is **optional**: a step that does not select a connection behaves exactly
as it does today (manual URL / API key / version / space fields).

## Chosen mechanism

A project-level **TeamCity OAuth Connection** (`OAuthProvider` +
`OAuthConnectionsManager`). This is the idiomatic TeamCity way to define a
credential once at the project level and reference it from many build
configurations; connections are inherited down the project tree. This mirrors:

- the recent `teamcity-oidc-plugin` (`OidcConnectionProvider` / `OidcConnectionsManager`,
  PR #130), used as the modern reference pattern, and
- the unmerged `origin/connection` branch in this repo, which started the same
  approach (`OctopusConnection extends OAuthProvider`, `ConnectionPropertyNames`,
  `OctopusConnectionController`). We lift the *connection concept* onto today's
  `main` rather than reviving that branch (which is a large, diverged rewrite).

### Alternatives considered (rejected)

- **Config-parameter convention** (users put URL/key in project parameters and
  reference `%octopus.url%`): almost no code, but poor UX, not discoverable, no
  dropdown, manual secret handling. Does not deliver "create a connection."
- **Revive `origin/connection`**: that branch bundles a new SDK, a restructured
  agent, and a "generic" runner; far diverged from `main`. High risk for the
  scope of this change.

## What a connection stores

| Field | Property key | Notes |
|-------|--------------|-------|
| Display name | `displayName` | Required by TeamCity; must be `displayName`. |
| Octopus URL | `octopus_host` | Required. |
| API key | `secure:octopus_apikey` | Required, stored as a TeamCity password (masked). |
| Octopus version | `octopus_version` | Required. One of the existing version options. |
| Space name | `octopus_space_name` | Optional default; per-step value can override. |

(No proxy fields — current build steps do not expose proxy, so it is out of scope.)

## Components (server)

All registered as constructor-autowired beans in
`octopus-server/src/main/resources/META-INF/build-server-plugin-Octopus.TeamCity.xml`.

1. **`octopus.teamcity.common.connection.ConnectionPropertyNames`** (revived in
   `octopus-common`) — the connection's own property-key constants (table above).

2. **`octopus.teamcity.server.connection.OctopusConnectionProvider extends OAuthProvider`**
   - `getType()` → `"OctopusConnection"`, `getDisplayName()` → `"OctopusDeploy Server"`.
   - `getEditParametersUrl()` → `editOctopusConnection.jsp`.
   - `describeConnection()` → summarises the Octopus URL (and version).
   - `getPropertiesProcessor()` → validates URL present + well-formed, API key
     present, version is a known value.

3. **`octopus.teamcity.server.connection.OctopusConnectionsManager`** — wraps
   TeamCity's `OAuthConnectionsManager`:
   - `listAvailable(SProject)` → typed connections available to a project
     (`getAvailableConnectionsOfType(project, OctopusConnection.TYPE)`).
   - `resolve(SProject, connectionId)` → `Optional<typed connection>` via
     `findConnectionById` (walks project + ancestors).

4. **`octopus.teamcity.server.connection.OctopusConnectionController`** — a
   `BaseController` registered against each credentialed runner's edit-JSP path.
   Adds the available connections to the model as a list of simple maps
   (`id`, `displayName`, `url`, `version`, `space`) so each step JSP can render
   the dropdown and so the Create-release Git-ref show/hide JS can map the chosen
   connection to its version. Also adds a link to Project Settings → Connections.

5. **`octopus.teamcity.server.connection.OctopusConnectionBuildStartProcessor`
   implements `BuildStartContextProcessor`** — at build start, iterates
   `buildStartContext.getRunnerContexts()`. For each Octopus runner whose
   `octopus_connection_id` is set, resolves the connection and writes the
   effective values into that runner's parameters via
   `runnerContext.addRunnerParameter(...)` (see Precedence). A
   `BuildStartContextProcessor` is already used in this codebase
   (`OctopusBuildInformationBuildStartProcessor`), so this is an established
   pattern.

## Constant

Add to `octopus.teamcity.common.OctopusConstants`:

```java
public String getConnectionIdKey() { return "octopus_connection_id"; }
```

## Per-step UI

The five credentialed steps get the optional selector:

- Create release, Deploy release, Promote release, Push package, Build information.
- (Pack package has no server credentials and is unchanged.)

At the top of each step's **Octopus Connection** settings group:

- A toggle / selector: **"Use a connection"** vs **"Enter manually"**.
- **Use a connection** → a dropdown populated from the controller's model, plus a
  link to add one under Project Settings → Connections. The manual URL / API key /
  version fields are **hidden**.
- **Enter manually** (default when no `octopus_connection_id` is stored) → today's
  fields, unchanged.
- The Space-name field is always shown (it can override the connection default).
- Create release's existing Git-ref show/hide JS reads the selected connection's
  version (from the controller-supplied data) when a connection is in use,
  instead of the hidden version `<select>`.

## Build-time data flow & precedence

The agent is **unchanged**: every agent build process reads `octopus_host`,
`secure:octopus_apikey`, `octopus_version`, and `octopus_space_name` from
`getContext().getRunnerParameters()`. The server makes those keys correct before
the build reaches the agent.

When `octopus_connection_id` is set on a runner, `OctopusConnectionBuildStartProcessor`
resolves the connection and sets that runner's effective parameters:

- **URL** (`octopus_host`) — from the connection.
- **API key** (`octopus_apikey`) — from the connection (remains masked as a password).
- **Version** (`octopus_version`) — from the connection.
- **Space name** (`octopus_space_name`) — **step value wins if non-empty;
  otherwise the connection's space; otherwise the default space.**

When `octopus_connection_id` is empty/absent → no injection; the step's manual
values are used exactly as today.

## Validation & backward compatibility

- Each affected run type's `PropertiesProcessor` changes from
  "URL and API key required" to **"either `octopus_connection_id` is set, or
  manual URL + API key are provided."** Version validation likewise accepts a
  connection in place of the manual version field.
- Existing saved steps store no `octopus_connection_id`, so they continue to use
  their manual fields with no change in behaviour — fully backward compatible.
- The connection's API key is stored as a TeamCity password (secure property) and
  stays masked when injected into runner parameters.

## Testing

- **Unit — connection validation**: `OctopusConnectionPropertiesProcessor`
  (revive/adapt the `origin/connection` branch's test) — missing/invalid URL,
  missing API key, unknown version.
- **Unit — resolver precedence**: `OctopusConnectionBuildStartProcessor` /
  resolver — URL, key, version come from the connection; space uses step value
  when set, else connection space, else default; no injection when no
  `connection_id`.
- **Unit — run-type validation**: the "either connection_id or manual URL+key"
  rule for each affected run type (valid with connection only; valid with manual
  only; invalid with neither).
- Agent build processes are unchanged, so existing agent tests stand.

## Out of scope

- Proxy settings in the connection.
- Changes to the Pack package step.
- Reviving the broader `origin/connection` rewrite (SDK, generic runner, agent
  restructure).
