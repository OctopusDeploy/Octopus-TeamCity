package octopus.teamcity.agent.cli;

import java.util.Map;

/** Runs a runbook. */
final class RunbookRunCommand extends ServerTaskCommand {
  private final Map<String, String> parameters;

  RunbookRunCommand(final Map<String, String> parameters) {
    this.parameters = parameters;
  }

  @Override
  protected String[] buildCommand(final boolean masked) {
    return CommandHelper.runbookRun(parameters);
  }
}
