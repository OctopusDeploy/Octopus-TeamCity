# End-to-end tests

The e2e suite drives the **real plugin** through real containers with
[Testcontainers](https://testcontainers.com): one shared stack — a TeamCity server (booted from a
prepared data directory with the built plugin pre-installed), a TeamCity agent, a **free-tier**
Octopus Deploy, and its MSSQL database — is started **once** for the whole suite (see `SharedStack`)
and reaped at JVM exit. Each test provisions its own Octopus project via the `OctopusProvisioning`
DSL, creates a TeamCity project/build with the relevant Octopus step over the REST client
(`TeamCityRest`), triggers the build, and asserts on what appears in Octopus (via the Octopus Java
SDK). The `*UiTest` classes additionally drive the connection edit forms in a browser with
Playwright.

Octopus runs in its free tier, so **no licence is required**.

## Prerequisites

- **Docker must be reachable.** Testcontainers launches the containers against the daemon at
  `DOCKER_HOST`; if yours isn't the default socket, export it (e.g.
  `export DOCKER_HOST=unix:///var/run/docker.sock`).
- **JDK 8 or 11** (same as the main build — see "Building" in the README).
- For the Playwright UI tests, fetch the browser first with `./gradlew :e2e:playwrightInstall`
  (downloads Chromium; on Linux also `apt-get`s the system libs it links against).

## Running on the host Docker daemon

```sh
./gradlew :e2e:playwrightInstall            # once — only needed for the *UiTest classes
./gradlew :e2e:e2eTest                       # run the whole suite
./gradlew :e2e:e2eTest --tests "*OctopusCreateReleaseE2ETest"   # a single test
```

`e2eTest` builds the plugin zip itself (it `dependsOn` the root `distZip`) and hands it to the
containers via the `TEAMCITY_PLUGIN_DIST` env var. To test a **prebuilt** zip instead of rebuilding
(this is what CI does — the `build` job's artifact), point that var at it:

```sh
TEAMCITY_PLUGIN_DIST=build/distributions/Octopus.TeamCity.<X.Y.Z>.zip ./gradlew :e2e:e2eTest
```

The `e2e` module's plain `test` task is disabled; everything runs under `e2eTest`, single-threaded
(`maxParallelForks = 1`), since the tests share one server.

## Running inside a Linux container (recommended on macOS/Windows)

The Playwright UI tests need Chromium's Linux system libraries, which `playwrightInstall
--with-deps` can only install on Linux. `e2e/docker-compose.yml` runs the suite in an Ubuntu
container that talks to the **host** Docker daemon via the mounted socket (so the TeamCity/Octopus
images aren't re-pulled), launching the test containers as siblings on the host. Run from the repo
root:

```sh
# default: just the Playwright UI test
docker compose -f e2e/docker-compose.yml up --abort-on-container-exit --exit-code-from e2e

# override GRADLE_TESTS to pick what runs, e.g. the full (non-UI) suite:
GRADLE_TESTS="--tests octopus.teamcity.e2e.test.*" \
  docker compose -f e2e/docker-compose.yml up --abort-on-container-exit --exit-code-from e2e
```

## Adding a new test

Tests build everything programmatically — there's no project export to maintain. To add one:

1. Grab the shared stack: `try (OctopusTeamCityStack stack = SharedStack.full()) { ... }`.
2. Provision any Octopus prerequisites with `OctopusProvisioning` (e.g. a project with a deployment
   process so `octo create-release` has a viable plan).
3. Create the TeamCity project/build and add the Octopus step over `TeamCityRest`, then trigger the
   build.
4. Assert on the outcome in Octopus via the SDK client from `stack.octopusClient()`.

Use **unique** TeamCity ids and Octopus project/environment names per test — the stack is shared, so
clashing names will collide.

## OIDC plugin dependency

The OIDC api-key source depends on the separate **teamcity-oidc-plugin** (it provides the
`oidc-identity-token` connection type our connection form lists). The stack installs that plugin
alongside ours: `OidcPluginFixture` downloads the **latest public release** on demand and caches it
by version under the temp dir, so the tests always run against the current plugin without vendoring a
binary in the repo.

For offline runs, or to pin a specific version, point `OIDC_PLUGIN_ZIP` at a local zip to skip the
download:

```bash
OIDC_PLUGIN_ZIP=/path/to/Octopus.TeamCity.OIDC.<ver>.zip ./gradlew :e2e:e2eTest
```

(Set `GITHUB_TOKEN` if you hit the unauthenticated GitHub API rate limit.)

`OctopusConnectionOidcSourceUiTest` checks the cross-plugin discovery (the connector appears in our
form); `OctopusParameterSourcePushE2ETest` covers the `%param%` api-key source end to end.

## Interactive manual testing

To bring the same stack up and click around by hand (rather than asserting in code), use
`docker-compose.manual.yml` — see the header of that file for the `cp .env.example .env`, build, and
`docker compose up` steps.
