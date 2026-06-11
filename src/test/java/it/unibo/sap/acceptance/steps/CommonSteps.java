package it.unibo.sap.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.vertx.core.json.JsonObject;
import it.unibo.sap.acceptance.support.FleetTestFixture;
import it.unibo.sap.acceptance.support.TestServices;
import it.unibo.sap.acceptance.support.World;
import it.unibo.sap.delivery.domain.fleet.DroneStatus;
import it.unibo.sap.session.application.SessionService;
import it.unibo.sap.session.domain.Session;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonSteps {

    private final TestServices services = TestServices.get();
    private final SessionService session = services.sessionService();
    private final World world = World.get();
    private final FleetTestFixture fleet = new FleetTestFixture(services.droneRepository());

    // ---------- authentication ----------

    @Given("I am logged in as {string} with password {string}")
    public void iAmLoggedIn(final String username, final String password) {
        registerIfNeeded(username, password);
        final Session s = session.login(username, password);
        world.setSessionId(s.getId());
        world.setRole(s.getRole());
    }

    @Given("I am logged in as admin {string} with password {string}")
    public void iAmLoggedInAsAdmin(final String username, final String password) {
        // The admin (admin-1 / Admin#123) is seeded by TestServices; just log in.
        final Session s = session.login(username, password);
        world.setSessionId(s.getId());
        world.setRole(s.getRole());
    }

    // ---------- generic error assertion (reused by create/cancel/track/etc.) ----------

    @Then("I should see the error {string}")
    public void shouldSeeError(final String message) {
        assertTrue(world.lastError().contains(message),
                "expected error containing '" + message + "' but was: '" + world.lastError() + "'");
    }

    // Shared across the scheduling and telemetry features.
    @io.cucumber.java.en.And("drone {string} should be in status {string}")
    public void droneShouldBeInStatus(final String droneId, final String status) {
        assertEquals(DroneStatus.valueOf(status), fleet.statusOf(droneId));
    }

    // ---------- helpers ----------

    private void registerIfNeeded(final String username, final String password) {
        final CompletableFuture<Void> done = new CompletableFuture<>();
        services.webClient()
                .post(services.accountPort(), services.host(), "/api/v1/accounts")
                .sendJsonObject(new JsonObject().put("username", username).put("password", password),
                        ar -> done.complete(null)); // 201 created or 409 already exists: both fine
        try {
            done.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to register test user", e);
        }
    }

    /** Used by other step classes to ensure an authenticated sender outside a Background. */
    public static void ensureSenderSession(final String username, final String password) {
        final TestServices services = TestServices.get();
        final CompletableFuture<Void> done = new CompletableFuture<>();
        services.webClient()
                .post(services.accountPort(), services.host(), "/api/v1/accounts")
                .sendJsonObject(new JsonObject().put("username", username).put("password", password),
                        ar -> done.complete(null));
        try {
            done.get(10, TimeUnit.SECONDS);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to register test user", e);
        }
        final Session s = services.sessionService().login(username, password);
        World.get().setSessionId(s.getId());
        World.get().setRole(s.getRole());
        assertNotNull(World.get().sessionId());
    }
}
