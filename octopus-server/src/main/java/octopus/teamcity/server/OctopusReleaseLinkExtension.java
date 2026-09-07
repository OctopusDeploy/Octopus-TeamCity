package octopus.teamcity.server;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.controllers.BuildDataExtensionUtil;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import octopus.teamcity.common.ReleaseSummary;
import org.jetbrains.annotations.NotNull;

/**
 * Puts the releases a build created into the build's summary, from what its steps recorded. Builds
 * that created no release contribute nothing to the page.
 *
 * <p>This is the classic build page's surface: the current UI has no slot for a fragment of the
 * build summary, and shows {@link OctopusReleaseTab} instead.
 */
public class OctopusReleaseLinkExtension extends SimplePageExtension {
  private final SBuildServer server;

  public OctopusReleaseLinkExtension(
      final PagePlaces pagePlaces,
      final PluginDescriptor pluginDescriptor,
      final SBuildServer server) {
    super(
        pagePlaces,
        PlaceId.BUILD_SUMMARY,
        "octopusReleaseLink",
        pluginDescriptor.getPluginResourcesPath("octopusReleaseLink.jsp"));
    this.server = server;
    register();
  }

  @Override
  public boolean isAvailable(@NotNull final HttpServletRequest request) {
    return !releasesOf(request).isEmpty();
  }

  @Override
  public void fillModel(
      @NotNull final Map<String, Object> model, @NotNull final HttpServletRequest request) {
    final List<ReleaseSummary> releases = releasesOf(request);
    if (!releases.isEmpty()) {
      model.put("octopusReleases", releases);
    }
  }

  private List<ReleaseSummary> releasesOf(final HttpServletRequest request) {
    return ReleaseSummaries.forRequest(
        request, BuildDataExtensionUtil.retrieveBuild(request, server));
  }
}
