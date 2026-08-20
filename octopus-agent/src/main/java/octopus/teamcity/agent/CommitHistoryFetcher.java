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
package octopus.teamcity.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jetbrains.buildServer.agent.BuildProgressLogger;
import octopus.teamcity.common.Commit;
import org.apache.commons.io.IOUtils;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.Change;

public class CommitHistoryFetcher {

  static final int PAGE_SIZE = 1000;
  static final int MAX_COMMITS = 10000;
  static final int TEAMCITY_DEFAULT_PAGE_SIZE = 100;
  static final int ATTEMPTS_PER_PAGE = 3;
  static final long RETRY_DELAY_MILLIS = 1000L;

  private static final Gson GSON = new GsonBuilder().create();

  interface ChangePageRequester {
    String get(String url) throws IOException;
  }

  interface RetryDelay {
    void pause(int failedAttempts) throws InterruptedException;
  }

  private final String serverUrl;
  private final BuildProgressLogger logger;
  private final ChangePageRequester requester;
  private final RetryDelay retryDelay;

  public CommitHistoryFetcher(
      final String serverUrl,
      final String accessUser,
      final String accessCode,
      final BuildProgressLogger logger) {
    this(
        serverUrl,
        logger,
        new BasicAuthRequester(accessUser, accessCode),
        failedAttempts -> Thread.sleep(failedAttempts * RETRY_DELAY_MILLIS));
  }

  CommitHistoryFetcher(
      final String serverUrl,
      final BuildProgressLogger logger,
      final ChangePageRequester requester,
      final RetryDelay retryDelay) {
    this.serverUrl =
        serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    this.logger = logger;
    this.requester = requester;
    this.retryDelay = retryDelay;
  }

  public CommitHistory fetch(final Build build, final long buildId) {
    final List<Commit> commits = new ArrayList<>();
    try {
      return fetchAllPages(buildId, commits);
    } catch (Exception ex) {
      if (!commits.isEmpty()) {
        logger.warning(
            "Unable to read the rest of the change list from the TeamCity REST API ("
                + ex
                + "). Keeping the "
                + commits.size()
                + " commits already read.");
        return new CommitHistory(commits, truncationWarning(commits.size()));
      }
      logger.warning(
          "Unable to read the change list from the TeamCity REST API ("
              + ex
              + "). Falling back to the first page of changes only.");
      final List<Commit> fallback = toCommits(build.fetchChanges());
      return new CommitHistory(fallback, fallbackWarning(fallback.size()));
    }
  }

  // The client library requests changes without a count, so TeamCity caps it at its default page.
  private static String fallbackWarning(final int commitCount) {
    if (commitCount < TEAMCITY_DEFAULT_PAGE_SIZE) {
      return null;
    }
    return truncationWarning(commitCount);
  }

  private static String truncationWarning(final int commitCount) {
    return "The full change list could not be read from TeamCity, so only the first "
        + commitCount
        + " commits of this build were included in the build information.";
  }

  private CommitHistory fetchAllPages(final long buildId, final List<Commit> commits)
      throws IOException {
    int start = 0;
    while (true) {
      final List<Commit> page = parsePage(getWithRetries(changesUrl(buildId, start)));
      commits.addAll(page);

      if (page.size() < PAGE_SIZE) {
        return new CommitHistory(commits, null);
      }
      // Only a page past the cap proves commits were left out; exactly MAX_COMMITS is complete.
      if (commits.size() > MAX_COMMITS) {
        return new CommitHistory(
            new ArrayList<>(commits.subList(0, MAX_COMMITS)),
            "Only the first "
                + MAX_COMMITS
                + " commits of this build were included in the build information.");
      }
      start += PAGE_SIZE;
    }
  }

  // A page can fail on a transient 500 or a read timeout, so give each one a few attempts before
  // settling for whatever has been read so far.
  private String getWithRetries(final String url) throws IOException {
    IOException lastFailure = null;
    for (int attempt = 1; attempt <= ATTEMPTS_PER_PAGE; attempt++) {
      try {
        return requester.get(url);
      } catch (IOException ex) {
        lastFailure = ex;
        if (attempt < ATTEMPTS_PER_PAGE) {
          logger.warning(
              "Reading a page of changes from the TeamCity REST API failed ("
                  + ex
                  + "). Retrying, attempt "
                  + (attempt + 1)
                  + " of "
                  + ATTEMPTS_PER_PAGE
                  + ".");
          pauseBeforeRetry(attempt);
        }
      }
    }
    throw lastFailure;
  }

  private void pauseBeforeRetry(final int failedAttempts) throws IOException {
    try {
      retryDelay.pause(failedAttempts);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting to retry the change list request", ex);
    }
  }

  private String changesUrl(final long buildId, final int start) {
    // Without an explicit count TeamCity returns only its default page of 100 changes.
    return serverUrl
        + "/httpAuth/app/rest/changes?locator=build:(id:"
        + buildId
        + "),count:"
        + PAGE_SIZE
        + ",start:"
        + start
        + "&fields=change(version,comment)";
  }

  private static List<Commit> parsePage(final String body) {
    final ChangesPage page = GSON.fromJson(body, ChangesPage.class);
    final List<Commit> commits = new ArrayList<>();
    if (page == null || page.change == null) {
      return commits;
    }
    for (final ChangeJson change : page.change) {
      commits.add(toCommit(change.version, change.comment));
    }
    return commits;
  }

  private static List<Commit> toCommits(final List<Change> changes) {
    final List<Commit> commits = new ArrayList<>();
    for (final Change change : changes) {
      commits.add(toCommit(change.getVersion(), change.getComment()));
    }
    return commits;
  }

  private static Commit toCommit(final String version, final String comment) {
    final Commit commit = new Commit();
    commit.Id = version;
    commit.Comment = comment;
    return commit;
  }

  private static final class ChangesPage {
    List<ChangeJson> change;
  }

  private static final class ChangeJson {
    String version;
    String comment;
  }

  private static final class BasicAuthRequester implements ChangePageRequester {

    private static final int TIMEOUT_MILLIS = 30000;

    private final String authorization;

    BasicAuthRequester(final String accessUser, final String accessCode) {
      final byte[] credentials = (accessUser + ":" + accessCode).getBytes(StandardCharsets.UTF_8);
      this.authorization = "Basic " + Base64.getEncoder().encodeToString(credentials);
    }

    @Override
    public String get(final String url) throws IOException {
      final HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      try {
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Accept", "application/json");

        final int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK) {
          throw new IOException("TeamCity responded with HTTP " + statusCode + " for " + url);
        }
        try (InputStream responseStream = connection.getInputStream()) {
          return IOUtils.toString(responseStream, StandardCharsets.UTF_8);
        }
      } finally {
        connection.disconnect();
      }
    }
  }
}
