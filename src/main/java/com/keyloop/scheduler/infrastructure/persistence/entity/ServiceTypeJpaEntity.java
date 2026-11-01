package com.keyloop.scheduler.infrastructure.persistence.entity;

import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_types")
public class ServiceTypeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_specialization", nullable = false, length = 100)
    private Specialization requiredSpecialization;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ServiceTypeJpaEntity() {}

    public static ServiceTypeJpaEntity fromDomain(ServiceType domain) {
        var entity = new ServiceTypeJpaEntity();
        entity.id = domain.getId().value();
        entity.name = domain.getName();
        entity.durationMinutes = domain.getDurationMinutes();
        entity.requiredSpecialization = domain.getRequiredSpecialization();
        return entity;
    }

    public ServiceType toDomain() {
        return new ServiceType(
            ServiceTypeId.of(id),
            name, durationMinutes,
            requiredSpecialization);
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public int getDurationMinutes() { return durationMinutes; }
    public Specialization getRequiredSpecialization() { return requiredSpecialization; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
