package it.unibo.sap.delivery.domain.fleet.events;

import it.unibo.sap.common.ddd.DomainEvent;
import it.unibo.sap.delivery.domain.fleet.DroneId;

import java.time.Instant;
import it.unibo.sap.delivery.domain.fleet.DroneStatus;

public record StatusUpdated(DroneId droneId, DroneStatus status, Instant occurredOn) implements DomainEvent {
}
