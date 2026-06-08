package it.unibo.sap.account.domain.events;

import it.unibo.sap.common.ddd.DomainEvent;

import java.time.Instant;

public record LoginFailed(
        String username,
        String reason,
        Instant occurredOn
) implements DomainEvent {
}
