package octopus.teamcity.server;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import org.jetbrains.annotations.NotNull;

/**
 * An "Octopus Deploy" tab on a build that created releases, linking to each of them. The tab is the
 * surface the current TeamCity UI renders for a plugin - it hosts one in its classic-UI adapter,
 * whereas the build summary {@link OctopusReleaseLinkExtension} extends is classic-only. Builds
 * that created no release get no tab.
 */
public class OctopusReleaseTab extends ViewLogTab {

  public OctopusReleaseTab(
      final PagePlaces pagePlaces,
      final PluginDescriptor pluginDescriptor,
      final SBuildServer server) {
    super("Octopus Deploy", "octopusRelease", pagePlaces, server);
    setIncludeUrl(pluginDescriptor.getPluginResourcesPath("octopusReleaseLink.jsp"));
    register();
  }

  @Override
  protected boolean isAvailable(
      @NotNull final HttpServletRequest request, @NotNull final SBuild build) {
    return super.isAvailable(request, build) && !ReleaseSummaries.of(build).isEmpty();
  }

  @Override
  protected void fillModel(
      @NotNull final Map<String, Object> model,
      @NotNull final HttpServletRequest request,
      @NotNull final SBuild build) {
    model.put("octopusReleases", ReleaseSummaries.forRequest(request, build));
  }
}
