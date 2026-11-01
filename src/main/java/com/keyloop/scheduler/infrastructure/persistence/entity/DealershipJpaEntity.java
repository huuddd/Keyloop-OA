package com.keyloop.scheduler.infrastructure.persistence.entity;

import com.keyloop.scheduler.domain.dealership.Dealership;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "dealerships")
public class DealershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(name = "operating_start", nullable = false)
    private LocalTime operatingStart;

    @Column(name = "operating_end", nullable = false)
    private LocalTime operatingEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DealershipJpaEntity() {}

    public static DealershipJpaEntity fromDomain(Dealership domain) {
        var entity = new DealershipJpaEntity();
        entity.id = domain.getId().value();
        entity.name = domain.getName();
        entity.address = domain.getAddress();
        entity.operatingStart = domain.getOperatingStart();
        entity.operatingEnd = domain.getOperatingEnd();
        return entity;
    }

    public Dealership toDomain() {
        return new Dealership(
            DealershipId.of(id),
            name, address,
            operatingStart, operatingEnd);
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public LocalTime getOperatingStart() { return operatingStart; }
    public LocalTime getOperatingEnd() { return operatingEnd; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
