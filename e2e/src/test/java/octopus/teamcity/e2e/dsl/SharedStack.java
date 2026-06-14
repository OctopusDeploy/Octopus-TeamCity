package octopus.teamcity.e2e.dsl;

/**
 * Provides a single {@link OctopusTeamCityStack} shared across the whole e2e suite so the expensive
 * TeamCity / agent / Octopus / MSSQL containers boot once instead of per test.
 *
 * <p>The stack (the full flavour, with an agent and Octopus) is started lazily on first use and
 * reaped by Testcontainers/Ryuk at JVM exit. It is marked shared, so the {@code try (... =
 * SharedStack.full())} blocks in the tests do not stop it on close.
 *
 * <p>Tests stay isolated by using unique TeamCity resource ids (project / build-type) and unique
 * Octopus project and environment names; the suite runs single-threaded ({@code maxParallelForks =
 * 1}), so there is no concurrent access to the shared server.
 */
public final class SharedStack {

  private static OctopusTeamCityStack full;

  private SharedStack() {}

  /** Returns the suite-wide stack, starting it on first call. */
  public static synchronized OctopusTeamCityStack full() throws Exception {
    if (full == null) {
      final OctopusTeamCityStack started = OctopusTeamCityStack.startWithAgentAndOctopus();
      started.markShared();
      full = started;
    }
    return full;
  }
}
