package octopus.teamcity.e2e.dsl;

import org.jetbrains.teamcity.rest.TeamCityInstance;
import org.testcontainers.containers.GenericContainer;

public class TeamCityContainers {
  private final GenericContainer<?> serverContainer;
  private final GenericContainer<?> agentContainer;

  private final TeamCityInstance restAPi;

  public TeamCityContainers(
      final GenericContainer<?> serverContainer,
      final GenericContainer<?> agentContainer,
      final TeamCityInstance restAPi) {
    this.serverContainer = serverContainer;
    this.agentContainer = agentContainer;
    this.restAPi = restAPi;
  }

  public GenericContainer<?> getServerContainer() {
    return serverContainer;
  }

  public GenericContainer<?> getAgentContainer() {
    return agentContainer;
  }

  public TeamCityInstance getRestAPi() {
    return restAPi;
  }
}
