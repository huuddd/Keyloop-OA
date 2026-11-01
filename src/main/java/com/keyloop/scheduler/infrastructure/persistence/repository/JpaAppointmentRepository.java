package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.appointment.Appointment;
import com.keyloop.scheduler.domain.appointment.AppointmentId;
import com.keyloop.scheduler.domain.appointment.AppointmentRepository;
import com.keyloop.scheduler.domain.appointment.AppointmentStatus;
import com.keyloop.scheduler.domain.customer.CustomerId;
import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.ServiceBayId;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.infrastructure.persistence.entity.AppointmentJpaEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class JpaAppointmentRepository implements AppointmentRepository {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "startTime", "endTime", "status", "createdAt", "customerId");

    private final SpringDataAppointmentRepository springRepo;

    public JpaAppointmentRepository(SpringDataAppointmentRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public Appointment save(Appointment appointment) {
        var entity = AppointmentJpaEntity.fromDomain(appointment);
        var saved = springRepo.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Appointment> findById(AppointmentId id) {
        return springRepo.findById(id.value()).map(AppointmentJpaEntity::toDomain);
    }

    @Override
    public List<Appointment> findByCustomerId(CustomerId customerId) {
        return springRepo.findByCustomerId(customerId.value()).stream()
            .map(AppointmentJpaEntity::toDomain).toList();
    }

    @Override
    public List<Appointment> findOverlappingForBay(
            ServiceBayId bayId, LocalDateTime start, LocalDateTime end) {
        return springRepo.findOverlappingForBay(bayId.value(), start, end).stream()
            .map(AppointmentJpaEntity::toDomain).toList();
    }

    @Override
    public List<Appointment> findOverlappingForTechnician(
            TechnicianId techId, LocalDateTime start, LocalDateTime end) {
        return springRepo.findOverlappingForTechnician(techId.value(), start, end).stream()
            .map(AppointmentJpaEntity::toDomain).toList();
    }

    @Override
    public List<Appointment> findOverlappingForBayWithLock(
            ServiceBayId bayId, LocalDateTime start, LocalDateTime end) {
        return springRepo.findOverlappingForBayWithLock(bayId.value(), start, end).stream()
            .map(AppointmentJpaEntity::toDomain).toList();
    }

    @Override
    public List<Appointment> findOverlappingForTechnicianWithLock(
            TechnicianId techId, LocalDateTime start, LocalDateTime end) {
        return springRepo.findOverlappingForTechnicianWithLock(techId.value(), start, end).stream()
            .map(AppointmentJpaEntity::toDomain).toList();
    }

    @Override
    public List<Appointment> findByDealershipAndDate(DealershipId dealershipId, LocalDate date) {
        return springRepo.findByDealershipAndDate(dealershipId.value(), date).stream()
            .map(AppointmentJpaEntity::toDomain).toList();
    }

    @Override
    public SearchResult searchAppointments(
            Long customerId, String status,
            LocalDate dateFrom, LocalDate dateTo,
            String sortBy, String sortDirection,
            int pageIndex, int pageSize) {

        Specification<AppointmentJpaEntity> spec = Specification.where(null);

        if (customerId != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("customerId"), customerId));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), AppointmentStatus.valueOf(status)));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("startTime"), dateFrom.atStartOfDay()));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) ->
                cb.lessThan(root.get("startTime"), dateTo.plusDays(1).atStartOfDay()));
        }

        // Sort column whitelist — prevents SQL injection
        String safeSortBy = ALLOWED_SORT_COLUMNS.contains(sortBy) ? sortBy : "startTime";
        Sort sort = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.by(safeSortBy).ascending()
            : Sort.by(safeSortBy).descending();

        var page = springRepo.findAll(spec, PageRequest.of(pageIndex, pageSize, sort));
        var items = page.getContent().stream()
            .map(AppointmentJpaEntity::toDomain).toList();

        return new SearchResult(items, page.getTotalElements());
    }
}
