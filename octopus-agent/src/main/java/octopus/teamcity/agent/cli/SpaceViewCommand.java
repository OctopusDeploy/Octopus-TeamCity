package octopus.teamcity.agent.cli;

import java.util.Map;
import java.util.function.Consumer;

import octopus.teamcity.agent.OctopusCommandBuilder;

/**
 * Looks up the space the step is configured against, whose id the portal address of anything the
 * step creates is built from. It is only ever run for that detail, so it is optional - a step that
 * cannot read its space still creates its release.
 */
final class SpaceViewCommand extends OctopusCommandBuilder {
  private final Map<String, String> parameters;
  private final Consumer<String> spaceIdRead;

  SpaceViewCommand(final Map<String, String> parameters, final Consumer<String> spaceIdRead) {
    this.parameters = parameters;
    this.spaceIdRead = spaceIdRead;
  }

  @Override
  public boolean isOptional() {
    return true;
  }

  @Override
  protected String[] buildCommand(final boolean masked) {
    return CommandHelper.spaceView(parameters);
  }

  @Override
  public void readResponse(final String output) {
    // Only an actual space id is worth handing on: an address built out of anything else would
    // point at nothing.
    CommandUtils.getSpaceId(output).filter(CommandUtils::isSpaceId).ifPresent(spaceIdRead);
  }
}
