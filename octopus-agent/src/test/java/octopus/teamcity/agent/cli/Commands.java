package octopus.teamcity.agent.cli;

import java.util.List;

import octopus.teamcity.agent.OctopusCommandBuilder;

/** Finds the command a step decided to run, the way CLIBuildProcess walks the list. */
final class Commands {

  private Commands() {}

  static <T extends OctopusCommandBuilder> T of(
      final List<OctopusCommandBuilder> commands, final Class<T> kind) {
    return commands.stream()
        .filter(kind::isInstance)
        .map(kind::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("The step did not run a " + kind.getSimpleName()));
  }

  static boolean ranA(final List<OctopusCommandBuilder> commands, final Class<?> kind) {
    return commands.stream().anyMatch(kind::isInstance);
  }
}
