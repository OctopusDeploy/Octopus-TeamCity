package octopus.teamcity.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactHolder;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import octopus.teamcity.common.ReleaseSummary;

/**
 * Reads back the releases a build's steps recorded as hidden artifacts. Shared by everything that
 * shows them, so a page that asks twice (TeamCity checks whether a page applies before rendering
 * it) still only reads the artifacts once.
 */
final class ReleaseSummaries {
  private static final String REQUEST_KEY = "octopus.release.summaries";

  private ReleaseSummaries() {}

  @SuppressWarnings("unchecked")
  static List<ReleaseSummary> forRequest(final HttpServletRequest request, final SBuild build) {
    final Object cached = request.getAttribute(REQUEST_KEY);
    if (cached != null) {
      return (List<ReleaseSummary>) cached;
    }

    final List<ReleaseSummary> releases = of(build);
    request.setAttribute(REQUEST_KEY, releases);
    return releases;
  }

  static List<ReleaseSummary> of(final SBuild build) {
    final List<ReleaseSummary> releases = new ArrayList<>();
    if (build == null) {
      return releases;
    }

    // VIEW_ALL is the only mode that addresses hidden artifacts by the path they were published
    // under; the hidden-only view is rooted inside .teamcity instead.
    final BuildArtifactHolder directory =
        build
            .getArtifacts(BuildArtifactsViewMode.VIEW_ALL)
            .findArtifact(ReleaseSummary.ARTIFACT_DIRECTORY);
    if (!directory.isAvailable() || !directory.isAccessible()) {
      return releases;
    }

    try {
      for (final BuildArtifact summary : directory.getArtifact().getChildren()) {
        if (ReleaseSummary.isSummary(summary.getName())) {
          read(summary).ifPresent(releases::add);
        }
      }
    } catch (final RuntimeException e) {
      Loggers.SERVER.warn(
          "Could not list the Octopus Deploy releases recorded by build " + build.getBuildId(), e);
    }

    return releases;
  }

  private static Optional<ReleaseSummary> read(final BuildArtifact summary) {
    try (InputStream contents = summary.getInputStream()) {
      return ReleaseSummary.readFrom(contents);
    } catch (final IOException e) {
      Loggers.SERVER.warn("Could not read the Octopus Deploy release in " + summary.getName(), e);
      return Optional.empty();
    }
  }
}
