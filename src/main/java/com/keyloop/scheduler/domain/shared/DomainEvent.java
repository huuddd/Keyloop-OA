package com.keyloop.scheduler.domain.shared;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredOn();
}
