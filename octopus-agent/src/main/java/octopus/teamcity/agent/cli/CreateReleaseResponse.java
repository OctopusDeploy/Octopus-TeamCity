package octopus.teamcity.agent.cli;

import java.util.Optional;

/**
 * What {@code release create} answered: the release it made, as far as its response can be read.
 * The raw response comes along for the ride, because a value that could not be read is only worth
 * reporting alongside what the CLI said instead.
 */
final class CreateReleaseResponse {
  private final String output;
  private final String id;
  private final String version;

  private CreateReleaseResponse(final String output, final String id, final String version) {
    this.output = output;
    this.id = id;
    this.version = version;
  }

  static CreateReleaseResponse of(final String output) {
    return new CreateReleaseResponse(
        output,
        CommandUtils.getReleaseId(output).orElse(null),
        CommandUtils.getReleaseVersion(output).orElse(null));
  }

  Optional<String> id() {
    return Optional.ofNullable(id);
  }

  Optional<String> version() {
    return Optional.ofNullable(version);
  }

  String output() {
    return output;
  }
}
