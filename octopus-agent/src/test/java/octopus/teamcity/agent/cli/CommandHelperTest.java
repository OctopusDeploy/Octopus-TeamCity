package octopus.teamcity.agent.cli;

import static org.assertj.core.api.Assertions.assertThat;

import octopus.teamcity.common.OctopusConstants;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class CommandHelperTest {

  @Test
  void loginCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;
    params.put(constants.getServerKey(), "https://octo.example");
    params.put(constants.getApiKey(), "API-KEY-123");

    String[] command = CommandHelper.login(params).buildCommand();

    assertThat(command).contains(
        "login",
        "--server",
        "https://octo.example",
        "--api-key",
        "API-KEY-123",
        "--no-prompt"
    );
  }

  @Test
  void deployCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;

    params.put(constants.getProjectNameKey(), "MyProject");
    params.put(constants.getReleaseNumberKey(), "1.2.3");
    params.put(constants.getDeployToKey(), "Env1,Env2");
    params.put(constants.getTenantsKey(), "TenantA");
    params.put(constants.getTenantTagsKey(), "TagX,TagY");
    params.put(constants.getCommandLineArgumentsKey(), "arg1 arg2");

    String[] command = CommandHelper.deploy(params, null);

    assertThat(command).contains(
        "release",
        "deploy",
        "--project",
        "MyProject",
        "--version",
        "1.2.3",
        "--environment",
        "Env1",
        "--environment",
        "Env2",
        "--tenant",
        "TenantA",
        "--tenant-tag",
        "TagX",
        "--tenant-tag",
        "TagY",
        "--output-format",
        "json",
        "arg1",
        "arg2",
        "--no-prompt"
    );
  }

  @Test
  void  waitCommand() {
    Map<String, String> params = new HashMap<>();
    final OctopusConstants constants = OctopusConstants.Instance;

    params.put(constants.getDeploymentTimeout(), "600");
    params.put(constants.getCancelDeploymentOnTimeout(), "true");
    params.put(constants.getSpaceName(), "MySpace");

    String[] command = CommandHelper.wait(params, "task-42");

    assertThat(command).containsExactly(
        "task",
        "wait",
        "task-42",
        "--space",
        "MySpace",
        "--progress",
        "--timeout",
        "600",
        "--cancel-on-timeout",
        "--output-format",
        "json",
        "--no-prompt"
    );
  }
}
