# Blank Inline Credentials When A Connection Is Selected — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** When a build step is saved with an Octopus connection selected and validation passes, strip the now-redundant inline URL / API key / version runner parameters from the stored step config.

**Architecture:** Add a small stateless helper in the `octopus.teamcity.server.connection` package that removes the three inline credential keys when `octopus_connection_id` is non-blank. Each of the five connection-aware runners' anonymous `PropertiesProcessor.process(...)` calls the helper as its final step, guarded by `result.isEmpty()` so stripping only happens on an otherwise-valid save. TeamCity persists a `PropertiesProcessor`-modified map on save (per its Javadoc), so removing keys there is the supported mechanism.

**Tech Stack:** Java 8, JUnit 5 (Jupiter), AssertJ, Mockito, TeamCity server-api, Gradle.

## Global Constraints

- Java language level **8**; build with **JDK 11** (JDK 8 also works; newer JDKs break error-prone). CI runs `./gradlew build test`.
- error-prone runs with `-Werror` — warnings fail the build.
- Formatting is google-java-format 1.7 with import order `com.octopus`, `java`, `` (everything else last). Run `./gradlew spotlessApply` before committing Java changes; `spotlessCheck` is part of `build`.
- **No single-letter variable, field, or parameter names in new code.** (Pre-existing `c`/`p` in the runner processors stay as-is; new code uses descriptive names.)
- Inline credential parameter keys (from `OctopusConstants`): server URL = `octopus_host` (`getServerKey()`), API key = `secure:octopus_apikey` (`getApiKey()`), version = `octopus_version` (`getOctopusVersion()`), connection id = `octopus_connection_id` (`getConnectionIdKey()`). Space = `octopus_space_name` (`getSpaceName()`) is **NOT** stripped.

---

### Task 1: `ConnectionInlineFieldCleaner` helper

**Files:**
- Create: `octopus-server/src/main/java/octopus/teamcity/server/connection/ConnectionInlineFieldCleaner.java`
- Test: `octopus-server/src/test/java/octopus/teamcity/server/connection/ConnectionInlineFieldCleanerTest.java`

**Interfaces:**
- Consumes: `OctopusConstants` (`getConnectionIdKey()`, `getServerKey()`, `getApiKey()`, `getOctopusVersion()`, `getSpaceName()`); `jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(String)`.
- Produces: `public static void ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(Map<String, String> properties)` — when `octopus_connection_id` is present and non-blank, removes `octopus_host`, `secure:octopus_apikey`, and `octopus_version` from the map; otherwise leaves the map unchanged. Null-safe (no-op on a null map).

- [ ] **Step 1: Write the failing test**

Create `octopus-server/src/test/java/octopus/teamcity/server/connection/ConnectionInlineFieldCleanerTest.java`:

```java
package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class ConnectionInlineFieldCleanerTest {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  @Test
  void removesInlineCredentialFieldsWhenConnectionSelected() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");
    properties.put(CONSTANTS.getSpaceName(), "Default");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties)
        .doesNotContainKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
    assertThat(properties).containsEntry(CONSTANTS.getSpaceName(), "Default");
    assertThat(properties).containsEntry(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
  }

  @Test
  void leavesInlineFieldsWhenNoConnectionSelected() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties)
        .containsKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
  }

  @Test
  void leavesInlineFieldsWhenConnectionIdIsBlank() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "   ");
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties)
        .containsKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
  }

  @Test
  void toleratesConnectionSelectedWithNoInlineFields() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");

    ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(properties);

    assertThat(properties).containsOnlyKeys(CONSTANTS.getConnectionIdKey());
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :octopus-server:test --tests "octopus.teamcity.server.connection.ConnectionInlineFieldCleanerTest"`
Expected: FAIL — compilation error, `ConnectionInlineFieldCleaner` does not exist.

- [ ] **Step 3: Create the helper**

Create `octopus-server/src/main/java/octopus/teamcity/server/connection/ConnectionInlineFieldCleaner.java`:

```java
/*
 * Copyright 2000-2012 Octopus Deploy Pty. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package octopus.teamcity.server.connection;

import java.util.Map;

import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;

/**
 * Removes a build step's inline Octopus credential parameters when the step references a reusable
 * connection instead. The connection supplies the server URL, API key, and version at build start,
 * so persisting the old inline values would only leave stale credentials behind the connection.
 *
 * <p>The space parameter is deliberately left untouched: a step's own space intentionally overrides
 * the connection's, and this class has no project context with which to resolve the connection.
 */
public final class ConnectionInlineFieldCleaner {
  private static final OctopusConstants CONSTANTS = new OctopusConstants();

  private ConnectionInlineFieldCleaner() {}

  public static void stripInlineFieldsIfUsingConnection(final Map<String, String> properties) {
    if (properties == null) {
      return;
    }
    final boolean usingConnection =
        !StringUtil.isEmptyOrSpaces(properties.get(CONSTANTS.getConnectionIdKey()));
    if (!usingConnection) {
      return;
    }
    properties.remove(CONSTANTS.getServerKey());
    properties.remove(CONSTANTS.getApiKey());
    properties.remove(CONSTANTS.getOctopusVersion());
  }
}
```

- [ ] **Step 4: Format**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL (reformats the new files if needed).

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :octopus-server:test --tests "octopus.teamcity.server.connection.ConnectionInlineFieldCleanerTest"`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/connection/ConnectionInlineFieldCleaner.java \
        octopus-server/src/test/java/octopus/teamcity/server/connection/ConnectionInlineFieldCleanerTest.java
git commit -m "feat: add helper to strip inline creds when a connection is selected"
```

---

### Task 2: Wire the cleaner into the five connection-aware runners

**Files:**
- Modify: `octopus-server/src/main/java/octopus/teamcity/server/OctopusCreateReleaseRunType.java`
- Modify: `octopus-server/src/main/java/octopus/teamcity/server/OctopusDeployReleaseRunType.java`
- Modify: `octopus-server/src/main/java/octopus/teamcity/server/OctopusPromoteReleaseRunType.java`
- Modify: `octopus-server/src/main/java/octopus/teamcity/server/OctopusPushPackageRunType.java`
- Modify: `octopus-server/src/main/java/octopus/teamcity/server/OctopusBuildInformationRunType.java`
- Test: `octopus-server/src/test/java/octopus/teamcity/server/OctopusDeployReleaseRunTypeValidationTest.java`

**Interfaces:**
- Consumes: `ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(Map<String, String>)` from Task 1.
- Produces: no new public API. Behavior change only: `getRunnerPropertiesProcessor().process(properties)` now strips the three inline credential keys from `properties` when a connection is selected and the returned `InvalidProperty` collection is empty.

Note: `OctopusPackPackageRunType` has no connection handling (local packaging, no server credentials) and is intentionally left untouched.

- [ ] **Step 1: Write the failing runner-level tests**

Add these two tests to `octopus-server/src/test/java/octopus/teamcity/server/OctopusDeployReleaseRunTypeValidationTest.java` (inside the class, after the existing tests). The existing `validate(...)` helper passes the map straight into `process(...)`, so mutations are observable on the same `properties` reference afterward.

```java
  @Test
  void stripsInlineCredentialFieldsWhenConnectionSelectedAndValidationPasses() {
    final Map<String, String> properties = withMandatoryNonCredentialFields(new HashMap<>());
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");
    properties.put(CONSTANTS.getSpaceName(), "Default");

    validate(properties);

    assertThat(properties)
        .doesNotContainKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
    assertThat(properties).containsEntry(CONSTANTS.getSpaceName(), "Default");
  }

  @Test
  void retainsInlineCredentialFieldsWhenValidationFails() {
    // Missing mandatory project name => validation fails, so nothing is stripped.
    final Map<String, String> properties = new HashMap<>();
    properties.put(CONSTANTS.getConnectionIdKey(), "PROJECT_EXT_1");
    properties.put(CONSTANTS.getServerKey(), "https://octo");
    properties.put(CONSTANTS.getApiKey(), "API-KEY");
    properties.put(CONSTANTS.getOctopusVersion(), "3.0+");

    final Collection<String> errors = validate(properties);

    assertThat(errors).contains(CONSTANTS.getProjectNameKey());
    assertThat(properties)
        .containsKeys(
            CONSTANTS.getServerKey(), CONSTANTS.getApiKey(), CONSTANTS.getOctopusVersion());
  }
```

