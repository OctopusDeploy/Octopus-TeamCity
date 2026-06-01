# Reusable Octopus Connections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an admin define an Octopus server connection (URL, API key, version, optional space) once as a project-level TeamCity OAuth connection, then optionally reference it from any Octopus build step, falling back to today's per-step manual fields when no connection is selected.

**Architecture:** A new `OAuthProvider` registers a project-level "OctopusDeploy Server" connection type. Each credentialed step's edit JSP gains a connection dropdown (served by a dedicated controller that injects the available connections into the JSP model). At build start a `BuildStartContextProcessor` resolves the selected connection and writes URL/key/version/space into that runner's parameters, so the **agent code is unchanged** — it keeps reading `octopus_host` / `secure:octopus_apikey` / `octopus_version` / `octopus_space_name` from its runner parameters.

**Tech Stack:** Java 8, Gradle, TeamCity `server-api` + `oauth` (both already `provided` deps), JUnit 5 (Jupiter), AssertJ, Mockito. JSP + JSTL for the UI.

---

## Key facts an implementer needs

- **Test runner:** JUnit 5. Run a single test class with:
  `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionPropertiesProcessorTest'`
  Run all common/server tests with `./gradlew :octopus-common:test :octopus-server:test`.
- **No server tests exist yet** under `octopus-server/src/test` — you are creating that tree. The deps are already wired (`octopus-server/build.gradle` has `testImplementation` for `server-api`, junit-jupiter, assertj). Mockito is available via `gradle/versions.gradle` (`org.mockito:mockito-core`, `mockito-junit-jupiter`) — if `octopus-server/build.gradle` lacks a `testImplementation 'org.mockito:mockito-core'` / `'org.mockito:mockito-junit-jupiter'` line, add it (see Task 5).
- **Do NOT depend on `com.octopus.sdk.*`** — the old `origin/connection` branch used `com.octopus.sdk.utils.ApiKeyValidator` and `BaseUserData`; neither exists on `main`. All new code here is self-contained.
- **Property keys reused intentionally:** the connection stores its URL/key/version/space under the *same* string keys the steps already use (`octopus_host`, `secure:octopus_apikey`, `octopus_version`, `octopus_space_name`). That is deliberate so the build-start processor can copy them straight into runner params.
- **`SECURE_PROPERTY_PREFIX`** is `"secure:"`, from `jetbrains.buildServer.agent.Constants.SECURE_PROPERTY_PREFIX`.
- **License header:** existing `octopus-server`/`octopus-common` connection-area files use the Apache 2.0 header block (see any file under `octopus-server/src/main/java/octopus/teamcity/server`). Reuse the same header on new files for consistency.

## File structure

**Create:**
- `octopus-common/src/main/java/octopus/teamcity/common/connection/ConnectionPropertyNames.java` — connection property-key constants.
- `octopus-common/src/test/java/octopus/teamcity/common/connection/ConnectionPropertyNamesTest.java`
- `octopus-common/src/test/java/octopus/teamcity/common/OctopusConstantsConnectionKeyTest.java`
- `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessor.java`
- `octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessorTest.java`
- `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionProvider.java`
- `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionsManager.java`
- `octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionsManagerTest.java`
- `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionController.java`
- `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessor.java`
- `octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessorTest.java`
- `octopus-server/src/main/resources/buildServerResources/connectionSelector.jsp` — shared dropdown + toggle JS fragment.
- `octopus-server/src/main/resources/buildServerResources/forms/` + 5 renamed form JSPs (see Task 8).

**Modify:**
- `octopus-common/src/main/java/octopus/teamcity/common/OctopusConstants.java` — add `getConnectionIdKey()`.
- `octopus-server/src/main/resources/buildServerResources/editOctopusConnection.jsp` — URL/key/version/space (drop proxy).
- `octopus-server/src/main/resources/META-INF/build-server-plugin-Octopus.TeamCity.xml` — register new beans.
- The 5 run types' `getRunnerPropertiesProcessor()` and `getEditRunnerParamsJspFilePath()`:
  `OctopusCreateReleaseRunType`, `OctopusDeployReleaseRunType`, `OctopusPromoteReleaseRunType`, `OctopusPushPackageRunType`, `OctopusBuildInformationRunType`.
- The 5 edit JSPs (renamed to `forms/…Form.jsp`, content adjusted to include the selector and wrap manual fields).
- `octopus-server/build.gradle` — add Mockito test deps if missing.

---

## Task 1: Add the connection-id constant

**Files:**
- Modify: `octopus-common/src/main/java/octopus/teamcity/common/OctopusConstants.java`
- Test: `octopus-common/src/test/java/octopus/teamcity/common/OctopusConstantsConnectionKeyTest.java`

- [ ] **Step 1: Write the failing test**

Create `octopus-common/src/test/java/octopus/teamcity/common/OctopusConstantsConnectionKeyTest.java`:

```java
package octopus.teamcity.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OctopusConstantsConnectionKeyTest {
  @Test
  void exposesConnectionIdKey() {
    assertThat(new OctopusConstants().getConnectionIdKey()).isEqualTo("octopus_connection_id");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :octopus-common:test --tests 'octopus.teamcity.common.OctopusConstantsConnectionKeyTest'`
Expected: FAIL — compile error, `getConnectionIdKey()` undefined.

- [ ] **Step 3: Add the method**

In `OctopusConstants.java`, after `getGitCommitKey()` (around line 166), add:

```java
  public String getConnectionIdKey() {
    return "octopus_connection_id";
  }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :octopus-common:test --tests 'octopus.teamcity.common.OctopusConstantsConnectionKeyTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add octopus-common/src/main/java/octopus/teamcity/common/OctopusConstants.java \
        octopus-common/src/test/java/octopus/teamcity/common/OctopusConstantsConnectionKeyTest.java
git commit -m "feat: add octopus_connection_id constant"
```

---

## Task 2: ConnectionPropertyNames (connection's own keys)

**Files:**
- Create: `octopus-common/src/main/java/octopus/teamcity/common/connection/ConnectionPropertyNames.java`
- Test: `octopus-common/src/test/java/octopus/teamcity/common/connection/ConnectionPropertyNamesTest.java`

- [ ] **Step 1: Write the failing test**

```java
package octopus.teamcity.common.connection;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnectionPropertyNamesTest {
  private final ConnectionPropertyNames keys = new ConnectionPropertyNames();

  @Test
  void exposesExpectedKeys() {
    assertThat(keys.getDisplayName()).isEqualTo("displayName");
    assertThat(keys.getServerUrlPropertyName()).isEqualTo("octopus_host");
    assertThat(keys.getApiKeyPropertyName()).isEqualTo("secure:octopus_apikey");
    assertThat(keys.getVersionPropertyName()).isEqualTo("octopus_version");
    assertThat(keys.getSpaceNamePropertyName()).isEqualTo("octopus_space_name");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :octopus-common:test --tests 'octopus.teamcity.common.connection.ConnectionPropertyNamesTest'`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Create the class**

