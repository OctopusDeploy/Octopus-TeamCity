package octopus.teamcity.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jetbrains.buildServer.agent.BuildProgressLogger;
import octopus.teamcity.common.Commit;
import org.jetbrains.teamcity.rest.Build;
import org.jetbrains.teamcity.rest.Change;
import org.junit.jupiter.api.Test;

class CommitHistoryFetcherTest {

  private static final String SERVER_URL = "https://teamcity.example.com/";
  private static final long BUILD_ID = 42L;

  private final BuildProgressLogger logger = mock(BuildProgressLogger.class);
  private final List<String> requestedUrls = new ArrayList<>();

  @Test
  void returnsEveryCommitFromASinglePage() {
    final CommitHistory history = fetcherReturningPagesOf(3).fetch(mock(Build.class), BUILD_ID);

    assertThat(history.getCommits())
        .extracting(commit -> commit.Id)
        .containsExactly("commit-0", "commit-1", "commit-2");
    assertThat(history.getIncompleteDataWarning()).isNull();
    assertThat(requestedUrls).hasSize(1);
  }

  @Test
  void pagesUntilAPageIsShorterThanThePageSize() {
    final CommitHistory history =
        fetcherReturningPagesOf(CommitHistoryFetcher.PAGE_SIZE, CommitHistoryFetcher.PAGE_SIZE, 250)
            .fetch(mock(Build.class), BUILD_ID);

    assertThat(history.getCommits()).hasSize(2250);
    assertThat(history.getCommits().get(0).Id).isEqualTo("commit-0");
    assertThat(history.getCommits().get(1000).Id).isEqualTo("commit-1000");
    assertThat(history.getCommits().get(2249).Id).isEqualTo("commit-2249");
    assertThat(history.getIncompleteDataWarning()).isNull();
    assertThat(requestedUrls)
        .hasSize(3)
        .allSatisfy(url -> assertThat(url).contains("count:" + CommitHistoryFetcher.PAGE_SIZE));
    assertThat(requestedUrls.get(0)).contains(",start:0&");
    assertThat(requestedUrls.get(1)).contains(",start:1000&");
    assertThat(requestedUrls.get(2)).contains(",start:2000&");
  }

  @Test
  void asksForAnExplicitCountSoTeamCityDoesNotCapThePageAt100() {
    fetcherReturningPagesOf(1).fetch(mock(Build.class), BUILD_ID);

    assertThat(requestedUrls)
        .containsExactly(
            "https://teamcity.example.com/httpAuth/app/rest/changes"
                + "?locator=build:(id:42),count:1000,start:0&fields=change(version,comment)");
  }

  @Test
  void stopsAtTheCapAndReportsTheTruncation() {
    final int fullPages = CommitHistoryFetcher.MAX_COMMITS / CommitHistoryFetcher.PAGE_SIZE;
    final int[] pageSizes = new int[fullPages + 1];
    Arrays.fill(pageSizes, CommitHistoryFetcher.PAGE_SIZE);

    final CommitHistory history =
        fetcherReturningPagesOf(pageSizes).fetch(mock(Build.class), BUILD_ID);

    assertThat(history.getCommits()).hasSize(CommitHistoryFetcher.MAX_COMMITS);
    assertThat(history.getIncompleteDataWarning())
        .contains(String.valueOf(CommitHistoryFetcher.MAX_COMMITS));
    assertThat(requestedUrls).hasSize(fullPages + 1);
  }

  @Test
  void doesNotReportTruncationWhenTheCommitCountIsExactlyTheCap() {
    final int fullPages = CommitHistoryFetcher.MAX_COMMITS / CommitHistoryFetcher.PAGE_SIZE;
    final int[] pageSizes = new int[fullPages + 1];
    Arrays.fill(pageSizes, CommitHistoryFetcher.PAGE_SIZE);
    pageSizes[fullPages] = 0;

    final CommitHistory history =
        fetcherReturningPagesOf(pageSizes).fetch(mock(Build.class), BUILD_ID);

    assertThat(history.getCommits()).hasSize(CommitHistoryFetcher.MAX_COMMITS);
    assertThat(history.getIncompleteDataWarning()).isNull();
  }

  @Test
  void keepsThePagesAlreadyReadWhenALaterPageFails() {
    final CommitHistoryFetcher fetcher =
        new CommitHistoryFetcher(
            SERVER_URL,
            logger,
            url -> {
              final int pageIndex = requestedUrls.size();
              requestedUrls.add(url);
              if (pageIndex == 2) {
                throw new IOException("read timed out");
              }
              return changesJson(
                  pageIndex * CommitHistoryFetcher.PAGE_SIZE, CommitHistoryFetcher.PAGE_SIZE);
            });

    final CommitHistory history = fetcher.fetch(buildWithChange("abc123", "a change"), BUILD_ID);

    assertThat(history.getCommits()).hasSize(2 * CommitHistoryFetcher.PAGE_SIZE);
    assertThat(history.getCommits().get(0).Id).isEqualTo("commit-0");
    assertThat(history.getIncompleteDataWarning())
        .contains(String.valueOf(2 * CommitHistoryFetcher.PAGE_SIZE));
    verify(logger).warning(anyString());
  }

