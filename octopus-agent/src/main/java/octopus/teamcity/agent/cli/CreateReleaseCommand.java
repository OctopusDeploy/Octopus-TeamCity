package octopus.teamcity.agent.cli;

import octopus.teamcity.agent.OctopusCommandBuilder;

import java.util.Map;
import java.util.function.Consumer;

/** Creates the release, and hands what it made back to the step that asked for it. */
final class CreateReleaseCommand extends OctopusCommandBuilder {
  private final Map<String, String> parameters;
  private final Consumer<CreateReleaseResponse> releaseCreated;

  CreateReleaseCommand(
      final Map<String, String> parameters, final Consumer<CreateReleaseResponse> releaseCreated) {
    this.parameters = parameters;
    this.releaseCreated = releaseCreated;
  }

  @Override
  protected String[] buildCommand(final boolean masked) {
    return CommandHelper.createRelease(parameters);
  }

  @Override
  public void readResponse(final String output) {
    releaseCreated.accept(CreateReleaseResponse.of(output));
  }
}
