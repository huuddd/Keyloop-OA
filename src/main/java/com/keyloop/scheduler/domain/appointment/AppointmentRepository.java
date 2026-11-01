package com.keyloop.scheduler.domain.appointment;

import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(AppointmentId id);

    List<Appointment> findByCustomerId(CustomerId customerId);

    /** Overlap check (no lock) — used for availability queries. */
    List<Appointment> findOverlappingForBay(
        ServiceBayId serviceBayId, LocalDateTime startTime, LocalDateTime endTime);

    List<Appointment> findOverlappingForTechnician(
        TechnicianId technicianId, LocalDateTime startTime, LocalDateTime endTime);

    /** Pessimistic lock version — SELECT FOR UPDATE (Deep Dive #1). */
    List<Appointment> findOverlappingForBayWithLock(
        ServiceBayId serviceBayId, LocalDateTime startTime, LocalDateTime endTime);

    List<Appointment> findOverlappingForTechnicianWithLock(
        TechnicianId technicianId, LocalDateTime startTime, LocalDateTime endTime);

    /** All appointments for a dealership on a given date (availability calculation). */
    List<Appointment> findByDealershipAndDate(DealershipId dealershipId, LocalDate date);

    /** Paged search with optional filters and sorting. */
    SearchResult searchAppointments(
        Long customerId, String status,
        LocalDate dateFrom, LocalDate dateTo,
        String sortBy, String sortDirection,
        int pageIndex, int pageSize);

    record SearchResult(List<Appointment> items, long totalCount) {}
}
