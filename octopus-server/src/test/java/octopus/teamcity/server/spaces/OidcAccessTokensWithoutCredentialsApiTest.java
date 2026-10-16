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

package octopus.teamcity.server.spaces;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.google.common.io.ByteStreams;
import octopus.teamcity.server.connection.OctopusConnectionsManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * TeamCity servers before 2023.05 have no connection credentials API. Loads this package's classes
 * with the credentials API hidden, as such a server would, and checks that OIDC lookups are turned
 * off rather than the controller failing to start.
 */
class OidcAccessTokensWithoutCredentialsApiTest {

  private static final String SPACES_PACKAGE = "octopus.teamcity.server.spaces.";
  private static final String CREDENTIALS_API_PACKAGE =
      "jetbrains.buildServer.serverSide.connections.credentials.";

  /** Defines this package's classes itself, and hides the credentials API from them. */
  private static final class ServerWithoutCredentialsApi extends ClassLoader {
    ServerWithoutCredentialsApi() {
      super(OidcAccessTokensWithoutCredentialsApiTest.class.getClassLoader());
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve)
        throws ClassNotFoundException {
      if (name.startsWith(CREDENTIALS_API_PACKAGE)) {
        throw new ClassNotFoundException(name);
      }
      if (!name.startsWith(SPACES_PACKAGE)) {
        return super.loadClass(name, resolve);
      }
      synchronized (getClassLoadingLock(name)) {
        final Class<?> loaded = findLoadedClass(name);
        if (loaded != null) {
          return loaded;
        }
        final String resource = name.replace('.', '/') + ".class";
        try (InputStream in = getParent().getResourceAsStream(resource)) {
          if (in == null) {
            throw new ClassNotFoundException(name);
          }
          final byte[] bytes = ByteStreams.toByteArray(in);
          return defineClass(name, bytes, 0, bytes.length);
        } catch (final IOException e) {
          throw new ClassNotFoundException(name, e);
        }
      }
    }
  }

  @Test
  void findsTheCredentialsApiWhenTheServerHasIt() {
    assertThat(ConnectionCredentialsApi.isPresent()).isTrue();
  }

  @Test
  void turnsOidcLookupsOffWhenTheServerHasNoCredentialsApi() throws Exception {
    final Class<?> accessTokensClass =
        new ServerWithoutCredentialsApi().loadClass(SPACES_PACKAGE + "OidcAccessTokens");
    final Constructor<?> constructor =
        accessTokensClass.getDeclaredConstructor(
            OctopusConnectionsManager.class, ApplicationContext.class);
    constructor.setAccessible(true);

    final Object accessTokens = constructor.newInstance(null, null);

    final Field idTokens = accessTokensClass.getDeclaredField("idTokens");
    idTokens.setAccessible(true);
    assertThat(idTokens.get(accessTokens).getClass().getName())
        .doesNotContain("OidcIdTokenRequester");
  }
}
