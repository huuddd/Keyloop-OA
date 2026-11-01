package com.keyloop.scheduler.infrastructure.event;

import com.keyloop.scheduler.application.port.EventPublisher;
import com.keyloop.scheduler.domain.shared.AggregateRoot;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishEventsFor(AggregateRoot<?> aggregateRoot) {
        aggregateRoot.getDomainEvents().forEach(applicationEventPublisher::publishEvent);
        aggregateRoot.clearDomainEvents();
    }
}
