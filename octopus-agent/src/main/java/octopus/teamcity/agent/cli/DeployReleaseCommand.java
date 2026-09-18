package octopus.teamcity.agent.cli;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Deploys a release. The version comes from a supplier because a Create release step that also
 * deploys only learns it when the release it just made answers, after this command was built.
 */
final class DeployReleaseCommand extends ServerTaskCommand {
  private final Map<String, String> parameters;
  private final Supplier<String> autoCreatedReleaseNumber;

  DeployReleaseCommand(final Map<String, String> parameters) {
    this(parameters, () -> null);
  }

  DeployReleaseCommand(
      final Map<String, String> parameters, final Supplier<String> autoCreatedReleaseNumber) {
    this.parameters = parameters;
    this.autoCreatedReleaseNumber = autoCreatedReleaseNumber;
  }

  @Override
  protected String[] buildCommand(final boolean masked) {
    return CommandHelper.deployRelease(parameters, autoCreatedReleaseNumber.get());
  }
}
