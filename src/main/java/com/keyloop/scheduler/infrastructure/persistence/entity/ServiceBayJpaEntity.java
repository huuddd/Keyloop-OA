package com.keyloop.scheduler.infrastructure.persistence.entity;

import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBay;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_bays")
public class ServiceBayJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dealership_id", nullable = false)
    private Long dealershipId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ServiceBayJpaEntity() {}

    public static ServiceBayJpaEntity fromDomain(ServiceBay domain) {
        var entity = new ServiceBayJpaEntity();
        entity.id = domain.getId().value();
        entity.dealershipId = domain.getDealershipId().value();
        entity.name = domain.getName();
        entity.isActive = domain.isActive();
        return entity;
    }

    public ServiceBay toDomain() {
        return new ServiceBay(
            ServiceBayId.of(id),
            DealershipId.of(dealershipId),
            name, isActive);
    }

    // Getters
    public Long getId() { return id; }
    public Long getDealershipId() { return dealershipId; }
    public String getName() { return name; }
    public boolean isActive() { return isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
