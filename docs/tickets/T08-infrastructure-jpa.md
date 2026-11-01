# T08 — Infrastructure: JPA Entities + Repository Implementations

**Type:** Feature | **Priority:** P1
**Depends on:** T03, T07
**Blocks:** T09

---

## Description

Create JPA entity classes (with `@Entity`, `@Table` annotations) and repository adapter implementations that bridge the domain repository interfaces (ports) to Spring Data JPA.

Key design decisions:
- **JPA entities are separate from domain entities** — JPA entity = data mapping, domain entity = business logic
- **Typed ID converters** unwrap strongly-typed IDs (`AppointmentId`, `CustomerId`, etc.) to their raw DB types
- **Audit fields** populated automatically via Spring Data JPA Auditing (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`)
- **Soft delete** via Hibernate `@SQLDelete` + `@Where` — DELETE becomes `UPDATE SET deleted = true`
- **Sort column whitelist** prevents SQL injection in dynamic search queries

## Acceptance Criteria

- [x] JPA entities for all tables (mapping matches schema from T03)
- [x] `AppointmentJpaEntity` with audit fields, soft delete annotations, typed ID conversion
- [x] `SpringDataAppointmentRepository` extends `JpaRepository` + `JpaSpecificationExecutor`
- [x] Pessimistic lock queries (`@Lock(PESSIMISTIC_WRITE)` + `@Query`)
- [x] `JpaAppointmentRepository` adapter implements `AppointmentRepository` domain port
- [x] `searchAppointments()` with `Specification`-based dynamic filtering + sort whitelist
- [x] Strongly-typed ID converters (`AppointmentIdConverter`, `CustomerIdConverter`, etc.)
- [x] All other JPA entities + adapters for remaining domain repositories
- [x] App starts successfully, Hibernate validates schema

## Implementation Details

### Package: `infrastructure.persistence.entity`

#### `AppointmentJpaEntity.java`

```java
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

    // Audit fields — populated by Spring Data JPA Auditing
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

    // --- Domain ↔ JPA Conversion ---

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

    // Getters for all fields...
}
```

Similarly create: `DealershipJpaEntity`, `ServiceBayJpaEntity`, `TechnicianJpaEntity`, `ServiceTypeJpaEntity`, `CustomerJpaEntity`, `VehicleJpaEntity`, `IdempotencyKeyJpaEntity` — all with `@EntityListeners`, `@SQLDelete`, `@Where`, and audit fields.

#### `TechnicianJpaEntity.java` (noteworthy: M:N specializations)

```java
@Entity
@Table(name = "technicians")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE technicians SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
public class TechnicianJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long dealershipId;
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "technician_specializations",
        joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "specialization")
    @Enumerated(EnumType.STRING)
    private Set<Specialization> specializations;

    // Audit + soft delete fields (same pattern)...

    public Technician toDomain() {
        return new Technician(
            TechnicianId.of(id),
            DealershipId.of(dealershipId),
            name, specializations);
    }
}
```

### Package: `infrastructure.persistence.converter`

Strongly-typed ID converters for JPA:

```java
@Converter(autoApply = true)
public class AppointmentIdConverter implements AttributeConverter<AppointmentId, UUID> {
    @Override
    public UUID convertToDatabaseColumn(AppointmentId id) {
        return id == null ? null : id.value();
    }
    @Override
    public AppointmentId convertToEntityAttribute(UUID value) {
        return value == null ? null : AppointmentId.of(value);
    }
}
```

Similarly: `CustomerIdConverter` (Long), `VehicleIdConverter` (Long), `DealershipIdConverter` (Long), `ServiceBayIdConverter` (Long), `TechnicianIdConverter` (Long), `ServiceTypeIdConverter` (Long).

### Package: `infrastructure.persistence.repository`

#### `SpringDataAppointmentRepository.java`

```java
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
```

#### `JpaAppointmentRepository.java` (Adapter — implements domain port)

```java
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
```

Similarly create adapters: `JpaCustomerRepository`, `JpaVehicleRepository`, `JpaDealershipRepository`, `JpaServiceBayRepository`, `JpaTechnicianRepository`, `JpaServiceTypeRepository` — each implementing its domain port and delegating to its `SpringData*Repository`.

### Package: `infrastructure.config`

#### `JpaAuditConfig.java`

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    // Enables @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy
}
```

## Commit message

```
feat(infra): add JPA entities, repository adapters, and typed ID converters
```
