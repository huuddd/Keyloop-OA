package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.AppointmentJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringDataAppointmentRepository
        extends JpaRepository<AppointmentJpaEntity, UUID>,
                JpaSpecificationExecutor<AppointmentJpaEntity> {

    List<AppointmentJpaEntity> findByCustomerId(Long customerId);

    // Availability check (no lock)
    @Query("""
        SELECT a FROM AppointmentJpaEntity a
        WHERE a.serviceBayId = :bayId
          AND a.status != com.keyloop.scheduler.domain.appointment.AppointmentStatus.CANCELLED
          AND a.startTime < :endTime
          AND a.endTime > :startTime
        """)
    List<AppointmentJpaEntity> findOverlappingForBay(
        @Param("bayId") Long bayId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // Booking — pessimistic lock (Deep Dive #1: double-booking prevention)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM AppointmentJpaEntity a
        WHERE a.serviceBayId = :bayId
          AND a.status != com.keyloop.scheduler.domain.appointment.AppointmentStatus.CANCELLED
          AND a.startTime < :endTime
          AND a.endTime > :startTime
        """)
    List<AppointmentJpaEntity> findOverlappingForBayWithLock(
        @Param("bayId") Long bayId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT a FROM AppointmentJpaEntity a
        WHERE a.technicianId = :techId
          AND a.status != com.keyloop.scheduler.domain.appointment.AppointmentStatus.CANCELLED
          AND a.startTime < :endTime
          AND a.endTime > :startTime
        """)
    List<AppointmentJpaEntity> findOverlappingForTechnicianWithLock(
        @Param("techId") Long techId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // Technician overlap (no lock — for availability)
    @Query("""
        SELECT a FROM AppointmentJpaEntity a
        WHERE a.technicianId = :techId
          AND a.status != com.keyloop.scheduler.domain.appointment.AppointmentStatus.CANCELLED
          AND a.startTime < :endTime
          AND a.endTime > :startTime
        """)
    List<AppointmentJpaEntity> findOverlappingForTechnician(
        @Param("techId") Long techId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    // All non-cancelled appointments at a dealership on a given date
    @Query("""
        SELECT a FROM AppointmentJpaEntity a
        WHERE a.serviceBayId IN (
            SELECT sb.id FROM ServiceBayJpaEntity sb
            WHERE sb.dealershipId = :dealershipId
        )
        AND a.status != com.keyloop.scheduler.domain.appointment.AppointmentStatus.CANCELLED
        AND CAST(a.startTime AS date) = :date
        """)
    List<AppointmentJpaEntity> findByDealershipAndDate(
        @Param("dealershipId") Long dealershipId,
        @Param("date") LocalDate date);
}
