package com.keyloop.scheduler.infrastructure.persistence.entity;

import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.Technician;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "technicians")
public class TechnicianJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dealership_id", nullable = false)
    private Long dealershipId;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "technician_specializations",
        joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "specialization")
    @Enumerated(EnumType.STRING)
    private Set<Specialization> specializations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TechnicianJpaEntity() {}

    public static TechnicianJpaEntity fromDomain(Technician domain) {
        var entity = new TechnicianJpaEntity();
        entity.id = domain.getId().value();
        entity.dealershipId = domain.getDealershipId().value();
        entity.name = domain.getName();
        entity.specializations = domain.getSpecializations();
        return entity;
    }

    public Technician toDomain() {
        return new Technician(
            TechnicianId.of(id),
            DealershipId.of(dealershipId),
            name, specializations);
    }

    // Getters
    public Long getId() { return id; }
    public Long getDealershipId() { return dealershipId; }
    public String getName() { return name; }
    public Set<Specialization> getSpecializations() { return specializations; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