`octopus-common/src/main/java/octopus/teamcity/common/connection/ConnectionPropertyNames.java`:

```java
/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.common.connection;

import jetbrains.buildServer.agent.Constants;

public class ConnectionPropertyNames {
  // DisplayName is _required_ by TeamCity and must be called "displayName".
  public static final String DISPLAY_NAME = "displayName";
  public static final String SERVER_URL = "octopus_host";
  public static final String API_KEY = Constants.SECURE_PROPERTY_PREFIX + "octopus_apikey";
  public static final String VERSION = "octopus_version";
  public static final String SPACE_NAME = "octopus_space_name";

  public ConnectionPropertyNames() {}

  public String getDisplayName() {
    return DISPLAY_NAME;
  }

  public String getServerUrlPropertyName() {
    return SERVER_URL;
  }

  public String getApiKeyPropertyName() {
    return API_KEY;
  }

  public String getVersionPropertyName() {
    return VERSION;
  }

  public String getSpaceNamePropertyName() {
    return SPACE_NAME;
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :octopus-common:test --tests 'octopus.teamcity.common.connection.ConnectionPropertyNamesTest'`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add octopus-common/src/main/java/octopus/teamcity/common/connection/ConnectionPropertyNames.java \
        octopus-common/src/test/java/octopus/teamcity/common/connection/ConnectionPropertyNamesTest.java
git commit -m "feat: add ConnectionPropertyNames for Octopus connection"
```

---

## Task 3: OctopusConnectionPropertiesProcessor (connection validation)

Validates the connection edit form: URL present + http(s), API key present, version is one of the known versions.

**Files:**
- Create: `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessor.java`
- Test: `octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.Test;

class OctopusConnectionPropertiesProcessorTest {
  private final OctopusConnectionPropertiesProcessor processor =
      new OctopusConnectionPropertiesProcessor();

  private Map<String, String> validProps() {
    final Map<String, String> p = new HashMap<>();
    p.put(ConnectionPropertyNames.SERVER_URL, "https://octopus.example.com");
    p.put(ConnectionPropertyNames.API_KEY, "API-XXXXXXXXXXXXXXXXXXXXXXXX");
    p.put(ConnectionPropertyNames.VERSION, "3.0+");
    return p;
  }

