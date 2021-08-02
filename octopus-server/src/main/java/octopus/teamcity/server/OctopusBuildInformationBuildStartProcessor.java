package octopus.teamcity.server;

import java.util.List;

import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.serverSide.BuildStartContext;
import jetbrains.buildServer.serverSide.BuildStartContextProcessor;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.vcs.VcsRootInstanceEntry;
import org.jetbrains.annotations.NotNull;

public class OctopusBuildInformationBuildStartProcessor implements BuildStartContextProcessor {

  private final ExtensionHolder extensionHolder;

  public OctopusBuildInformationBuildStartProcessor(@NotNull final ExtensionHolder extensionHolder, @NotNull final WebLinks webLinks) {
    this.extensionHolder = extensionHolder;
  }

  @Override
  public void updateParameters(@NotNull BuildStartContext buildStartContext) {

    final SRunningBuild build = buildStartContext.getBuild();
    final List<VcsRootInstanceEntry> vcsRoots = build.getVcsRootEntries();

    if (vcsRoots.size() == 0) {
      return;
    }

    boolean buildContainsBuildInformationStep =
        buildStartContext.getRunnerContexts().stream().anyMatch(rc -> rc.getRunType() instanceof OctopusBuildInformationRunType);

    if (buildContainsBuildInformationStep) {
      final VcsRootInstanceEntry vcsRoot = vcsRoots.get(0);
      String vcsType = "Unknown";
      if (vcsRoot.getVcsName().contains("git")) {
        vcsType = "Git";
      }
      buildStartContext.addSharedParameter("octopus_vcstype", vcsType);
    }
  }

  public void register() {
    extensionHolder.registerExtension(BuildStartContextProcessor.class, this.getClass().getName(), this);
  }
}
