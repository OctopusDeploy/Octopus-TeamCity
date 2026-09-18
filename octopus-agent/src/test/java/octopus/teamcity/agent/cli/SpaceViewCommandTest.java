package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

class SpaceViewCommandTest {

  private final AtomicReference<String> spaceId = new AtomicReference<>();
  private final SpaceViewCommand command = new SpaceViewCommand(new HashMap<>(), spaceId::set);

  @Test
  void isOptionalBecauseAStepThatCannotReadItsSpaceStillCreatesItsRelease() {
    assertThat(command.isOptional()).isTrue();
  }

  @Test
  void asksForTheConfiguredSpaceAsJson() {
    final Map<String, String> params = new HashMap<>();
    params.put(OctopusConstants.Instance.getSpaceName(), "Build Platform");

    assertThat(new SpaceViewCommand(params, spaceId::set).buildCommand())
        .containsExactly(
            "space", "view", "Build Platform", "--output-format", "json", "--no-prompt");
  }

  @Test
  void readsTheSpaceIdOutOfItsOwnResponse() {
    command.readResponse(
        "{\"Id\": \"Spaces-162\", \"Name\": \"Build Platform\", \"TaskQueue\": \"Running\"}");

    assertThat(spaceId.get()).isEqualTo("Spaces-162");
  }

  @Test
  void hasNoSpaceToReportWhenItsResponseCannotBeRead() {
    command.readResponse("Error: space 'Build Platform' not found");
    assertThat(spaceId.get()).isNull();
  }

  /** An address built out of something that is not a space id would point at nothing. */
  @Test
  void hasNoSpaceToReportWhenTheIdIsNotASpaceId() {
    command.readResponse("{\"Id\": \"Environments-1\", \"TaskQueue\": \"Running\"}");
    assertThat(spaceId.get()).isNull();
  }
}
