package octopus.teamcity.agent.cli;

import octopus.teamcity.agent.OctopusCommandBuilder;

/**
 * A command that starts work on the server and answers with the task doing it. The id of that task
 * is the only thing a following {@code task wait} can be pointed at, so a step that waits cannot
 * carry on without it.
 */
abstract class ServerTaskCommand extends OctopusCommandBuilder {
  private String serverTaskId;
  private String output;

  @Override
  public void readResponse(final String output) {
    this.output = output;
    this.serverTaskId = CommandUtils.getServerTaskId(output).orElse(null);
  }

  /** The task this command started, or a failure that says what the CLI answered instead. */
  String requireServerTaskId() {
    if (serverTaskId == null) {
      throw new IllegalStateException(
          "Could not read the id of the task to wait for from the CLI's response, so there is "
              + "nothing to wait for. The response was: "
              + CommandUtils.loggableOutput(output));
    }
    return serverTaskId;
  }
}
