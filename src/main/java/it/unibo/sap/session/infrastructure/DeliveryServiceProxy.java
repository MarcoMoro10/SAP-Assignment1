package it.unibo.sap.session.infrastructure;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import it.unibo.sap.common.hexagonal.OutputAdapter;
import it.unibo.sap.session.application.DeliveryService;
import it.unibo.sap.session.application.UpstreamServiceException;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class DeliveryServiceProxy implements DeliveryService, OutputAdapter {

    private final WebClient webClient;
    private final String host;
    private final int port;
    private final int fleetPort;

    public DeliveryServiceProxy(final WebClient webClient, final String host,
                                final int port, final int fleetPort) {
        this.webClient = webClient;
        this.host = host;
        this.port = port;
        this.fleetPort = fleetPort;
    }

    @Override
    public JsonObject createDelivery(final JsonObject request) {
        return blocking(
                webClient.post(port, host, "/api/v1/deliveries"),
                request,
                resp -> resp.bodyAsJsonObject().put("_statusCode", resp.statusCode()));
    }

    @Override
    public JsonObject cancelDelivery(final String deliveryId, final String senderId) {
        return blocking(
                webClient.post(port, host, "/api/v1/deliveries/" + deliveryId + "/cancel"),
                new JsonObject().put("senderId", senderId),
                resp -> resp.bodyAsJsonObject().put("_statusCode", resp.statusCode()));
    }

    @Override
    public Optional<JsonObject> getDelivery(final String deliveryId, final String senderId) {
        final CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();
        webClient.get(port, host, "/api/v1/deliveries/" + deliveryId)
                .addQueryParam("senderId", senderId)
                .send(ar -> {
                    if (ar.failed()) {
                        future.completeExceptionally(new UpstreamServiceException(
                                "delivery-service is unreachable", ar.cause()));
                        return;
                    }
                    final int status = ar.result().statusCode();
                    if (status == 200) {
                        future.complete(Optional.of(ar.result().bodyAsJsonObject()));
                    } else if (status == 404) {
                        future.complete(Optional.empty());
                    } else {
                        future.completeExceptionally(new UpstreamServiceException(
                                "delivery-service returned unexpected status " + status));
                    }
                });
        try {
            return future.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Interrupted while contacting delivery-service", e);
        } catch (final Exception e) {
            throw new UpstreamServiceException("Failed to contact delivery-service", e);
        }
    }

    @Override
    public JsonObject trackDelivery(final String deliveryId, final String senderId) {
        return blocking(
                webClient.post(port, host, "/api/v1/deliveries/" + deliveryId + "/track"),
                new JsonObject().put("senderId", senderId),
                resp -> resp.bodyAsJsonObject().put("_statusCode", resp.statusCode()));
    }

    @Override
    public JsonObject viewFleet() {
        return blocking(
                webClient.get(fleetPort, host, "/api/v1/admin/fleet"),
                resp -> new JsonObject().put("fleet", resp.bodyAsJsonArray()));
    }

    @Override
    public JsonObject viewScheduling(final String droneId) {
        String path = "/api/v1/admin/scheduling";
        if (droneId != null && !droneId.isBlank()) {
            path += "?droneId=" + droneId;
        }
        return blocking(
                webClient.get(fleetPort, host, path),
                resp -> new JsonObject().put("scheduling", resp.bodyAsJsonArray()));
    }

    private JsonObject blocking(final HttpRequest<io.vertx.core.buffer.Buffer> request,
                                final Function<HttpResponse<io.vertx.core.buffer.Buffer>, JsonObject> onSuccess) {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        request.send(ar -> {
            if (ar.succeeded()) {
                future.complete(onSuccess.apply(ar.result()));
            } else {
                future.completeExceptionally(ar.cause());
            }
        });
        return await(future);
    }

    private JsonObject blocking(final HttpRequest<io.vertx.core.buffer.Buffer> request,
                                final JsonObject body,
                                final Function<HttpResponse<io.vertx.core.buffer.Buffer>, JsonObject> onSuccess) {
        final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        request.sendJsonObject(body, ar -> {
            if (ar.succeeded()) {
                future.complete(onSuccess.apply(ar.result()));
            } else {
                future.completeExceptionally(ar.cause());
            }
        });
        return await(future);
    }

    private JsonObject await(final CompletableFuture<JsonObject> future) {
        try {
            return future.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpstreamServiceException("Interrupted while contacting delivery-service", e);
        } catch (final Exception e) {
            throw new UpstreamServiceException("Failed to contact delivery-service", e);
        }
    }
}