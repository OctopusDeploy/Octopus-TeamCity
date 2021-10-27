package octopus.teamcity.agent.createrelease;

import static java.util.Collections.singletonList;

import com.octopus.sdk.model.commands.CreateReleaseCommandBody;
import com.octopus.sdk.operation.executionapi.CreateRelease;

import java.util.Collection;

import jetbrains.buildServer.agent.BuildRunnerContext;
import octopus.teamcity.agent.GenericUploadingBuildProcess;
import octopus.teamcity.common.createrelease.CreateReleaseUserData;

public class OctopusCreateReleaseBuildProcess
    extends GenericUploadingBuildProcess<CreateReleaseCommandBody> {

  public OctopusCreateReleaseBuildProcess(
      final BuildRunnerContext context, final CreateRelease executor) {
    super(context, executor);
  }

  @Override
  protected Collection<CreateReleaseCommandBody> collateParameters() {
    final CreateReleaseUserData userData = new CreateReleaseUserData(context.getRunnerParameters());

    final CreateReleaseCommandBody body =
        new CreateReleaseCommandBody(
            userData.getSpaceName(), userData.getProjectName(), userData.getPackageVersion());
    userData.getReleaseVersion().ifPresent(body::setReleaseVersion);
    userData.getChannelName().ifPresent(body::setChannelIdOrName);
    body.setPackages(userData.getPackages());

    return singletonList(body);
  }

  @Override
  protected String getIdentifier(final CreateReleaseCommandBody parameter) {
    return parameter.getReleaseVersion();
  }
}
