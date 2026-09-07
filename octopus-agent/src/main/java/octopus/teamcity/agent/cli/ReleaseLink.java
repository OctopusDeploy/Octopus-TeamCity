package octopus.teamcity.agent.cli;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * The address of a release in the Octopus Deploy portal, in the form Octopus itself hands out as a
 * release's {@code Links.Web} (and the CLI prints whenever it is not writing JSON): {@code
 * <server>/app#/<spaceId>/releases/<releaseId>}.
 */
final class ReleaseLink {

  private ReleaseLink() {}

  static Optional<String> of(final String serverUrl, final String spaceId, final String releaseId) {
    if (StringUtils.isAnyBlank(serverUrl, spaceId, releaseId)) {
      return Optional.empty();
    }

    return Optional.of(
        StringUtils.stripEnd(serverUrl.trim(), "/")
            + "/app#/"
            + spaceId
            + "/releases/"
            + releaseId);
  }
}
