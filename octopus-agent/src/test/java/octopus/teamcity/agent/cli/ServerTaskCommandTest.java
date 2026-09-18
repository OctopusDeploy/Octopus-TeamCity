package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

class ServerTaskCommandTest {

  private final DeployReleaseCommand command = new DeployReleaseCommand(new HashMap<>());

  @Test
  void readsTheTaskItStartedOutOfItsOwnResponse() {
    command.readResponse("[{\"ServerTaskId\": \"task-xyz\", \"DeploymentId\": \"Deployments-1\"}]");

    assertThat(command.requireServerTaskId()).isEqualTo("task-xyz");
  }

  /** There is nothing to wait for, and no way to say what to wait for, so this has to fail. */
  @Test
  void failsWithTheCliResponseWhenTheTaskCannotBeRead() {
    command.readResponse("Error: the deployment was not started");

    assertThatThrownBy(command::requireServerTaskId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("nothing to wait for")
        .hasMessageContaining("Error: the deployment was not started");
  }

  @Test
  void failsWhenItWasNeverGivenAResponseAtAll() {
    assertThatThrownBy(command::requireServerTaskId)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("nothing to wait for");
  }
}
