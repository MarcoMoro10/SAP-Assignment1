package it.unibo.sap.delivery.domain.deliveries;

import it.unibo.sap.common.ddd.ValueObject;

import java.util.Objects;
import java.util.Optional;

public record DeliveryRequest(Package parcel,
                              Location pickupLocation,
                              Location destination,
                              RequestedDateTime requestedDateTime,
                              Deadline deadline) implements ValueObject {

    public DeliveryRequest {
        Objects.requireNonNull(parcel, "DeliveryRequest parcel must not be null");
        Objects.requireNonNull(pickupLocation, "DeliveryRequest pickupLocation must not be null");
        Objects.requireNonNull(destination, "DeliveryRequest destination must not be null");
        Objects.requireNonNull(requestedDateTime, "DeliveryRequest requestedDateTime must not be null");
        // deadline may be null (optional)
    }

    public Optional<Deadline> optionalDeadline() {
        return Optional.ofNullable(deadline);
    }

    public boolean isImmediate() {
        return requestedDateTime.isImmediate();
    }
}
