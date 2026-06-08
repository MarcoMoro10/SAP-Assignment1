package it.unibo.sap.session.application;

import io.vertx.core.json.JsonObject;
import it.unibo.sap.common.hexagonal.OutputPort;

import java.util.Optional;

public interface DeliveryService extends OutputPort {

    JsonObject createDelivery(JsonObject request);

    JsonObject cancelDelivery(String deliveryId);

    Optional<JsonObject> getDelivery(String deliveryId);

    JsonObject trackDelivery(String deliveryId);

    JsonObject viewFleet();

    JsonObject viewScheduling(String droneId);
}