  @Test
  void mapsChangeVersionToIdAndChangeCommentToComment() {
    final CommitHistoryFetcher fetcher =
        new CommitHistoryFetcher(
            SERVER_URL,
            logger,
            url -> "{\"count\":1,\"change\":[{\"version\":\"deadbeef\",\"comment\":\"Fix it\"}]}");

    final List<Commit> commits = fetcher.fetch(mock(Build.class), BUILD_ID).getCommits();

    assertThat(commits).hasSize(1);
    assertThat(commits.get(0).Id).isEqualTo("deadbeef");
    assertThat(commits.get(0).Comment).isEqualTo("Fix it");
  }

  @Test
  void fallsBackToTheTeamCityClientWhenTheRequestFails() {
    final CommitHistoryFetcher fetcher =
        new CommitHistoryFetcher(
            SERVER_URL,
            logger,
            url -> {
              throw new IOException("connection refused");
            });

    final CommitHistory history = fetcher.fetch(buildWithChange("abc123", "a change"), BUILD_ID);

    assertThat(history.getCommits()).hasSize(1);
    assertThat(history.getCommits().get(0).Id).isEqualTo("abc123");
    assertThat(history.getCommits().get(0).Comment).isEqualTo("a change");
    assertThat(history.getIncompleteDataWarning()).isNull();
    verify(logger).warning(anyString());
  }

  @Test
  void reportsTruncationWhenTheFallbackHitsTheTeamCityDefaultPageSize() {
    final CommitHistoryFetcher fetcher =
        new CommitHistoryFetcher(
            SERVER_URL,
            logger,
            url -> {
              throw new IOException("connection refused");
            });

    final CommitHistory history =
        fetcher.fetch(buildWithChanges(CommitHistoryFetcher.TEAMCITY_DEFAULT_PAGE_SIZE), BUILD_ID);

    assertThat(history.getCommits()).hasSize(CommitHistoryFetcher.TEAMCITY_DEFAULT_PAGE_SIZE);
    assertThat(history.getIncompleteDataWarning())
        .contains("only the first " + CommitHistoryFetcher.TEAMCITY_DEFAULT_PAGE_SIZE + " commits");
  }

  @Test
  void fallsBackToTheTeamCityClientWhenTheResponseIsNotValidJson() {
    final CommitHistoryFetcher fetcher =
        new CommitHistoryFetcher(SERVER_URL, logger, url -> "<html>not json</html>");

    final CommitHistory history = fetcher.fetch(buildWithChange("abc123", "a change"), BUILD_ID);

    assertThat(history.getCommits()).extracting(commit -> commit.Id).containsExactly("abc123");
    verify(logger).warning(anyString());
  }

  private CommitHistoryFetcher fetcherReturningPagesOf(final int... pageSizes) {
    return new CommitHistoryFetcher(
        SERVER_URL,
        logger,
        url -> {
          final int pageIndex = requestedUrls.size();
          requestedUrls.add(url);
          return changesJson(pageIndex * CommitHistoryFetcher.PAGE_SIZE, pageSizes[pageIndex]);
        });
  }

  private static String changesJson(final int firstCommitIndex, final int count) {
    final StringBuilder json =
        new StringBuilder("{\"count\":").append(count).append(",\"change\":[");
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        json.append(',');
      }
      final int commitIndex = firstCommitIndex + i;
      json.append("{\"version\":\"commit-")
          .append(commitIndex)
          .append("\",\"comment\":\"comment ")
          .append(commitIndex)
          .append("\"}");
    }
    return json.append("]}").toString();
  }

  private static Build buildWithChange(final String version, final String comment) {
    final Change change = mock(Change.class);
    when(change.getVersion()).thenReturn(version);
    when(change.getComment()).thenReturn(comment);
    final Build build = mock(Build.class);
    when(build.fetchChanges()).thenReturn(Collections.singletonList(change));
    return build;
  }

  private static Build buildWithChanges(final int count) {
    final List<Change> changes = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final Change change = mock(Change.class);
      when(change.getVersion()).thenReturn("commit-" + i);
      changes.add(change);
    }
    final Build build = mock(Build.class);
    when(build.fetchChanges()).thenReturn(changes);
    return build;
  }
}
