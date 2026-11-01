package com.keyloop.scheduler.domain.appointment;

import com.keyloop.scheduler.domain.shared.Result;
import com.keyloop.scheduler.domain.appointment.event.*;
import com.keyloop.scheduler.domain.shared.AggregateRoot;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.servicetype.ServiceType;
import com.keyloop.scheduler.domain.servicetype.ServiceTypeId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.customer.VehicleId;
import com.keyloop.scheduler.domain.dealership.Technician;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * AGGREGATE ROOT.
 *
 * Invariants:
 *   1. endTime > startTime
 *   2. startTime must be in the future (at creation time)
 *   3. Technician must be qualified for the ServiceType
 *   4. Status transitions follow state machine (AppointmentStatus.canTransitionTo)
 *
 * Note: Double-booking prevention is NOT within this Aggregate --
 * it is a cross-aggregate constraint, handled by Application Service + DB lock.
 */
public class Appointment extends AggregateRoot<AppointmentId> {

    private AppointmentId id;
    private CustomerId customerId;
    private VehicleId vehicleId;
    private ServiceBayId serviceBayId;
    private TechnicianId technicianId;
    private ServiceTypeId serviceTypeId;
    private TimeSlot timeSlot;
    private AppointmentStatus status;

    private Appointment() {}

    /**
     * Factory method -- creates a new Appointment.
     * Validates all domain invariants at creation time.
     *
     * @param clock injected clock for deterministic time
     * @return Result containing the new Appointment, or an Error
     */
    public static Result<Appointment> create(
            CustomerId customerId,
            VehicleId vehicleId,
            ServiceBayId serviceBayId,
            TechnicianId technicianId,
            ServiceType serviceType,
            Technician technician,
            LocalDateTime startTime,
            Clock clock) {

        // Invariant: Technician must be qualified
        if (!technician.isQualifiedFor(serviceType.getRequiredSpecialization())) {
            return Result.failure(AppointmentErrors.TECHNICIAN_NOT_QUALIFIED);
        }

        // Invariant: startTime must be in the future
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            return Result.failure(AppointmentErrors.START_TIME_IN_PAST);
        }

        var now = LocalDateTime.now(clock);
        var appointment = new Appointment();
        appointment.id = AppointmentId.generate();
        appointment.customerId = customerId;
        appointment.vehicleId = vehicleId;
        appointment.serviceBayId = serviceBayId;
        appointment.technicianId = technicianId;
        appointment.serviceTypeId = serviceType.getId();
        appointment.timeSlot = TimeSlot.of(startTime, serviceType.getDurationMinutes());
        appointment.status = AppointmentStatus.CONFIRMED;
        appointment.setCreatedAt(now);
        appointment.setUpdatedAt(now);

        // Raise domain event
        appointment.registerEvent(AppointmentCreatedEvent.of(
            appointment.id.value(), customerId.value(), serviceBayId.value(),
            technicianId.value(), appointment.getStartTime(), appointment.getEndTime()));

        return Result.success(appointment);
    }

    /** Cancel: CONFIRMED or IN_PROGRESS -> CANCELLED */
    public Result<Void> cancel(Clock clock) {
        var guard = guardTransition(AppointmentStatus.CANCELLED);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.CANCELLED;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentCancelledEvent.of(
            this.id.value(), this.customerId.value(), getUpdatedAt()));
        return Result.success();
    }

    /** Check in: CONFIRMED -> IN_PROGRESS */
    public Result<Void> checkIn(Clock clock) {
        var guard = guardTransition(AppointmentStatus.IN_PROGRESS);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.IN_PROGRESS;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentCheckedInEvent.of(this.id.value(), getUpdatedAt()));
        return Result.success();
    }

    /** Complete: IN_PROGRESS -> COMPLETED */
    public Result<Void> complete(Clock clock) {
        var guard = guardTransition(AppointmentStatus.COMPLETED);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.COMPLETED;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentCompletedEvent.of(this.id.value(), getUpdatedAt()));
        return Result.success();
    }

    /** Mark no-show: CONFIRMED -> NO_SHOW */
    public Result<Void> markNoShow(Clock clock) {
        var guard = guardTransition(AppointmentStatus.NO_SHOW);
        if (guard.isFailure()) return guard;
        this.status = AppointmentStatus.NO_SHOW;
        setUpdatedAt(LocalDateTime.now(clock));
        registerEvent(AppointmentNoShowEvent.of(
            this.id.value(), this.customerId.value(), getUpdatedAt()));
        return Result.success();
    }

    private Result<Void> guardTransition(AppointmentStatus target) {
        if (!this.status.canTransitionTo(target)) {
            return Result.failure(
                AppointmentErrors.invalidTransition(this.status.name(), target.name()));
        }
        return Result.success();
    }

    // Getters (no setters -- state change only via domain methods)
    @Override public AppointmentId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public VehicleId getVehicleId() { return vehicleId; }
    public ServiceBayId getServiceBayId() { return serviceBayId; }
    public TechnicianId getTechnicianId() { return technicianId; }
    public ServiceTypeId getServiceTypeId() { return serviceTypeId; }
    public LocalDateTime getStartTime() { return timeSlot.startTime(); }
    public LocalDateTime getEndTime() { return timeSlot.endTime(); }
    public TimeSlot getTimeSlot() { return timeSlot; }
    public AppointmentStatus getStatus() { return status; }

    /** Reconstruct from persistence (no validation, no events). */
    public static Appointment reconstruct(
            AppointmentId id, CustomerId customerId, VehicleId vehicleId,
            ServiceBayId serviceBayId, TechnicianId technicianId, ServiceTypeId serviceTypeId,
            LocalDateTime startTime, LocalDateTime endTime,
            AppointmentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        var apt = new Appointment();
        apt.id = id;
        apt.customerId = customerId;
        apt.vehicleId = vehicleId;
        apt.serviceBayId = serviceBayId;
        apt.technicianId = technicianId;
        apt.serviceTypeId = serviceTypeId;
        apt.timeSlot = new TimeSlot(startTime, endTime);
        apt.status = status;
        apt.setCreatedAt(createdAt);
        apt.setUpdatedAt(updatedAt);
        return apt;
    }
}
