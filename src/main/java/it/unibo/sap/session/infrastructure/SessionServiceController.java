package it.unibo.sap.session.infrastructure;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.unibo.sap.common.hexagonal.InputAdapter;
import it.unibo.sap.session.application.SessionService;
import it.unibo.sap.session.domain.Session;
import it.unibo.sap.session.domain.SessionId;

public class SessionServiceController implements InputAdapter {

    private final SessionService sessionService;

    public SessionServiceController(final SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void registerRoutes(final Router router) {
        router.route("/api/v1/*").handler(BodyHandler.create());
        router.post("/api/v1/login").handler(this::handleLogin);
        router.post("/api/v1/user-sessions/:sessionId/create-delivery").handler(this::handleCreateDelivery);
        router.post("/api/v1/user-sessions/:sessionId/cancel-delivery").handler(this::handleCancelDelivery);
        router.post("/api/v1/user-sessions/:sessionId/track-delivery").handler(this::handleTrackDelivery);
        router.get("/api/v1/user-sessions/:sessionId/deliveries/:deliveryId").handler(this::handleGetDelivery);
        router.get("/api/v1/user-sessions/:sessionId/admin/fleet").handler(this::handleViewFleet);
        router.get("/api/v1/user-sessions/:sessionId/admin/scheduling").handler(this::handleViewScheduling);
    }

    private void handleLogin(final RoutingContext ctx) {
        final JsonObject body = ctx.body().asJsonObject();
        final String username = body.getString("username");
        final String password = body.getString("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            ctx.response().setStatusCode(400)
                    .end(new JsonObject().put("error", "Missing username or password").encode());
            return;
        }
        try {
            final Session session = sessionService.login(username, password);
            final JsonObject links = new JsonObject();
            final String base = "/api/v1/user-sessions/" + session.getId().value();
            if ("SENDER".equals(session.getRole())) {
                links.put("createDeliveryLink", base + "/create-delivery");
                links.put("trackDeliveryLink", base + "/track-delivery");
            } else if ("ADMIN".equals(session.getRole())) {
                links.put("fleetLink", base + "/admin/fleet");
                links.put("schedulingLink", base + "/admin/scheduling");
            }
            ctx.response().setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject()
                            .put("sessionId", session.getId().value())
                            .put("accountId", session.getAccountId())
                            .put("role", session.getRole())
                            .put("links", links)
                            .encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(401)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void handleCreateDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        try {
            final JsonObject body = ctx.body().asJsonObject();
            final JsonObject result = sessionService.createDelivery(sessionId, body);
            final int statusCode = result.containsKey("_statusCode") ? result.getInteger("_statusCode") : 201;
            result.remove("_statusCode");
            ctx.response().setStatusCode(statusCode)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (final SecurityException e) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void handleCancelDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        try {
            final JsonObject body = ctx.body().asJsonObject();
            final String deliveryId = body.getString("deliveryId");
            final JsonObject result = sessionService.cancelDelivery(sessionId, deliveryId);
            final int statusCode = result.containsKey("_statusCode") ? result.getInteger("_statusCode") : 200;
            result.remove("_statusCode");
            ctx.response().setStatusCode(statusCode)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (final SecurityException e) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void handleTrackDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        try {
            final JsonObject body = ctx.body().asJsonObject();
            final String deliveryId = body.getString("deliveryId");
            final JsonObject result = sessionService.trackDelivery(sessionId, deliveryId);
            ctx.response().setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.encode());
        } catch (final SecurityException e) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void handleGetDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final String deliveryId = ctx.pathParam("deliveryId");
        try {
            sessionService.getDelivery(sessionId, deliveryId).ifPresentOrElse(
                    delivery -> ctx.response().setStatusCode(200)
                            .putHeader("Content-Type", "application/json")
                            .end(delivery.encode()),
                    () -> ctx.response().setStatusCode(404)
                            .end(new JsonObject().put("error", "Delivery not found").encode())
            );
        } catch (final SecurityException e) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void handleViewFleet(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        try {
            final JsonObject result = sessionService.viewFleet(sessionId);
            ctx.response().setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.getJsonArray("fleet").encode());
        } catch (final SecurityException e) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }

    private void handleViewScheduling(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final String droneId = ctx.queryParams().get("droneId");
        try {
            final JsonObject result = sessionService.viewScheduling(sessionId, droneId);
            ctx.response().setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(result.getJsonArray("scheduling").encode());
        } catch (final SecurityException e) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        } catch (final IllegalArgumentException e) {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", e.getMessage()).encode());
        }
    }
}
