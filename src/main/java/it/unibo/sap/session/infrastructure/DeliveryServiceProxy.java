package it.unibo.sap.session.infrastructure;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import it.unibo.sap.common.hexagonal.OutputAdapter;
import it.unibo.sap.session.application.DeliveryService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DeliveryServiceProxy implements DeliveryService, OutputAdapter {

    private final WebClient webClient;
    private final String host;
    private final int port;

    public DeliveryServiceProxy(final WebClient webClient, final String host, final int port) {
        this.webClient = webClient;
        this.host = host;
        this.port = port;
    }

    @Override
    public JsonObject createDelivery(final JsonObject request) {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        webClient.post(port, host, "/api/v1/deliveries")
                .sendJsonObject(request, ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result().bodyAsJsonObject()
                                .put("_statusCode", ar.result().statusCode()));
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            return future.get();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to contact delivery-service", e);
        }
    }

    @Override
    public JsonObject cancelDelivery(final String deliveryId) {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        webClient.post(port, host, "/api/v1/deliveries/" + deliveryId + "/cancel")
                .send(ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result().bodyAsJsonObject()
                                .put("_statusCode", ar.result().statusCode()));
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            return future.get();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to contact delivery-service", e);
        }
    }

    @Override
    public Optional<JsonObject> getDelivery(final String deliveryId) {
        final CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();
        webClient.get(port, host, "/api/v1/deliveries/" + deliveryId)
                .send(ar -> {
                    if (ar.succeeded() && ar.result().statusCode() == 200) {
                        future.complete(Optional.of(ar.result().bodyAsJsonObject()));
                    } else {
                        future.complete(Optional.empty());
                    }
                });
        try {
            return future.get();
        } catch (final Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public JsonObject trackDelivery(final String deliveryId) {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        webClient.post(port, host, "/api/v1/deliveries/" + deliveryId + "/track")
                .send(ar -> {
                    if (ar.succeeded()) {
                        future.complete(ar.result().bodyAsJsonObject());
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            return future.get();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to contact delivery-service", e);
        }
    }

    @Override
    public JsonObject viewFleet() {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        webClient.get(port, host, "/api/v1/fleet")
                .send(ar -> {
                    if (ar.succeeded()) {
                        future.complete(new JsonObject().put("fleet", ar.result().bodyAsJsonArray()));
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            return future.get();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to contact delivery-service", e);
        }
    }

    @Override
    public JsonObject viewScheduling(final String droneId) {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        String path = "/api/v1/scheduling";
        if (droneId != null && !droneId.isBlank()) {
            path += "?droneId=" + droneId;
        }
        webClient.get(port, host, path)
                .send(ar -> {
                    if (ar.succeeded()) {
                        future.complete(new JsonObject().put("scheduling", ar.result().bodyAsJsonArray()));
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });
        try {
            return future.get();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to contact delivery-service", e);
        }
    }
}
