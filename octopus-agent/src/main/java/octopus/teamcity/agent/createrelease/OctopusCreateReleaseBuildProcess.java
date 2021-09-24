package octopus.teamcity.agent.createrelease;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.InterruptableBuildProcess;

public class OctopusCreateReleaseBuildProcess extends InterruptableBuildProcess {

  private final BuildProgressLogger buildLogger;

  public OctopusCreateReleaseBuildProcess(BuildRunnerContext context) {
    super(context);
    this.buildLogger = context.getBuild().getBuildLogger();
  }

  @Override
  public void doStart() throws RunBuildException {
    try {
      buildLogger.message("Create release step not implemented");
      complete(BuildFinishedStatus.FINISHED_FAILED);
    } catch (final Throwable ex) {
      throw new RunBuildException("Error processing build information build step.", ex);
    }
  }
}
