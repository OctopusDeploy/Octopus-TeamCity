package octopus.teamcity.server;

import java.util.List;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;

public class OctopusBuildInformationBuildStartProcessor implements BuildStartContextProcessor {

  private final Logger logger = Loggers.SERVER;
  private final WebLinks webLinks;

  public OctopusBuildInformationBuildStartProcessor(
      final ExtensionHolder extensionHolder, final WebLinks webLinks) {
    extensionHolder.registerExtension(
        BuildStartContextProcessor.class, this.getClass().getName(), this);
    this.webLinks = webLinks;
  }

  @Override
  public void updateParameters(final BuildStartContext buildStartContext) {
    try {
      boolean buildContainsBuildInformationStep =
          buildStartContext.getRunnerContexts().stream()
              .anyMatch(
                  rc -> rc.getRunType().getType().equals(OctopusConstants.METADATA_RUNNER_TYPE));

      if (buildContainsBuildInformationStep) {
        final SRunningBuild build = buildStartContext.getBuild();
        final List<VcsRootInstanceEntry> vcsRoots = build.getVcsRootEntries();

        if (vcsRoots.size() != 0) {
          final VcsRootInstanceEntry vcsRoot = vcsRoots.get(0);
          String vcsType = "Unknown";
          if (vcsRoot.getVcsName().contains("git")) {
            vcsType = "Git";
          }
          buildStartContext.addSharedParameter("octopus_vcstype", vcsType);
        }
        final String buildUrl = webLinks.getViewLogUrl(buildStartContext.getBuild());
        buildStartContext.addSharedParameter("externalBuildUrl", buildUrl);
      }
    } catch (final Throwable t) {
      logger.error("Failed to write VCS type into the buildstartContext's shared parameters", t);
      throw t;
    }
  }
}
