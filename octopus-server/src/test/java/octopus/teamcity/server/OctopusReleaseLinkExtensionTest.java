package octopus.teamcity.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactHolder;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifacts;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.web.openapi.PagePlace;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import octopus.teamcity.common.ReleaseSummary;
import org.junit.jupiter.api.Test;

class OctopusReleaseLinkExtensionTest {

  private static final long BUILD_ID = 42;
  private static final String RELEASE_URL =
      "https://my.octopus.app/app#/Spaces-162/releases/Releases-14";
  private static final String OTHER_RELEASE_URL =
      "https://my.octopus.app/app#/Spaces-162/releases/Releases-15";

  private final SBuildServer server = mock(SBuildServer.class);
  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final Map<String, Object> requestAttributes = new HashMap<>();

  private OctopusReleaseLinkExtension extension() {
    final PagePlaces pagePlaces = mock(PagePlaces.class);
    when(pagePlaces.getPlaceById(any())).thenReturn(mock(PagePlace.class));
    final PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
    when(pluginDescriptor.getPluginResourcesPath(anyString())).thenReturn("plugins/octopus/x.jsp");

    when(request.getParameter("buildId")).thenReturn(String.valueOf(BUILD_ID));
    when(request.getAttribute(anyString()))
        .thenAnswer(invocation -> requestAttributes.get(invocation.<String>getArgument(0)));
    doAnswer(
            invocation -> {
              requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1));
              return null;
            })
        .when(request)
        .setAttribute(anyString(), any());

    return new OctopusReleaseLinkExtension(pagePlaces, pluginDescriptor, server);
  }

  /** A build whose hidden artifact directory holds the given files. */
  private void buildWithHiddenFiles(final List<BuildArtifact> files) {
    final SBuild build = mock(SBuild.class);
    when(build.getBuildId()).thenReturn(BUILD_ID);
    when(server.findBuildInstanceById(BUILD_ID)).thenReturn(build);

    final BuildArtifact directory = mock(BuildArtifact.class);
    when(directory.getChildren()).thenReturn(files);

    final BuildArtifactHolder holder = mock(BuildArtifactHolder.class);
    when(holder.isAvailable()).thenReturn(!files.isEmpty());
    when(holder.isAccessible()).thenReturn(true);
    when(holder.getArtifact()).thenReturn(directory);

    final BuildArtifacts artifacts = mock(BuildArtifacts.class);
    when(artifacts.findArtifact(ReleaseSummary.ARTIFACT_DIRECTORY)).thenReturn(holder);
    when(build.getArtifacts(BuildArtifactsViewMode.VIEW_ALL)).thenReturn(artifacts);
  }

  private static BuildArtifact summaryOf(
      final String stepId, final String url, final String version) throws Exception {
    return file(
        ReleaseSummary.artifactNameFor(stepId),
        "url=" + url + System.lineSeparator() + "version=" + version);
  }

  private static BuildArtifact file(final String name, final String contents) throws Exception {
    final BuildArtifact artifact = mock(BuildArtifact.class);
    when(artifact.getName()).thenReturn(name);
    when(artifact.getInputStream())
        .thenReturn(new ByteArrayInputStream(contents.getBytes("UTF-8")));
    return artifact;
  }

  @SuppressWarnings("unchecked")
  private static List<ReleaseSummary> releasesIn(final Map<String, Object> model) {
    final Object releases = model.get("octopusReleases");
    return releases == null ? new ArrayList<>() : (List<ReleaseSummary>) releases;
  }

  @Test
  void showsTheReleaseTheBuildCreated() throws Exception {
    buildWithHiddenFiles(Collections.singletonList(summaryOf("RUNNER_1", RELEASE_URL, "1.2.3")));
    final OctopusReleaseLinkExtension extension = extension();

    assertThat(extension.isAvailable(request)).isTrue();

    final Map<String, Object> model = new HashMap<>();
    extension.fillModel(model, request);

    assertThat(releasesIn(model)).hasSize(1);
    assertThat(releasesIn(model).get(0).getUrl()).isEqualTo(RELEASE_URL);
    assertThat(releasesIn(model).get(0).getVersion()).isEqualTo("1.2.3");
  }

  @Test
  void showsEveryReleaseABuildCreated() throws Exception {
    buildWithHiddenFiles(
        Arrays.asList(
            summaryOf("RUNNER_1", RELEASE_URL, "1.2.3"),
            summaryOf("RUNNER_4", OTHER_RELEASE_URL, "4.5.6")));
    final OctopusReleaseLinkExtension extension = extension();

    final Map<String, Object> model = new HashMap<>();
    extension.fillModel(model, request);

    assertThat(releasesIn(model)).hasSize(2);
    assertThat(releasesIn(model).get(1).getUrl()).isEqualTo(OTHER_RELEASE_URL);
  }

  @Test
  void ignoresHiddenFilesThatAreNotReleaseSummaries() throws Exception {
    buildWithHiddenFiles(
        Arrays.asList(
            file("notes.txt", "url=nonsense"), summaryOf("RUNNER_1", RELEASE_URL, "1.2.3")));
    final OctopusReleaseLinkExtension extension = extension();

    final Map<String, Object> model = new HashMap<>();
    extension.fillModel(model, request);

    assertThat(releasesIn(model)).hasSize(1);
    assertThat(releasesIn(model).get(0).getUrl()).isEqualTo(RELEASE_URL);
  }

  @Test
  void readsTheSummariesOncePerRequest() throws Exception {
    buildWithHiddenFiles(Collections.singletonList(summaryOf("RUNNER_1", RELEASE_URL, "1.2.3")));
    final OctopusReleaseLinkExtension extension = extension();

    // The streams are consumed by the first read, so a second one would come back empty.
    extension.isAvailable(request);
    final Map<String, Object> model = new HashMap<>();
    extension.fillModel(model, request);

    assertThat(releasesIn(model)).hasSize(1);
  }

  @Test
  void addsNothingToABuildThatCreatedNoRelease() {
    buildWithHiddenFiles(Collections.emptyList());
    final OctopusReleaseLinkExtension extension = extension();

    assertThat(extension.isAvailable(request)).isFalse();

    final Map<String, Object> model = new HashMap<>();
    extension.fillModel(model, request);
    assertThat(model).isEmpty();
  }

  @Test
  void addsNothingWhenThereIsNoBuildToShow() {
    final OctopusReleaseLinkExtension extension = extension();

    assertThat(extension.isAvailable(request)).isFalse();
  }
}
