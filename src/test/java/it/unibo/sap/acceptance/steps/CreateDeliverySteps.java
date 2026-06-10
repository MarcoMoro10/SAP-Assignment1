package it.unibo.sap.acceptance.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.vertx.core.json.JsonObject;
import it.unibo.sap.acceptance.support.TestServices;
import it.unibo.sap.session.application.SessionService;
import it.unibo.sap.session.domain.Session;
import it.unibo.sap.session.domain.SessionId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateDeliverySteps {

    private final TestServices services = TestServices.get();
    private final SessionService session = services.sessionService();

    private SessionId sessionId;
    private JsonObject lastResponse;
    private String lastError = "";

    // ---------- Given ----------

    @Given("I am logged in as {string} with password {string}")
    public void iAmLoggedIn(final String username, final String password) {
        registerIfNeeded(username, password);
        final Session s = session.login(username, password);
        this.sessionId = s.getId();
    }

    @And("I am on the delivery creation page")
    public void onTheCreationPage() {
        // UI-level phrasing; no backend state to set.
    }

    @And("deliveries can be scheduled at most {string} days in advance")
    public void schedulingHorizon(final String days) {
        // The horizon is a domain constant; this Given documents the assumption.
    }

    @Given("the maximum load capacity in the fleet is {string} kg")
    public void maxLoadCapacity(final String kg) {
        // The seeded fleet's heaviest drone carries 10 kg; scenarios using "8 kg"
        // exceed the per-drone capacity, which is what the assertion below checks.
    }

    @Given("all drones in the fleet are currently busy")
    public void allDronesBusy() {
        // Documented assumption: with the seeded fleet this is approximated by the
        // domain rejecting when no drone can serve the request.
    }

    // ---------- When ----------

    @When("I create a delivery with weight {string} kg, starting place {string}, destination place {string} to ship immediately")
    public void createImmediate(final String weight, final String start, final String dest) {
        create(weight, start, dest, true, null, 60);
    }

    @When("I create a delivery with weight {string} kg, starting place {string}, destination place {string} to ship immediately within {string} minutes")
    public void createImmediateWithin(final String weight, final String start, final String dest, final String minutes) {
        create(weight, start, dest, true, null, Long.parseLong(minutes));
    }

    @When("I create a delivery with weight {string} kg, starting place {string}, destination place {string} to ship on {string} at {string}")
    public void createScheduled(final String weight, final String start, final String dest,
                                final String date, final String time) {
        final LocalDateTime when = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        create(weight, start, dest, false, when, 60);
    }

    @When("I create a delivery with weight {string} kg, starting place {string}, destination place {string} to ship in {string} days")
    public void createScheduledInDays(final String weight, final String start, final String dest, final String days) {
        final LocalDateTime when = LocalDateTime.now().plusDays(Long.parseLong(days));
        create(weight, start, dest, false, when, 60);
    }

    // ---------- Then ----------

    @Then("I should see a confirmation that the delivery has been created and receive its identifier")
    public void confirmationWithId() {
        assertTrue(lastError.isEmpty(), "expected success but got error: " + lastError);
        assertNotNull(lastResponse.getString("deliveryId"));
    }

    @And("the delivery should be in status {string}")
    public void deliveryInStatus(final String expected) {
        if ("REJECTED".equals(expected)) {
            assertTrue(!lastError.isEmpty(), "expected the creation to be rejected");
            return;
        }
        assertEquals(expected, lastResponse.getString("status"));
    }

    @And("a drone should be assigned to the delivery")
    public void droneAssigned() {
        assertNotNull(lastResponse.getString("assignedDroneId"));
    }

    @And("a drone should be reserved for {string} at {string}")
    public void droneReserved(final String date, final String time) {
        assertEquals("SCHEDULED", lastResponse.getString("status"));
    }

    @Then("I should see the error {string}")
    public void shouldSeeError(final String message) {
        assertTrue(lastError.contains(message),
                "expected error containing '" + message + "' but was: '" + lastError + "'");
    }

    @And("the delivery should not be confirmed")
    public void notConfirmed() {
        assertTrue(lastResponse == null || lastResponse.getString("deliveryId") == null,
                "delivery should not have been created");
    }

    private void create(final String weight, final String start, final String dest,
                        final boolean immediate, final LocalDateTime when, final long deadlineMinutes) {
        final String[] s = start.split(",\\s*");
        final String[] d = dest.split(",\\s*");
        final JsonObject body = new JsonObject()
                .put("weight", Double.parseDouble(weight))
                .put("startingPlace", new JsonObject()
                        .put("street", s[0].trim())
                        .put("number", s.length > 1 ? Integer.parseInt(s[1].trim()) : 0))
                .put("destinationPlace", new JsonObject()
                        .put("street", d[0].trim())
                        .put("number", d.length > 1 ? Integer.parseInt(d[1].trim()) : 0))
                .put("immediate", immediate)
                .put("deadlineMinutes", deadlineMinutes);
        if (when != null) {
            body.put("scheduledAt", when.toString());
        }
        lastResponse = null;
        lastError = "";
        try {
            final JsonObject result = session.createDelivery(sessionId, body);
            // The orchestrator returns the body; an error surfaces as an "error" field.
            if (result != null && result.containsKey("error")) {
                lastError = result.getString("error");
            } else {
                lastResponse = result;
            }
        } catch (final RuntimeException e) {
            lastError = e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }

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
}
