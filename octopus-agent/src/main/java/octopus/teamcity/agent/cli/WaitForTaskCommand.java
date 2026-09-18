package octopus.teamcity.agent.cli;

import java.util.Map;
import java.util.function.Supplier;

import octopus.teamcity.agent.OctopusCommandBuilder;

/**
 * Waits for the task a previous command started. The id is asked for when this command is built
 * into its arguments, which is the first moment the command before it can have answered.
 */
final class WaitForTaskCommand extends OctopusCommandBuilder {
  private final Map<String, String> parameters;
  private final Supplier<String> serverTaskId;

  WaitForTaskCommand(final Map<String, String> parameters, final Supplier<String> serverTaskId) {
    this.parameters = parameters;
    this.serverTaskId = serverTaskId;
  }

  @Override
  protected String[] buildCommand(final boolean masked) {
    return CommandHelper.wait(parameters, serverTaskId.get());
  }
}
