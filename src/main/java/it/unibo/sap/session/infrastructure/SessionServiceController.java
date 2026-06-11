package it.unibo.sap.session.infrastructure;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import it.unibo.sap.common.hexagonal.InputAdapter;
import it.unibo.sap.session.application.SessionService;
import it.unibo.sap.session.domain.Session;
import it.unibo.sap.session.domain.SessionId;

public class SessionServiceController extends AbstractVerticle implements InputAdapter {

    private final SessionService sessionService;
    private final int port;

    public SessionServiceController(final SessionService sessionService, final int port) {
        this.sessionService = sessionService;
        this.port = port;
    }

    @Override
    public void start(final Promise<Void> startPromise) {
        final Router router = Router.router(vertx);
        router.route("/api/v1/*").handler(BodyHandler.create());
        router.post("/api/v1/login").handler(this::handleLogin);
        router.post("/api/v1/user-sessions/:sessionId/create-delivery").handler(this::handleCreateDelivery);
        router.post("/api/v1/user-sessions/:sessionId/cancel-delivery").handler(this::handleCancelDelivery);
        router.post("/api/v1/user-sessions/:sessionId/track-delivery").handler(this::handleTrackDelivery);
        router.get("/api/v1/user-sessions/:sessionId/deliveries/:deliveryId").handler(this::handleGetDelivery);
        router.get("/api/v1/user-sessions/:sessionId/admin/fleet").handler(this::handleViewFleet);
        router.get("/api/v1/user-sessions/:sessionId/admin/scheduling").handler(this::handleViewScheduling);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, http -> {
                    if (http.succeeded()) {
                        System.out.println("session-service ready - port: " + port);
                        startPromise.complete();
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private void handleLogin(final RoutingContext ctx) {
        final JsonObject body = ctx.body().asJsonObject();
        final String username = body == null ? null : body.getString("username");
        final String password = body == null ? null : body.getString("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            ctx.response().setStatusCode(400)
                    .end(new JsonObject().put("error", "Missing username or password").encode());
            return;
        }
        vertx.executeBlocking(() -> {
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
            return new JsonObject()
                    .put("sessionId", session.getId().value())
                    .put("accountId", session.getAccountId())
                    .put("role", session.getRole())
                    .put("links", links);
        }, false).onComplete(ar -> {
            if (ar.succeeded()) {
                ctx.response().setStatusCode(200)
                        .putHeader("Content-Type", "application/json")
                        .end(ar.result().encode());
            } else {
                ctx.response().setStatusCode(401)
                        .end(new JsonObject().put("error", causeMessage(ar.cause())).encode());
            }
        });
    }

    private void handleCreateDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final JsonObject body = ctx.body().asJsonObject();
        vertx.executeBlocking(() -> sessionService.createDelivery(sessionId, body), false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        final JsonObject result = ar.result();
                        final int statusCode = result.containsKey("_statusCode")
                                ? result.getInteger("_statusCode") : 201;
                        result.remove("_statusCode");
                        ctx.response().setStatusCode(statusCode)
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    } else {
                        respondError(ctx, ar.cause());
                    }
                });
    }

    private void handleCancelDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final JsonObject body = ctx.body().asJsonObject();
        final String deliveryId = body == null ? null : body.getString("deliveryId");
        vertx.executeBlocking(() -> sessionService.cancelDelivery(sessionId, deliveryId), false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        final JsonObject result = ar.result();
                        final int statusCode = result.containsKey("_statusCode")
                                ? result.getInteger("_statusCode") : 200;
                        result.remove("_statusCode");
                        ctx.response().setStatusCode(statusCode)
                                .putHeader("Content-Type", "application/json")
                                .end(result.encode());
                    } else {
                        respondError(ctx, ar.cause());
                    }
                });
    }

    private void handleTrackDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final JsonObject body = ctx.body().asJsonObject();
        final String deliveryId = body == null ? null : body.getString("deliveryId");
        vertx.executeBlocking(() -> sessionService.trackDelivery(sessionId, deliveryId), false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        ctx.response().setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(ar.result().encode());
                    } else {
                        respondError(ctx, ar.cause());
                    }
                });
    }

    private void handleGetDelivery(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final String deliveryId = ctx.pathParam("deliveryId");
        vertx.executeBlocking(() -> sessionService.getDelivery(sessionId, deliveryId), false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        ar.result().ifPresentOrElse(
                                delivery -> ctx.response().setStatusCode(200)
                                        .putHeader("Content-Type", "application/json")
                                        .end(delivery.encode()),
                                () -> ctx.response().setStatusCode(404)
                                        .end(new JsonObject().put("error", "Delivery not found").encode())
                        );
                    } else {
                        respondError(ctx, ar.cause());
                    }
                });
    }

    private void handleViewFleet(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        vertx.executeBlocking(() -> sessionService.viewFleet(sessionId), false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        ctx.response().setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(ar.result().getJsonArray("fleet").encode());
                    } else {
                        respondError(ctx, ar.cause());
                    }
                });
    }

    private void handleViewScheduling(final RoutingContext ctx) {
        final SessionId sessionId = SessionId.of(ctx.pathParam("sessionId"));
        final String droneId = ctx.queryParams().get("droneId");
        vertx.executeBlocking(() -> sessionService.viewScheduling(sessionId, droneId), false)
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        ctx.response().setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(ar.result().getJsonArray("scheduling").encode());
                    } else {
                        respondError(ctx, ar.cause());
                    }
                });
    }

    private void respondError(final RoutingContext ctx, final Throwable cause) {
        if (cause instanceof SecurityException) {
            ctx.response().setStatusCode(403)
                    .end(new JsonObject().put("error", cause.getMessage()).encode());
        } else {
            ctx.response().setStatusCode(404)
                    .end(new JsonObject().put("error", causeMessage(cause)).encode());
        }
    }

    private static String causeMessage(final Throwable t) {
        return t == null || t.getMessage() == null ? "Error" : t.getMessage();
    }
}
