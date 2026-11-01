package com.keyloop.scheduler.infrastructure.persistence.entity;

import com.keyloop.scheduler.domain.appointment.Appointment;
import com.keyloop.scheduler.domain.appointment.AppointmentId;
import com.keyloop.scheduler.domain.appointment.AppointmentStatus;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE appointments SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class AppointmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "service_bay_id", nullable = false)
    private Long serviceBayId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    // Audit fields
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Soft delete fields
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    protected AppointmentJpaEntity() {}

    // --- Domain <-> JPA Conversion ---

    public static AppointmentJpaEntity fromDomain(Appointment domain) {
        var entity = new AppointmentJpaEntity();
        entity.id = domain.getId().value();
        entity.customerId = domain.getCustomerId().value();
        entity.vehicleId = domain.getVehicleId().value();
        entity.serviceBayId = domain.getServiceBayId().value();
        entity.technicianId = domain.getTechnicianId().value();
        entity.serviceTypeId = domain.getServiceTypeId().value();
        entity.startTime = domain.getStartTime();
        entity.endTime = domain.getEndTime();
        entity.status = domain.getStatus();
        entity.createdAt = domain.getCreatedAt();
        entity.updatedAt = domain.getUpdatedAt();
        entity.createdBy = domain.getCreatedBy();
        entity.updatedBy = domain.getUpdatedBy();
        entity.deleted = domain.isDeleted();
        entity.deletedAt = domain.getDeletedAt();
        entity.deletedBy = domain.getDeletedBy();
        return entity;
    }

    public Appointment toDomain() {
        return Appointment.reconstruct(
            AppointmentId.of(id),
            CustomerId.of(customerId),
            VehicleId.of(vehicleId),
            ServiceBayId.of(serviceBayId),
            TechnicianId.of(technicianId),
            ServiceTypeId.of(serviceTypeId),
            startTime, endTime,
            status, createdAt, updatedAt);
    }

    // Getters
    public UUID getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getVehicleId() { return vehicleId; }
    public Long getServiceBayId() { return serviceBayId; }
    public Long getTechnicianId() { return technicianId; }
    public Long getServiceTypeId() { return serviceTypeId; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AppointmentStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public boolean isDeleted() { return deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public String getDeletedBy() { return deletedBy; }
}