(The file already imports `java.util.Collection`, `java.util.HashMap`, `java.util.Map`, and statically imports `assertThat`, so no new imports are needed.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :octopus-server:test --tests "octopus.teamcity.server.OctopusDeployReleaseRunTypeValidationTest"`
Expected: FAIL — `stripsInlineCredentialFieldsWhenConnectionSelectedAndValidationPasses` fails because the inline keys are still present (stripping not yet wired in). `retainsInlineCredentialFieldsWhenValidationFails` should pass already (nothing strips yet), which is fine.

- [ ] **Step 3: Wire the cleaner into `OctopusDeployReleaseRunType`**

In `octopus-server/src/main/java/octopus/teamcity/server/OctopusDeployReleaseRunType.java`, add the import (with the other `octopus.*` imports):

```java
import octopus.teamcity.server.connection.ConnectionInlineFieldCleaner;
```

Then in `process(...)`, replace the final return block:

```java
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
        checkNotEmpty(p, c.getReleaseNumberKey(), "Release number must be specified", result);
        checkNotEmpty(p, c.getDeployToKey(), "Deploy to must be specified", result);

        return result;
```

with:

```java
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
        checkNotEmpty(p, c.getReleaseNumberKey(), "Release number must be specified", result);
        checkNotEmpty(p, c.getDeployToKey(), "Deploy to must be specified", result);

        if (result.isEmpty()) {
          ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(p);
        }

        return result;
```

- [ ] **Step 4: Run the Deploy tests to verify they pass**

Run: `./gradlew :octopus-server:test --tests "octopus.teamcity.server.OctopusDeployReleaseRunTypeValidationTest"`
Expected: PASS (existing tests plus the two new ones).

- [ ] **Step 5: Wire the cleaner into the other four runners**

Apply the same two edits (add the import, and wrap the strip call in `if (result.isEmpty()) { ... }` immediately before `return result;`) to each of these. The import line is identical in every file:

```java
import octopus.teamcity.server.connection.ConnectionInlineFieldCleaner;
```

**`OctopusCreateReleaseRunType.java`** — replace:

```java
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);

        return result;
```

with:

```java
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);

        if (result.isEmpty()) {
          ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(p);
        }

        return result;
```

**`OctopusPromoteReleaseRunType.java`** — replace:

```java
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
        checkNotEmpty(
            p, c.getPromoteFromKey(), "Environment to promote from must be specified", result);
        checkNotEmpty(p, c.getDeployToKey(), "Deploy to must be specified", result);

        return result;
```

with:

```java
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
        checkNotEmpty(
            p, c.getPromoteFromKey(), "Environment to promote from must be specified", result);
        checkNotEmpty(p, c.getDeployToKey(), "Deploy to must be specified", result);

        if (result.isEmpty()) {
          ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(p);
        }

        return result;
```

**`OctopusPushPackageRunType.java`** — replace:

```java
        checkNotEmpty(p, c.getPackagePathsKey(), "Package paths must be specified", result);

        return result;
```

with:

```java
        checkNotEmpty(p, c.getPackagePathsKey(), "Package paths must be specified", result);

        if (result.isEmpty()) {
          ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(p);
        }

        return result;
```

**`OctopusBuildInformationRunType.java`** — replace:

```java
        checkNotEmpty(p, c.getPackageIdKey(), "Package ID must be specified", result);
        checkNotEmpty(p, c.getPackageVersionKey(), "Package version be specified", result);

        return result;
```

with:

```java
        checkNotEmpty(p, c.getPackageIdKey(), "Package ID must be specified", result);
        checkNotEmpty(p, c.getPackageVersionKey(), "Package version be specified", result);

        if (result.isEmpty()) {
          ConnectionInlineFieldCleaner.stripInlineFieldsIfUsingConnection(p);
        }

        return result;
```

- [ ] **Step 6: Format**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Full build and test (matches CI)**

Run: `./gradlew build test`
Expected: BUILD SUCCESSFUL — compile, spotlessCheck, error-prone (`-Werror`), license check, and all unit tests pass.

- [ ] **Step 8: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/OctopusCreateReleaseRunType.java \
        octopus-server/src/main/java/octopus/teamcity/server/OctopusDeployReleaseRunType.java \
        octopus-server/src/main/java/octopus/teamcity/server/OctopusPromoteReleaseRunType.java \
        octopus-server/src/main/java/octopus/teamcity/server/OctopusPushPackageRunType.java \
        octopus-server/src/main/java/octopus/teamcity/server/OctopusBuildInformationRunType.java \
        octopus-server/src/test/java/octopus/teamcity/server/OctopusDeployReleaseRunTypeValidationTest.java
git commit -m "feat: blank inline credentials on save when a connection is selected"
```

---

## Notes

- **Space is never stripped** — deliberate; the step space overrides the connection space, and the processor has no project context to resolve the connection's space.
- **`octopus_apikey_source` / OIDC inline keys are left as-is** — `OctopusConnectionBuildStartProcessor` overwrites them from the connection at build start, so any leftover value is harmless.
- **e2e coverage** stays in its separate PR per the existing plan; this plan is unit-tested only.