  private List<String> invalidKeys(final Map<String, String> p) {
    return processor.process(p).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  @Test
  void validPropertiesProduceNoErrors() {
    assertThat(processor.process(validProps())).isEmpty();
  }

  @Test
  void missingServerUrlIsInvalid() {
    final Map<String, String> p = validProps();
    p.remove(ConnectionPropertyNames.SERVER_URL);
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void nonHttpServerUrlIsInvalid() {
    final Map<String, String> p = validProps();
    p.put(ConnectionPropertyNames.SERVER_URL, "ftp://octopus.example.com");
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void malformedServerUrlIsInvalid() {
    final Map<String, String> p = validProps();
    p.put(ConnectionPropertyNames.SERVER_URL, "not a url");
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.SERVER_URL);
  }

  @Test
  void missingApiKeyIsInvalid() {
    final Map<String, String> p = validProps();
    p.remove(ConnectionPropertyNames.API_KEY);
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.API_KEY);
  }

  @Test
  void unknownVersionIsInvalid() {
    final Map<String, String> p = validProps();
    p.put(ConnectionPropertyNames.VERSION, "banana");
    assertThat(invalidKeys(p)).contains(ConnectionPropertyNames.VERSION);
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionPropertiesProcessorTest'`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Create the class**

`octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessor.java`:

```java
/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.server.connection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;

public class OctopusConnectionPropertiesProcessor implements PropertiesProcessor {
  private static final ConnectionPropertyNames KEYS = new ConnectionPropertyNames();

  @Override
  public List<InvalidProperty> process(final Map<String, String> properties) {
    final List<InvalidProperty> result = new ArrayList<>();
    if (properties == null) {
      return result;
    }

    validateServerUrl(properties.get(KEYS.getServerUrlPropertyName()), result);

    if (StringUtil.isEmptyOrSpaces(properties.get(KEYS.getApiKeyPropertyName()))) {
      result.add(new InvalidProperty(KEYS.getApiKeyPropertyName(), "API key must be specified"));
    }

    validateVersion(properties.get(KEYS.getVersionPropertyName()), result);

    return result;
  }

  private void validateServerUrl(final String serverUrl, final List<InvalidProperty> result) {
    final String id = KEYS.getServerUrlPropertyName();
    if (StringUtil.isEmptyOrSpaces(serverUrl)) {
      result.add(new InvalidProperty(id, "Server URL must be specified"));
      return;
    }
    try {
      final URL url = new URL(serverUrl);
      if (!"http".equals(url.getProtocol()) && !"https".equals(url.getProtocol())) {
        result.add(new InvalidProperty(id, "Server URL must use the http or https protocol"));
      }
    } catch (final MalformedURLException e) {
      result.add(new InvalidProperty(id, "Illegally formatted URL - " + e.getLocalizedMessage()));
    }
  }

  private void validateVersion(final String version, final List<InvalidProperty> result) {
    final String id = KEYS.getVersionPropertyName();
    if (StringUtil.isEmptyOrSpaces(version)) {
      result.add(new InvalidProperty(id, "Octopus version must be specified"));
      return;
    }
    final boolean known =
        Arrays.asList(OctopusConstants.Instance.getOctopusVersions()).contains(version);
    if (!known) {
      result.add(new InvalidProperty(id, "Unknown Octopus version: " + version));
    }
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionPropertiesProcessorTest'`
Expected: PASS (6 tests)

- [ ] **Step 5: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessor.java \
        octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionPropertiesProcessorTest.java
git commit -m "feat: validate Octopus connection url, api key, and version"
```

---

## Task 4: OctopusConnectionProvider (the OAuthProvider)

Registers the project-level connection type. Not unit-testable in isolation (abstract TeamCity base), so this task is build-verified.

**Files:**
- Create: `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionProvider.java`

- [ ] **Step 1: Create the class**

```java
/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.server.connection;

import java.util.LinkedHashMap;
import java.util.Map;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.jetbrains.annotations.NotNull;

public class OctopusConnection extends OAuthProvider {

  public static final String TYPE = "OctopusConnection";

  private final PluginDescriptor pluginDescriptor;

  public OctopusConnection(final PluginDescriptor pluginDescriptor) {
    this.pluginDescriptor = pluginDescriptor;
  }

  @NotNull
  @Override
  public String getType() {
    return TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "OctopusDeploy Server";
  }

  @Override
  public String getEditParametersUrl() {
    return pluginDescriptor.getPluginResourcesPath("editOctopusConnection.jsp");
  }

  @NotNull
  @Override
  public Map<String, String> getDefaultProperties() {
    final Map<String, String> defaults = new LinkedHashMap<>();
    defaults.put(ConnectionPropertyNames.VERSION, "3.0+");
    return defaults;
  }

  @NotNull
  @Override
  public String describeConnection(@NotNull final OAuthConnectionDescriptor connection) {
    final Map<String, String> params = connection.getParameters();
    final String url = params.getOrDefault(ConnectionPropertyNames.SERVER_URL, "(no URL)");
    final String version = params.getOrDefault(ConnectionPropertyNames.VERSION, "");
    return version.isEmpty()
        ? "Octopus Server URL: " + url
        : "Octopus Server URL: " + url + " (version " + version + ")";
  }

  @Override
  public PropertiesProcessor getPropertiesProcessor() {
    return new OctopusConnectionPropertiesProcessor();
  }
}
```

> **Note on class name:** the file is `OctopusConnectionProvider.java` per the file map, but the `OAuthProvider` subclass is named `OctopusConnection` (matching the `origin/connection` branch and the `TYPE` constant other tasks reference as `OctopusConnection.TYPE`). Put the `public class OctopusConnection` in a file named `OctopusConnection.java`, NOT `OctopusConnectionProvider.java` — Java requires the filename to match the public class. **Action: create the file as `OctopusConnection.java`.** Update the file map mentally; all other tasks reference `OctopusConnection.TYPE`.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :octopus-server:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnection.java
git commit -m "feat: add OctopusConnection OAuthProvider"
```

---

## Task 5: OctopusConnectionsManager (list + resolve)

Wraps TeamCity's `OAuthConnectionsManager`. `listAvailableConnections(user)` returns connections from every project the user can view (deduped by id) for the edit UI; `resolve(project, id)` finds a connection by id walking the project + ancestors for build-time resolution.

**Files:**
- Modify: `octopus-server/build.gradle` (add Mockito test deps if absent)
- Create: `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionsManager.java`
- Test: `octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionsManagerTest.java`

- [ ] **Step 1: Ensure Mockito is on the test classpath**

Open `octopus-server/build.gradle`. In the `dependencies { ... }` block, after the existing `testImplementation` lines (around line 25), add if not present:

```groovy
  testImplementation 'org.mockito:mockito-core'
  testImplementation 'org.mockito:mockito-junit-jupiter'
```

- [ ] **Step 2: Write the failing test**

```java
package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusConnectionsManagerTest {
  @Mock private OAuthConnectionsManager teamcityOAuth;
  @Mock private ProjectManager projectManager;
  @Mock private SProject project;
  @Mock private User user;

  private OctopusConnectionsManager manager;

  @BeforeEach
  void setUp() {
    manager = new OctopusConnectionsManager(teamcityOAuth, projectManager);
  }

  private OAuthConnectionDescriptor connection(final String id) {
    final OAuthConnectionDescriptor d = org.mockito.Mockito.mock(OAuthConnectionDescriptor.class);
    when(d.getId()).thenReturn(id);
    return d;
  }

  @Test
  void listAvailableDedupesById() {
    when(projectManager.getProjects()).thenReturn(Collections.singletonList(project));
    when(project.getProjectId()).thenReturn("p1");
    when(user.isPermissionGrantedForProject("p1", Permission.VIEW_PROJECT)).thenReturn(true);
    final OAuthConnectionDescriptor a = connection("c1");
    final OAuthConnectionDescriptor b = connection("c1");
    when(teamcityOAuth.getAvailableConnectionsOfType(project, OctopusConnection.TYPE))
        .thenReturn(Arrays.asList(a, b));

    final List<OAuthConnectionDescriptor> result = manager.listAvailableConnections(user);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("c1");
  }

  @Test
  void listAvailableSkipsProjectsUserCannotView() {
    when(projectManager.getProjects()).thenReturn(Collections.singletonList(project));
    when(project.getProjectId()).thenReturn("p1");
    when(user.isPermissionGrantedForProject("p1", Permission.VIEW_PROJECT)).thenReturn(false);

    assertThat(manager.listAvailableConnections(user)).isEmpty();
  }

  @Test
  void resolveReturnsConnectionById() {
    final OAuthConnectionDescriptor a = connection("c1");
    when(teamcityOAuth.findConnectionById(project, "c1")).thenReturn(a);

    assertThat(manager.resolve(project, "c1")).contains(a);
  }

  @Test
  void resolveEmptyForBlankId() {
    assertThat(manager.resolve(project, "  ")).isEmpty();
    assertThat(manager.resolve(project, null)).isEmpty();
  }

  @Test
  void resolveEmptyWhenNotFound() {
    when(teamcityOAuth.findConnectionById(project, "missing")).thenReturn(null);
    assertThat(manager.resolve(project, "missing")).isEmpty();
  }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionsManagerTest'`
Expected: FAIL — class does not exist.

- [ ] **Step 4: Create the class**

`octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionsManager.java`:

```java
/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.server.connection;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Permission;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionsManager;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OctopusConnectionsManager {
  private final OAuthConnectionsManager teamcityOAuth;
  private final ProjectManager projectManager;

  public OctopusConnectionsManager(
      final OAuthConnectionsManager teamcityOAuth, final ProjectManager projectManager) {
    this.teamcityOAuth = teamcityOAuth;
    this.projectManager = projectManager;
  }

  @NotNull
  public List<OAuthConnectionDescriptor> listAvailableConnections(@NotNull final User user) {
    return projectManager.getProjects().stream()
        .filter(p -> user.isPermissionGrantedForProject(p.getProjectId(), Permission.VIEW_PROJECT))
        .map(p -> teamcityOAuth.getAvailableConnectionsOfType(p, OctopusConnection.TYPE))
        .flatMap(Collection::stream)
        .filter(distinctByKey(OAuthConnectionDescriptor::getId))
        .collect(Collectors.toList());
  }

  @NotNull
  public Optional<OAuthConnectionDescriptor> resolve(
      @NotNull final SProject project, @Nullable final String connectionId) {
    if (StringUtil.isEmptyOrSpaces(connectionId)) {
      return Optional.empty();
    }
    return Optional.ofNullable(teamcityOAuth.findConnectionById(project, connectionId));
  }

  private static <T> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
    final Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionsManagerTest'`
Expected: PASS (5 tests)

- [ ] **Step 6: Commit**

```bash
git add octopus-server/build.gradle \
        octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionsManager.java \
        octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionsManagerTest.java
git commit -m "feat: add OctopusConnectionsManager for listing and resolving connections"
```

---

## Task 6: OctopusConnectionBuildStartProcessor (resolution + precedence)

At build start, for each Octopus runner with a `octopus_connection_id`, resolve the connection and inject URL/key/version/space into that runner's parameters. **Space precedence:** step value wins if non-empty, else the connection's space.

**Files:**
- Create: `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessor.java`
- Test: `octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessorTest.java`

- [ ] **Step 1: Write the failing test**

```java
package octopus.teamcity.server.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunnerContext;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OctopusConnectionBuildStartProcessorTest {
  private static final OctopusConstants C = new OctopusConstants();

  @Mock private ExtensionHolder extensionHolder;
  @Mock private OctopusConnectionsManager connectionsManager;
  @Mock private BuildStartContext buildStartContext;
  @Mock private SRunnerContext runnerContext;
  @Mock private SRunningBuild build;
  @Mock private SBuildType buildType;
  @Mock private SProject project;

  private OctopusConnectionBuildStartProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new OctopusConnectionBuildStartProcessor(extensionHolder, connectionsManager);
    lenient().when(buildStartContext.getBuild()).thenReturn(build);
    lenient().when(build.getBuildType()).thenReturn(buildType);
    lenient().when(buildType.getProject()).thenReturn(project);
    lenient()
        .when(buildStartContext.getRunnerContexts())
        .thenReturn(Collections.singletonList(runnerContext));
    lenient().when(runnerContext.getType()).thenReturn(OctopusConstants.CREATE_RELEASE_RUNNER_TYPE);
  }

  private OAuthConnectionDescriptor connectionWith(final String url, final String key,
      final String version, final String space) {
    final OAuthConnectionDescriptor d = mock(OAuthConnectionDescriptor.class);
    final Map<String, String> params = new HashMap<>();
    params.put(ConnectionPropertyNames.SERVER_URL, url);
    params.put(ConnectionPropertyNames.API_KEY, key);
    params.put(ConnectionPropertyNames.VERSION, version);
    if (space != null) {
      params.put(ConnectionPropertyNames.SPACE_NAME, space);
    }
    when(d.getParameters()).thenReturn(params);
    return d;
  }

  private void stepParams(final Map<String, String> p) {
    when(runnerContext.getParameters()).thenReturn(p);
  }

  @Test
  void injectsConnectionValuesWhenConnectionSelected() {
    final Map<String, String> p = new HashMap<>();
    p.put(C.getConnectionIdKey(), "c1");
    stepParams(p);
    when(connectionsManager.resolve(project, "c1"))
        .thenReturn(Optional.of(
            connectionWith("https://octo", "API-KEY", "3.0+", "Spaces-1")));

    processor.updateParameters(buildStartContext);

    verify(runnerContext).addRunnerParameter(C.getServerKey(), "https://octo");
    verify(runnerContext).addRunnerParameter(C.getApiKey(), "API-KEY");
    verify(runnerContext).addRunnerParameter(C.getOctopusVersion(), "3.0+");
    verify(runnerContext).addRunnerParameter(C.getSpaceName(), "Spaces-1");
  }

  @Test
  void stepSpaceOverridesConnectionSpace() {
    final Map<String, String> p = new HashMap<>();
    p.put(C.getConnectionIdKey(), "c1");
    p.put(C.getSpaceName(), "StepSpace");
    stepParams(p);
    when(connectionsManager.resolve(project, "c1"))
        .thenReturn(Optional.of(
            connectionWith("https://octo", "API-KEY", "3.0+", "ConnSpace")));

    processor.updateParameters(buildStartContext);

    // Step space is non-empty, so the connection's space must NOT overwrite it.
    verify(runnerContext, never()).addRunnerParameter(C.getSpaceName(), "ConnSpace");
  }

  @Test
  void noInjectionWhenNoConnectionId() {
    stepParams(new HashMap<>());

    processor.updateParameters(buildStartContext);

    verify(runnerContext, never()).addRunnerParameter(
        org.mockito.ArgumentMatchers.eq(C.getServerKey()),
        org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void ignoresNonOctopusRunners() {
    when(runnerContext.getType()).thenReturn("some.other.runner");
    stepParams(new HashMap<>());

    processor.updateParameters(buildStartContext);

    verify(connectionsManager, never()).resolve(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionBuildStartProcessorTest'`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Create the class**

`octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessor.java`:

```java
/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.server.connection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.SRunnerContext;
import jetbrains.buildServer.util.StringUtil;
import octopus.teamcity.common.OctopusConstants;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;

public class OctopusConnectionBuildStartProcessor implements BuildStartContextProcessor {
  private static final OctopusConstants C = new OctopusConstants();
  private static final ConnectionPropertyNames CONN = new ConnectionPropertyNames();
  private static final Set<String> OCTOPUS_RUNNER_TYPES =
      new HashSet<>(
          Arrays.asList(
              OctopusConstants.CREATE_RELEASE_RUNNER_TYPE,
              OctopusConstants.DEPLOY_RELEASE_RUNNER_TYPE,
              OctopusConstants.PROMOTE_RELEASE_RUNNER_TYPE,
              OctopusConstants.PUSH_PACKAGE_RUNNER_TYPE,
              OctopusConstants.METADATA_RUNNER_TYPE));

  private final Logger logger = Loggers.SERVER;
  private final OctopusConnectionsManager connectionsManager;

  public OctopusConnectionBuildStartProcessor(
      final ExtensionHolder extensionHolder,
      final OctopusConnectionsManager connectionsManager) {
    this.connectionsManager = connectionsManager;
    extensionHolder.registerExtension(
        BuildStartContextProcessor.class, this.getClass().getName(), this);
  }

  @Override
  public void updateParameters(final BuildStartContext buildStartContext) {
    final SBuildType buildType = buildStartContext.getBuild().getBuildType();
    final SProject project = buildType == null ? null : buildType.getProject();
    if (project == null) {
      return;
    }

    for (final SRunnerContext runner : buildStartContext.getRunnerContexts()) {
      if (!OCTOPUS_RUNNER_TYPES.contains(runner.getType())) {
        continue;
      }
      final Map<String, String> stepParams = runner.getParameters();
      final String connectionId = stepParams.get(C.getConnectionIdKey());
      if (StringUtil.isEmptyOrSpaces(connectionId)) {
        continue;
      }

      final Optional<OAuthConnectionDescriptor> resolved =
          connectionsManager.resolve(project, connectionId);
      if (!resolved.isPresent()) {
        logger.warn(
            "Octopus connection '" + connectionId + "' referenced by build step could not be resolved");
        continue;
      }

      final Map<String, String> connParams = resolved.get().getParameters();
      setIfPresent(runner, C.getServerKey(), connParams.get(CONN.getServerUrlPropertyName()));
      setIfPresent(runner, C.getApiKey(), connParams.get(CONN.getApiKeyPropertyName()));
      setIfPresent(runner, C.getOctopusVersion(), connParams.get(CONN.getVersionPropertyName()));

      // Space precedence: keep the step's value if it set one; otherwise use the connection's.
      if (StringUtil.isEmptyOrSpaces(stepParams.get(C.getSpaceName()))) {
        setIfPresent(runner, C.getSpaceName(), connParams.get(CONN.getSpaceNamePropertyName()));
      }
    }
  }

  private void setIfPresent(final SRunnerContext runner, final String key, final String value) {
    if (value != null) {
      runner.addRunnerParameter(key, value);
    }
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.connection.OctopusConnectionBuildStartProcessorTest'`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessor.java \
        octopus-server/src/test/java/octopus/teamcity/server/connection/OctopusConnectionBuildStartProcessorTest.java
git commit -m "feat: inject resolved connection values into runner params at build start"
```

---

## Task 7: Rewrite the connection edit JSP (URL/key/version/space)

Replace the orphaned proxy-oriented `editOctopusConnection.jsp` with fields matching `ConnectionPropertyNames`.

**Files:**
- Modify: `octopus-server/src/main/resources/buildServerResources/editOctopusConnection.jsp`

- [ ] **Step 1: Replace the file contents**

```jsp
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%--
  ~ Copyright (c) Octopus Deploy and contributors. All rights reserved.
  ~ Licensed under the Apache License, Version 2.0 (the "License").
  --%>
<jsp:useBean id="keys" class="octopus.teamcity.common.connection.ConnectionPropertyNames"/>
<jsp:useBean id="versionKeys" class="octopus.teamcity.common.OctopusConstants"/>
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<tr>
  <th>Connection Name:</th>
  <td>
    <props:textProperty name="${keys.displayName}" className="longField"/>
    <span class="error" id="error_${keys.displayName}"></span>
    <span class="smallNote">A uniquely distinguishable name for this connection.</span>
  </td>
</tr>
<tr>
  <th>Octopus URL:<l:star/></th>
  <td>
    <props:textProperty name="${keys.serverUrlPropertyName}" className="longField"/>
    <span class="error" id="error_${keys.serverUrlPropertyName}"></span>
    <span class="smallNote">Specify the Octopus server URL (e.g. http(s)://{hostname}:{port}).</span>
  </td>
</tr>
<tr>
  <th>API key:<l:star/></th>
  <td>
    <props:passwordProperty name="${keys.apiKeyPropertyName}" className="longField"/>
    <span class="error" id="error_${keys.apiKeyPropertyName}"></span>
    <span class="smallNote">You can get this from your user page in the Octopus web portal.</span>
  </td>
</tr>
<tr>
  <th>Octopus version:<l:star/></th>
  <td>
    <props:selectProperty name="${keys.versionPropertyName}" multiple="false">
      <c:forEach var="version" items="${versionKeys.octopusVersions}">
        <props:option value="${version}"><c:out value="${version}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="error" id="error_${keys.versionPropertyName}"></span>
    <span class="smallNote">Which version of the Octopus Deploy server are you using?</span>
  </td>
</tr>
<tr>
  <th>Space name:</th>
  <td>
    <props:textProperty name="${keys.spaceNamePropertyName}" className="longField"/>
    <span class="error" id="error_${keys.spaceNamePropertyName}"></span>
    <span class="smallNote">Optional default Space. Individual build steps may override this.</span>
  </td>
</tr>
```

- [ ] **Step 2: Verify the module still builds**

Run: `./gradlew :octopus-server:compileJava`
Expected: BUILD SUCCESSFUL (JSP isn't compiled here, but ensures no stale references).

- [ ] **Step 3: Commit**

```bash
git add octopus-server/src/main/resources/buildServerResources/editOctopusConnection.jsp
git commit -m "feat: connection edit form for url, api key, version, and space"
```

---

## Task 8: Connection selector fragment + per-step JSP wiring

The controller (Task 9) forwards each step's edit request to a renamed *form* JSP and supplies `octopusConnections` (list of maps: `id`, `displayName`, `url`, `version`, `space`) plus `editConnectionUrl`. Each form JSP includes a shared `connectionSelector.jsp` fragment that renders the dropdown and toggles the manual fields.

**Design of the fragment:**
- A `selectProperty` bound to `${keys.connectionIdKey}` with a blank option `-- Enter connection details manually --` plus one option per connection.
- All manual rows (URL, API key, version) in each form JSP get `class="octopusManualField"`.
- The fragment's JS hides every `.octopusManualField` row when a connection is chosen, shows them when blank. When a connection is chosen and the Space field (`${versionKeys.spaceName}`) is empty, it fills the Space field from the chosen connection's space (per the spec's UI note).

**Files:**
- Create: `octopus-server/src/main/resources/buildServerResources/connectionSelector.jsp`
- Rename (git mv) + modify each of the 5 edit JSPs into `forms/`:
  - `editOctopusCreateRelease.jsp` → `forms/editOctopusCreateReleaseForm.jsp`
  - `editOctopusDeployRelease.jsp` → `forms/editOctopusDeployReleaseForm.jsp`
  - `editOctopusPromoteRelease.jsp` → `forms/editOctopusPromoteReleaseForm.jsp`
  - `editOctopusPushPackage.jsp` → `forms/editOctopusPushPackageForm.jsp`
  - `editOctopusBuildInformation.jsp` → `forms/editOctopusBuildInformationForm.jsp`

- [ ] **Step 1: Create the shared fragment**

`octopus-server/src/main/resources/buildServerResources/connectionSelector.jsp`:

```jsp
<%@ include file="/include-internal.jsp" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- Requires request attributes: octopusConnections (List<Map>), editConnectionUrl (String).
     Requires page beans: keys (OctopusConstants), and a Space field named ${keys.spaceName}. --%>

<tr>
  <th>Connection:</th>
  <td>
    <props:selectProperty name="${keys.connectionIdKey}" id="octopusConnectionId" className="longField">
      <props:option value="">-- Enter connection details manually --</props:option>
      <c:forEach var="conn" items="${octopusConnections}">
        <props:option value="${conn.id}"><c:out value="${conn.displayName}"/></props:option>
      </c:forEach>
    </props:selectProperty>
    <span class="smallNote">
      Reuse a connection defined under
      <a href="${editConnectionUrl}" target="_blank">Project Settings &raquo; Connections</a>,
      or leave blank to enter details manually below.
    </span>
  </td>
</tr>

<script type="text/javascript">
  (function () {
    var octopusConnSpaces = {};
    <c:forEach var="conn" items="${octopusConnections}">
      octopusConnSpaces["${conn.id}"] = "${conn.space}";
    </c:forEach>

    function octopusToggleManualFields() {
      var select = document.getElementById("octopusConnectionId");
      if (!select) return;
      var usingConnection = select.value !== "";
      var rows = document.querySelectorAll("tr.octopusManualField");
      for (var i = 0; i < rows.length; i++) {
        rows[i].style.display = usingConnection ? "none" : "table-row";
      }
      if (usingConnection) {
        var spaceField = document.getElementById("${keys.spaceName}");
        if (spaceField && spaceField.value === "" && octopusConnSpaces[select.value]) {
          spaceField.value = octopusConnSpaces[select.value];
        }
      }
    }

    $j(document).ready(function () {
      var select = document.getElementById("octopusConnectionId");
      if (select) {
        select.addEventListener("change", octopusToggleManualFields);
        octopusToggleManualFields();
      }
    });

    // Exposed so step-specific scripts (e.g. Create release git-ref) can react.
    window.octopusSelectedConnectionId = function () {
      var s = document.getElementById("octopusConnectionId");
      return s ? s.value : "";
    };
  })();
</script>
```

- [ ] **Step 2: Move the 5 JSPs into `forms/` and add the selector**

For each step JSP, run the git move, then edit:

```bash
cd octopus-server/src/main/resources/buildServerResources
mkdir -p forms
git mv editOctopusCreateRelease.jsp forms/editOctopusCreateReleaseForm.jsp
git mv editOctopusDeployRelease.jsp forms/editOctopusDeployReleaseForm.jsp
git mv editOctopusPromoteRelease.jsp forms/editOctopusPromoteReleaseForm.jsp
git mv editOctopusPushPackage.jsp forms/editOctopusPushPackageForm.jsp
git mv editOctopusBuildInformation.jsp forms/editOctopusBuildInformationForm.jsp
cd -
```

Then in **each** moved form JSP, inside the existing `<l:settingsGroup title="Octopus Connection">`:

  (a) Immediately after the `<l:settingsGroup title="Octopus Connection">` line, add the fragment include:

```jsp
  <jsp:include page="../connectionSelector.jsp"/>
```

  (b) Add `class="octopusManualField"` to the `<tr>` rows that contain the **Octopus URL**, **API key**, and **Octopus version** props. Leave the **Space name** row WITHOUT that class (it stays visible). Example, in `forms/editOctopusCreateReleaseForm.jsp` the URL row becomes:

```jsp
<tr class="octopusManualField">
  <th>Octopus URL:<l:star/></th>
  <td>
    <props:textProperty name="${keys.serverKey}" className="longField"/>
    <span class="error" id="error_${keys.serverKey}"></span>
    <span class="smallNote">Specify Octopus web portal URL</span>
  </td>
</tr>
```

  Apply the same `class="octopusManualField"` to the API-key `<tr>` and (where present — create/deploy/promote only) the Octopus-version `<tr>`. Push package and build information JSPs have no version row; just tag their URL and API-key rows.

- [ ] **Step 3: Update Create release's git-ref version logic**

In `forms/editOctopusCreateReleaseForm.jsp`, the existing `showHideGitRefField()` reads the version `<select>`. When a connection is selected the version row is hidden, so derive the effective version from the connection. Add a connection→version map and prefer it. Replace the `<script>` block's `showHideGitRefField` with:

```javascript
    var octopusConnVersions = {};
    <c:forEach var="conn" items="${octopusConnections}">
      octopusConnVersions["${conn.id}"] = "${conn.version}";
    </c:forEach>

    function octopusEffectiveVersion() {
      var connId = window.octopusSelectedConnectionId ? window.octopusSelectedConnectionId() : "";
      if (connId && octopusConnVersions[connId]) {
        return octopusConnVersions[connId];
      }
      return document.getElementById("${keys.octopusVersion}").value;
    }

    function showHideGitRefField() {
        const gitRefRow  = document.getElementById("gitRefRow");
        const gitCommitRow  = document.getElementById("gitCommitRow");
        const versionSelected = octopusEffectiveVersion();
        let gitProjectsAreSupported = versionSelected === "${keys.version3}" || versionSelected === "${keys.previewVersion}";
        if (gitProjectsAreSupported) {
            gitRefRow.style.display = "table-row";
            gitCommitRow.style.display = "table-row";
        } else {
            gitRefRow.style.display = "none";
            gitCommitRow.style.display = "none";
            document.getElementById("${keys.gitRefKey}").value = null
            document.getElementById("${keys.gitCommitKey}").value = null
        }
    }
```

And in the existing `$j(document).ready(...)` of that file, also bind the connection dropdown so changing it re-evaluates git-ref visibility:

```javascript
        var octoConnSelect = document.getElementById("octopusConnectionId");
        if (octoConnSelect) {
            octoConnSelect.addEventListener("change", showHideGitRefField);
        }
```

- [ ] **Step 4: Build check**

Run: `./gradlew :octopus-server:compileJava`
Expected: BUILD SUCCESSFUL (verifies no Java referenced the old JSP names in a way that breaks compilation; JSP path strings are updated in Task 10).

- [ ] **Step 5: Commit**

```bash
git add octopus-server/src/main/resources/buildServerResources/
git commit -m "feat: add connection selector and manual-field toggle to step forms"
```

---

## Task 9: OctopusConnectionController (serve step forms with connections)

Registers one controller against each step's *edit path* (Task 10 makes the run types return those paths). It puts the available connections (as simple maps) and the connections admin URL into the model, then forwards to the renamed form JSP. Not unit-testable; build- and manually-verified.

**Files:**
- Create: `octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionController.java`

- [ ] **Step 1: Create the class**

```java
/*
 * Copyright (c) Octopus Deploy and contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 *  these files except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package octopus.teamcity.server.connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import octopus.teamcity.common.connection.ConnectionPropertyNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

public class OctopusConnectionController extends BaseController {
  private static final ConnectionPropertyNames CONN = new ConnectionPropertyNames();

  // Maps the registered controller path (the run type's edit path) -> physical form JSP.
  private static final Map<String, String> PATH_TO_FORM = new HashMap<>();

  static {
    PATH_TO_FORM.put("editOctopusCreateRelease.jsp", "forms/editOctopusCreateReleaseForm.jsp");
    PATH_TO_FORM.put("editOctopusDeployRelease.jsp", "forms/editOctopusDeployReleaseForm.jsp");
    PATH_TO_FORM.put("editOctopusPromoteRelease.jsp", "forms/editOctopusPromoteReleaseForm.jsp");
    PATH_TO_FORM.put("editOctopusPushPackage.jsp", "forms/editOctopusPushPackageForm.jsp");
    PATH_TO_FORM.put("editOctopusBuildInformation.jsp", "forms/editOctopusBuildInformationForm.jsp");
  }

  private final OctopusConnectionsManager connectionsManager;
  private final ProjectManager projectManager;
  private final PluginDescriptor pluginDescriptor;
  private final WebLinks webLinks;

  public OctopusConnectionController(
      final WebControllerManager webControllerManager,
      final OctopusConnectionsManager connectionsManager,
      final ProjectManager projectManager,
      final PluginDescriptor pluginDescriptor,
      final SBuildServer server,
      final WebLinks webLinks) {
    super(server);
    this.connectionsManager = connectionsManager;
    this.projectManager = projectManager;
    this.pluginDescriptor = pluginDescriptor;
    this.webLinks = webLinks;

    for (final String path : PATH_TO_FORM.keySet()) {
      webControllerManager.registerController(
          pluginDescriptor.getPluginResourcesPath(path), this);
    }
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(
      @NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) {
    final String pathInfo = request.getPathInfo();
    String formJsp = null;
    for (final Map.Entry<String, String> e : PATH_TO_FORM.entrySet()) {
      if (pathInfo != null && pathInfo.endsWith(e.getKey())) {
        formJsp = e.getValue();
        break;
      }
    }
    if (formJsp == null) {
      formJsp = "forms/editOctopusCreateReleaseForm.jsp"; // defensive fallback
    }

    final ModelAndView modelAndView =
        new ModelAndView(pluginDescriptor.getPluginResourcesPath(formJsp));

    final SUser user = SessionUser.getUser(request);
    final List<Map<String, String>> connections = new ArrayList<>();
    if (user != null) {
      for (final OAuthConnectionDescriptor d : connectionsManager.listAvailableConnections(user)) {
        final Map<String, String> params = d.getParameters();
        final Map<String, String> view = new HashMap<>();
        view.put("id", d.getId());
        view.put("displayName", d.getConnectionDisplayName());
        view.put("url", params.getOrDefault(CONN.getServerUrlPropertyName(), ""));
        view.put("version", params.getOrDefault(CONN.getVersionPropertyName(), ""));
        view.put("space", params.getOrDefault(CONN.getSpaceNamePropertyName(), ""));
        connections.add(view);
      }
    }

    modelAndView.addObject("octopusConnections", connections);
    modelAndView.addObject(
        "editConnectionUrl",
        webLinks.getEditProjectPageUrl("_Root") + "&tab=oauthConnections");
    return modelAndView;
  }
}
```

> **Why a form-name map:** TeamCity routes the step's edit-include through the Spring controller registered at the run type's edit path. The controller must forward to a *different* physical JSP file (the renamed `forms/…Form.jsp`) to avoid the controller re-intercepting its own view. This mirrors the `origin/connection` branch, which registered at `editOctopusGeneric.jsp` and forwarded to `v2/editOctopusGeneric.jsp`.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :octopus-server:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/connection/OctopusConnectionController.java
git commit -m "feat: controller supplying available connections to step edit forms"
```

---

## Task 10: Run types — keep edit paths, relax validation to connection-OR-manual

The run types must (a) still return the *original* edit JSP path (now the controller's registered path) and (b) accept a step that has only a connection. The five edit paths are unchanged strings (`editOctopusCreateRelease.jsp` etc.) — they already match the controller's `PATH_TO_FORM` keys, so **no change to `getEditRunnerParamsJspFilePath()` is required**; the physical files moved, but the controller now serves those paths.

Each run type's `PropertiesProcessor.process()` changes so that the URL/API-key (and, where validated, version) checks are skipped when `octopus_connection_id` is set.

**Files & tests (repeat the pattern for all five):**
- Modify + Test: `OctopusCreateReleaseRunType`, `OctopusDeployReleaseRunType`, `OctopusPromoteReleaseRunType`, `OctopusPushPackageRunType`, `OctopusBuildInformationRunType`.

- [ ] **Step 1: Write a failing validation test (Create release shown; replicate for the other four)**

`octopus-server/src/test/java/octopus/teamcity/server/OctopusCreateReleaseRunTypeValidationTest.java`:

```java
package octopus.teamcity.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class OctopusCreateReleaseRunTypeValidationTest {
  private static final OctopusConstants C = new OctopusConstants();

  private Collection<String> validate(final Map<String, String> p) {
    final OctopusCreateReleaseRunType runType =
        new OctopusCreateReleaseRunType(mock(RunTypeRegistry.class), mock(PluginDescriptor.class));
    return runType.getRunnerPropertiesProcessor().process(p).stream()
        .map(InvalidProperty::getPropertyName)
        .collect(Collectors.toList());
  }

  @Test
  void connectionOnlyIsValidForServerAndKey() {
    final Map<String, String> p = new HashMap<>();
    p.put(C.getConnectionIdKey(), "c1");
    p.put(C.getProjectNameKey(), "MyProject");
    final Collection<String> errors = validate(p);
    assertThat(errors).doesNotContain(C.getServerKey(), C.getApiKey());
  }

  @Test
  void manualOnlyIsValid() {
    final Map<String, String> p = new HashMap<>();
    p.put(C.getServerKey(), "https://octo");
    p.put(C.getApiKey(), "API-KEY");
    p.put(C.getProjectNameKey(), "MyProject");
    assertThat(validate(p)).doesNotContain(C.getServerKey(), C.getApiKey());
  }

  @Test
  void neitherConnectionNorManualIsInvalid() {
    final Map<String, String> p = new HashMap<>();
    p.put(C.getProjectNameKey(), "MyProject");
    assertThat(validate(p)).contains(C.getServerKey(), C.getApiKey());
  }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.OctopusCreateReleaseRunTypeValidationTest'`
Expected: FAIL — `connectionOnlyIsValidForServerAndKey` fails (server/api-key still required).

- [ ] **Step 3: Update the processor logic in each run type**

In `OctopusCreateReleaseRunType.getRunnerPropertiesProcessor()`, change the body of `process()` so the server/api-key checks are gated on the absence of a connection id. Replace the existing block:

```java
        checkNotEmpty(p, c.getApiKey(), "API key must be specified", result);
        checkNotEmpty(p, c.getServerKey(), "Server must be specified", result);
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
```

with:

```java
        final boolean usingConnection =
            !jetbrains.buildServer.util.StringUtil.isEmptyOrSpaces(p.get(c.getConnectionIdKey()));
        if (!usingConnection) {
          checkNotEmpty(p, c.getApiKey(), "API key must be specified", result);
          checkNotEmpty(p, c.getServerKey(), "Server must be specified", result);
        }
        checkNotEmpty(p, c.getProjectNameKey(), "Project name must be specified", result);
```

Apply the equivalent change in the other four run types, gating ONLY the `getApiKey()` and `getServerKey()` (and any `getOctopusVersion()` requirement, if present) checks behind `!usingConnection`. Keep all non-credential checks (project name, package id, package version, etc.) unconditional. Reference current validation bodies:
- `OctopusDeployReleaseRunType` / `OctopusPromoteReleaseRunType`: gate apiKey + serverKey.
- `OctopusPushPackageRunType`: gate apiKey + serverKey; keep package-related checks unconditional.
- `OctopusBuildInformationRunType`: gate apiKey + serverKey (lines ~79-80); keep packageId/packageVersion checks unconditional.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.OctopusCreateReleaseRunTypeValidationTest'`
Expected: PASS (3 tests)

- [ ] **Step 5: Add equivalent validation tests for the other four run types**

Create the analogous test classes, copying the structure from Step 1 but constructing the matching run type and including each type's mandatory non-credential fields:
- `OctopusDeployReleaseRunTypeValidationTest` (deploy; mandatory: project name — confirm against its current `process()`).
- `OctopusPromoteReleaseRunTypeValidationTest` (promote; mandatory: project name + promote-from — confirm against its current `process()`).
- `OctopusPushPackageRunTypeValidationTest` (push; mandatory: package paths — confirm against its current `process()`).
- `OctopusBuildInformationRunTypeValidationTest` (build info; mandatory: package id + package version).

For each, assert the same three behaviours: connection-only valid (no server/key errors), manual-only valid, neither invalid (server + key errors present). Read each run type's current `process()` to list the exact non-credential required keys to populate in the "valid" cases.

- [ ] **Step 6: Run all five validation tests**

Run: `./gradlew :octopus-server:test --tests 'octopus.teamcity.server.*RunTypeValidationTest'`
Expected: PASS (all)

- [ ] **Step 7: Commit**

```bash
git add octopus-server/src/main/java/octopus/teamcity/server/ \
        octopus-server/src/test/java/octopus/teamcity/server/
git commit -m "feat: accept a connection in place of manual url/api key in step validation"
```

---

## Task 11: Register the new server beans

**Files:**
- Modify: `octopus-server/src/main/resources/META-INF/build-server-plugin-Octopus.TeamCity.xml`

- [ ] **Step 1: Add bean definitions**

Inside `<beans>...</beans>`, add:

```xml
    <bean class="octopus.teamcity.server.connection.OctopusConnection"/>
    <bean class="octopus.teamcity.server.connection.OctopusConnectionsManager"/>
    <bean class="octopus.teamcity.server.connection.OctopusConnectionController"/>
    <bean class="octopus.teamcity.server.connection.OctopusConnectionBuildStartProcessor"/>
```

(Constructor autowiring is already enabled via `default-autowire="constructor"`. `OAuthConnectionsManager`, `WebControllerManager`, `ProjectManager`, `SBuildServer`, `WebLinks`, `ExtensionHolder`, and `PluginDescriptor` are all supplied by TeamCity's container.)

- [ ] **Step 2: Build the whole server module + tests**

Run: `./gradlew :octopus-common:test :octopus-server:test`
Expected: BUILD SUCCESSFUL; all new tests pass.

- [ ] **Step 3: Commit**

```bash
git add octopus-server/src/main/resources/META-INF/build-server-plugin-Octopus.TeamCity.xml
git commit -m "feat: register Octopus connection provider, manager, controller, and build start processor"
```

---

## Task 12: Full build + packaging sanity

- [ ] **Step 1: Build the distributable plugin zip**

Run: `./gradlew clean build`
Expected: BUILD SUCCESSFUL; `build/distributions/Octopus.TeamCity.<version>.zip` produced.

- [ ] **Step 2: Confirm the renamed JSPs and new resources are packaged**

Run: `./gradlew :octopus-server:installDist && find octopus-server/build/install -name 'editOctopusConnection.jsp' -o -name 'connectionSelector.jsp' -o -path '*forms*'`
Expected: lists `editOctopusConnection.jsp`, `connectionSelector.jsp`, and the five `forms/…Form.jsp` files.

- [ ] **Step 3: Commit (if any packaging tweaks were needed; otherwise skip)**

---

## Task 13: Manual verification (real TeamCity)

The JSP/controller/OAuth wiring cannot be unit-tested; verify in a running server. `docker-compose.teamcity.yml` exists for a local server.

- [ ] **Step 1:** Deploy the built plugin zip to a TeamCity server and restart.
- [ ] **Step 2:** Under a project's **Settings → Connections**, add a new connection → confirm **"OctopusDeploy Server"** appears; fill URL/API key/version/optional space; save; confirm validation rejects a bad URL and an empty API key.
- [ ] **Step 3:** In a build configuration, add an **OctopusDeploy: Create release** step. Confirm the **Connection** dropdown lists the connection. Selecting it hides URL/API key/version; the Space field is auto-filled from the connection when empty. Selecting "-- Enter connection details manually --" restores the manual fields.
- [ ] **Step 4:** With a connection selected, confirm Create release's **Git Ref / Git Commit** rows show/hide correctly based on the connection's version (3.0+/Preview → shown).
- [ ] **Step 5:** Run a build using a connection. In the build log / agent, confirm the CLI runs against the connection's URL and the **API key is masked** in logs. Confirm a second step with a manual configuration (no connection) still works unchanged.
- [ ] **Step 6:** Confirm an existing pre-upgrade step (manual fields, no connection) still runs unchanged (backward compatibility).

---

## Self-review notes (for the implementer)

- **Spec coverage:** connection fields (Task 2, 7), provider (4), manager (5), controller (9), build-start injection + precedence (6), per-step UI incl. empty-space auto-fill and git-ref version (8), validation either/or (10), bean registration (11), all five steps covered (8, 10). Backward compatibility verified in 10 (tests) and 13 (manual).
- **Masking caveat:** the API key is stored in the connection under the `secure:` key and injected under the same `secure:octopus_apikey` runner key the agent already reads. If manual verification (Task 13 Step 5) shows the key is NOT masked or NOT readable from `descriptor.getParameters()`, treat that as a defect to fix before merge — TeamCity may expose secure OAuth params differently; the fix would be to read the secure value via the connection descriptor's secure-parameter accessor rather than `getParameters()`. Flag and stop if observed.
- **Naming consistency:** the `OAuthProvider` subclass is `OctopusConnection` (file `OctopusConnection.java`), `TYPE = "OctopusConnection"`, referenced as `OctopusConnection.TYPE` in Tasks 5, 6 setup. Manager methods: `listAvailableConnections(User)`, `resolve(SProject, String)`. Constant: `getConnectionIdKey()` → `octopus_connection_id`.
