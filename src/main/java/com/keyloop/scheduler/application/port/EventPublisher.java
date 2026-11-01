package com.keyloop.scheduler.application.port;

import com.keyloop.scheduler.domain.shared.AggregateRoot;

public interface EventPublisher {
    void publishEventsFor(AggregateRoot<?> aggregateRoot);
}
