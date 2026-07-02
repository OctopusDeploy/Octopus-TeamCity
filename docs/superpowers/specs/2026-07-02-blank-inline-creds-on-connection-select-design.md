# Blank inline credentials when a connection is selected

## Problem

A build step can either supply Octopus credentials inline (URL / API key / version /
space) or reference a reusable Octopus connection via `octopus_connection_id`. Today,
switching a step from inline credentials to a connection leaves the old inline values
persisted in the step config — they are simply ignored at build time. Stale credentials
(including a masked-but-stored API key) linger behind the connection reference.

When a step is saved with a connection selected, the now-redundant inline credential
fields should be removed from the stored runner parameters.

## Behavior

On save, when `octopus_connection_id` is non-blank **and validation passes** (the
processor produces no `InvalidProperty` entries), remove these inline runner-parameter
keys from the properties map before it is stored:

- `octopus_host` — the server URL (`OctopusConstants.getServerKey()`)
- `secure:octopus_apikey` — the API key (`OctopusConstants.getApiKey()`)
- `octopus_version` — the Octopus version (`OctopusConstants.getOctopusVersion()`)

`octopus_space_name` is **not** removed. See "Why space is left alone" below.

When `octopus_connection_id` is blank (the step uses inline fields), or when validation
fails, the map is left untouched. Leaving the map intact on a failed save means the edit
form is redisplayed with the user's entered values, rather than having fields silently
cleared underneath them.

### Why this persists

`jetbrains.buildServer.serverSide.PropertiesProcessor.process(Map)` may modify the
properties map, not only validate it. Per its Javadoc: *"Properties map passed as
argument can be verified or modified by the processor ... If processor was called during
saving properties to a configuration file, then modified map will be stored without
changes."* Removing keys inside `process()` is therefore the supported way to strip them
on save.

### Why space is left alone

`octopus_space_name` has deliberate override semantics:
`OctopusConnectionBuildStartProcessor` injects the connection's space **only if the step
did not set one** — a step's own space intentionally wins over the connection's. Blanking
the step space on save would destroy that feature.

### Scope: UI save path only

Stripping runs inside the runner `PropertiesProcessor`, which is the save path for the
web edit form. Steps created or updated by other means — the REST API, versioned settings
(Kotlin DSL / project-config XML in VCS), or config import — may not route through
`process()`, so stale inline credentials could persist there. This is a hygiene
limitation, not a correctness issue: whenever a connection is selected those leftover
inline values are ignored at build time (`OctopusConnectionBuildStartProcessor` overwrites
URL / API key / version from the connection).

### Why space is left alone (continued)

Additionally, `PropertiesProcessor.process(Map)` receives only the properties map — no
project context — so it cannot resolve the connection via `OctopusConnectionsManager` to
check whether the connection itself defines a space. A conditional "blank the step space
only when the connection has one" rule is not implementable at this layer.

Leaving the step space untouched does the right thing in both cases: if the connection's
space is blank the step keeps its space (the only source); if the connection defines a
space the step's retained space still wins, matching documented behavior.

## Design

A small shared helper in the `octopus.teamcity.server.connection` package:

```java
public final class ConnectionInlineFieldCleaner {
  // Removes the inline URL, API key, and version keys when a connection is selected.
  public static void stripInlineFieldsIfUsingConnection(Map<String, String> properties);
}
```

Each connection-aware runner's anonymous `PropertiesProcessor.process(...)` runs its
existing validation first, then calls the helper only when validation produced no
errors:

```java
public Collection<InvalidProperty> process(@Nullable final Map<String, String> properties) {
  final Collection<InvalidProperty> result = new ArrayList<>();
  if (properties == null) return result;

  final boolean usingConnection =
      !StringUtil.isEmptyOrSpaces(properties.get(constants.getConnectionIdKey()));
  if (!usingConnection) {
    checkNotEmpty(properties, constants.getApiKey(), "API key must be specified", result);
    checkNotEmpty(properties, constants.getServerKey(), "Server must be specified", result);
  }
  // ... remaining required-field checks unchanged ...

  if (result.isEmpty()) {
    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);
  }
  return result;
}
```

The helper is self-contained: it re-checks that `octopus_connection_id` is non-blank and
removes the three inline keys, so callers only need to guard on validation success.
Extracting it avoids copy-pasting the same three `remove()` calls into five anonymous
processors.

### Applies to

The five connection-aware runners:

- `OctopusCreateReleaseRunType`
- `OctopusDeployReleaseRunType`
- `OctopusPromoteReleaseRunType`
- `OctopusPushPackageRunType`
- `OctopusBuildInformationRunType`

`OctopusPackPackageRunType` has no connection handling (local packaging, no server
credentials) and is left untouched.

### Interaction with existing validation

Validation is unchanged and runs first; stripping happens only afterward, and only when
the result collection is empty. When a connection is selected the inline API-key /
server-URL required checks are already skipped, so a connection-backed step validates
cleanly and its inline keys are then stripped. The `octopus_apikey_source` / OIDC inline
keys are left as-is — at build start
`OctopusConnectionBuildStartProcessor` overwrites them from the connection, so any
leftover value is harmless.

## Testing

Unit-test the helper directly:

- Connection id set → `octopus_host`, `secure:octopus_apikey`, `octopus_version` removed;
  `octopus_space_name` and unrelated keys retained.
- Connection id blank → map unchanged.
- Connection id set but inline keys already absent → no error, map unchanged aside from
  the (absent) keys.

Because the "strip only when validation passes" guard lives in each runner's
`process()`, also cover it at that level (at least one runner): a connection-selected
step that fails a required-field check (e.g. missing project name) returns the
`InvalidProperty` and leaves the inline URL / API key / version intact.

e2e coverage stays in its separate PR per the existing plan.
