# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

This is the **Octopus Deploy plugin for TeamCity** — it adds build runners that trigger Octopus
operations (create/deploy/promote releases, push/pack packages, publish build information) by
invoking the Octopus CLI on the build agent.

## Build & test commands

The Gradle wrapper downloads Gradle itself. Java target is **8**; build with **JDK 8 (what CI uses)
or JDK 11** — error-prone (run with `-Werror`, so warnings fail the build) breaks on much newer JDKs.

- **Full verification (what CI runs):** `./gradlew build test` — compile, spotless check,
  error-prone, license-header check, and unit tests.
- **Format code:** `./gradlew spotlessApply`. Formatting is google-java-format 1.7 with import order
  `com.octopus`, `java`, ``; `spotlessCheck` is part of `build` and a mis-format fails CI. Run
  `spotlessApply` before committing Java changes.
- **Build the plugin zip:** `./gradlew clean distZip` → `build/distributions/Octopus.TeamCity.<version>.zip`.
- **Run one test class:** `./gradlew :octopus-server:test --tests "OctopusConnectionTest"`
  (substitute the module and class).

### End-to-end tests

`./gradlew e2eTest` (or `:e2e:e2eTest --tests "*SomeTest"`). These spin up real containers
(TeamCity, an agent, and — where needed — Octopus + MSSQL) via Testcontainers, so **Docker must be
reachable** (`DOCKER_HOST`). The task builds the plugin and passes it via `TEAMCITY_PLUGIN_DIST`.
Playwright UI tests also need `./gradlew :e2e:playwrightInstall` first. The `e2e` module's unit
`test` task is disabled — everything runs under `e2eTest`.

See `README.md` for IntelliJ remote-debug setup (`AttachToServer`/`AttachToAgent`), the
`docker-compose.*.yml` dev stacks, and the release process.

## Architecture

A TeamCity plugin is **two cooperating sub-plugins** — a server side and an agent side — shipped in
one zip. The root `distZip` composes `:octopus-agent:distZip` into `agent/` and
`:octopus-server:installDist` into `server/`, alongside `teamcity-plugin.xml`. Each sub-plugin boots
its own Spring context from `META-INF/build-{server,agent}-plugin-Octopus.TeamCity.xml` — **those XML
files are the registration entry points**; a class only takes effect if it's declared there (or
self-registers, e.g. run types call `runTypeRegistry.registerRunType(this)`).

Gradle modules:
- **`octopus-common`** — the contract between the two sides. `OctopusConstants` and
  `ConnectionPropertyNames` define the runner-parameter *string keys*. The server and agent never
  share objects; they only agree on these keys.
- **`octopus-server`** — defines each runner: a `RunType` (display, default props, edit-form JSP) and
  its `PropertiesProcessor` (server-side validation). JSP edit forms live in
  `src/main/resources/buildServerResources/`.
- **`octopus-agent`** — executes a step: a build process reads runner parameters by `OctopusConstants`
  key and builds an `octo` command line.
- **`e2e`** — Testcontainers-based integration tests (not part of the shipped plugin).

**The server→agent flow:** the server stores a build step's settings as runner parameters keyed by
`OctopusConstants`. At build time the agent's matching build process reads those same keys and runs
the CLI. There are **six runners**: `octopus.create.release`, `octopus.deploy.release`,
`octopus.promote.release`, `octopus.push.package`, `octopus.pack.package`, and `octopus.metadata`
(build information).

**The Go CLI on the agent.** The agent bundles the Octopus Go CLI as classpath resources at
`resources/cli/2.0/<os>-<arch>/...`. The archives are not in git: the `downloadOctopusCli` task
fetches them from the OctopusDeploy/cli GitHub release into
`octopus-agent/build/generated/octopus-cli/` and checks them against the SHA-256 hashes in
`gradle.properties`. To bump the Go CLI, update `octopusCliVersion` and every hash from the
release's `SHA256SUMS` file. `OctopusOsUtils` picks the folder from the agent OS and the agent
JVM's `os.arch`, `EmbeddedResourceExtractor` extracts the archive, and the build processes in the
`cli/` package run it.

### Reusable Octopus connections

Lets an admin define the Octopus URL/API key/version/space **once** as a project-level TeamCity OAuth
connection and reference it from steps, instead of re-entering credentials per step. Optional and
backward compatible — a step with no connection uses its inline fields as before. Key pieces:

- `OctopusConnection` (an `OAuthProvider`) + `OctopusConnectionsManager` (list/resolve, walking the
  project tree) + `OctopusConnectionPropertiesProcessor` (validation).
- `OctopusConnectionBuildStartProcessor` resolves the selected connection at **build start** and
  injects its URL/key/version/space into the step's runner parameters **under the same
  `OctopusConstants` keys** — so the agent is unchanged and connection-agnostic. A step's own space
  value wins over the connection's.
- The step edit forms get the connection list from the **`OctopusConnectionUiData` static accessor**,
  not a controller: TeamCity server-side-includes the runner-params JSP and does **not** route that
  include through a registered controller, so a controller never runs for it.

## Conventions

- **No single-letter variable, field, or parameter names in new code** — use descriptive names
  (e.g. `properties`, `descriptor`, `constants`). Some pre-existing code uses `c`/`p`; leave those as
  they are, but don't add new ones.

## Gotchas

- Edit resources (JSPs, Spring XML) under `octopus-server/src/...` — **never** `octopus-server/build/...`,
  which is generated output and silently overwritten by the next build.
- A JSP pulled in with `jsp:include` does **not** inherit the including page's `<jsp:useBean>` beans;
  shared fragments must declare their own (e.g. `connectionSelector.jsp` re-declares `keys`).
- The API key is a TeamCity secure property and is injected under the `secure:` prefix so it stays
  masked in build logs; never place it in a JSP/page model.
