package it.unibo.sap.session.domain.events;

import it.unibo.sap.common.ddd.DomainEvent;
import it.unibo.sap.session.domain.SessionId;

import java.time.Instant;

public record SessionExpired(
        SessionId sessionId,
        Instant occurredOn
) implements DomainEvent {
}
