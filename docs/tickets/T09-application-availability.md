# T09 — Application: Availability Query (CQRS)

**Type:** Feature | **Priority:** P1
**Depends on:** T08
**Blocks:** T11

---

## Description

Implement `GetAvailableSlotsQueryHandler` — a read-only CQRS query handler that computes available time slots for a given dealership, service type, and date. Returns a paginated result of bay + technician + time combinations.

This follows the CQRS pattern: `GetAvailableSlotsQuery` implements `Query<PagedResult<AvailableSlotDto>>`, handled by `GetAvailableSlotsQueryHandler`.

## Acceptance Criteria

- [ ] `GetAvailableSlotsQuery` record implements `Query<PagedResult<AvailableSlotDto>>`
- [ ] `GetAvailableSlotsQueryHandler` implements `QueryHandler`
- [ ] Calculates all available slots = all possible − booked (per bay × per tech)
- [ ] Uses typed IDs (`DealershipId`, `ServiceTypeId`) — anti-corruption layer at handler boundary
- [ ] Validates: dealership exists, serviceType exists, date not in past
- [ ] Returns sorted by startTime with OFFSET-based pagination (`PagedResult<AvailableSlotDto>`)
- [ ] Unit tests with mock repositories

## Implementation Details

### Package: `application.appointment.query`

#### `GetAvailableSlotsQuery.java`

```java
public record GetAvailableSlotsQuery(
    Long dealershipId,
    Long serviceTypeId,
    LocalDate date,
    int pageIndex,
    int pageSize
) implements Query<PagedResult<AvailableSlotDto>> {}
```

#### `GetAvailableSlotsQueryHandler.java`

```java
@Service
@Transactional(readOnly = true)
public class GetAvailableSlotsQueryHandler
        implements QueryHandler<GetAvailableSlotsQuery, PagedResult<AvailableSlotDto>> {

    private final DealershipRepository dealershipRepo;
    private final ServiceTypeRepository serviceTypeRepo;
    private final ServiceBayRepository serviceBayRepo;
    private final TechnicianRepository technicianRepo;
    private final AppointmentRepository appointmentRepo;

    // Constructor injection

    @Override
    public PagedResult<AvailableSlotDto> handle(GetAvailableSlotsQuery query) {
        // Anti-corruption layer: convert raw IDs to typed IDs
        var dealershipId = DealershipId.of(query.dealershipId());
        var serviceTypeId = ServiceTypeId.of(query.serviceTypeId());

        // 1. Validate
        var dealership = dealershipRepo.findById(dealershipId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Dealership not found: " + query.dealershipId()));
        var serviceType = serviceTypeRepo.findById(serviceTypeId)
            .orElseThrow(() -> new EntityNotFoundException(
                "ServiceType not found: " + query.serviceTypeId()));
        if (query.date().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Date must not be in the past");
        }

        // 2-3. Generate possible start times
        var possibleStarts = dealership.generatePossibleStartTimes(
            serviceType.getDurationMinutes());

        // 4-5. Fetch resources
        var bays = serviceBayRepo.findActiveByDealershipId(dealershipId);
        var technicians = technicianRepo.findByDealershipIdAndSpecialization(
            dealershipId, serviceType.getRequiredSpecialization());

        // 6. Existing non-cancelled appointments for that date
        var existingAppointments = appointmentRepo.findByDealershipAndDate(
            dealershipId, query.date());

        // 7. Compute available slots
        List<AvailableSlotDto> availableSlots = new ArrayList<>();

        for (var bay : bays) {
            for (var startLocalTime : possibleStarts) {
                var startDateTime = LocalDateTime.of(query.date(), startLocalTime);
                var timeSlot = TimeSlot.of(startDateTime,
                    serviceType.getDurationMinutes());

                // Check bay availability
                boolean bayBusy = existingAppointments.stream()
                    .anyMatch(apt -> apt.getServiceBayId().equals(bay.getId())
                                  && apt.getTimeSlot().overlapsWith(timeSlot));
                if (bayBusy) continue;

                // Find first available qualified technician
                for (var tech : technicians) {
                    boolean techBusy = existingAppointments.stream()
                        .anyMatch(apt -> apt.getTechnicianId().equals(tech.getId())
                                      && apt.getTimeSlot().overlapsWith(timeSlot));
                    if (!techBusy) {
                        availableSlots.add(new AvailableSlotDto(
                            timeSlot.startTime(), timeSlot.endTime(),
                            bay.getId().value(), bay.getName(),
                            tech.getId().value(), tech.getName()));
                        break; // 1 tech per bay-slot combo
                    }
                }
            }
        }

        // 8. Sort
        availableSlots.sort(Comparator.comparing(AvailableSlotDto::startTime));

        // 9. In-memory pagination
        int totalCount = availableSlots.size();
        int fromIndex = Math.min(query.pageIndex() * query.pageSize(), totalCount);
        int toIndex = Math.min(fromIndex + query.pageSize(), totalCount);
        var page = availableSlots.subList(fromIndex, toIndex);

        return new PagedResult<>(page, totalCount, query.pageIndex(), query.pageSize());
    }
}
```

### Package: `application.dto`

#### `AvailableSlotDto.java`

```java
public record AvailableSlotDto(
    LocalDateTime startTime,
    LocalDateTime endTime,
    Long serviceBayId,
    String serviceBayName,
    Long technicianId,
    String technicianName
) {}
```

#### `PagedResult.java`

```java
public record PagedResult<T>(
    List<T> items,
    long totalCount,
    int pageIndex,
    int pageSize,
    int totalPages,
    boolean hasPrevious,
    boolean hasNext
) {
    public PagedResult(List<T> items, long totalCount, int pageIndex, int pageSize) {
        this(items, totalCount, pageIndex, pageSize,
            (int) Math.ceil((double) totalCount / pageSize),
            pageIndex > 0,
            (long) (pageIndex + 1) * pageSize < totalCount);
    }
}
```

### Test: `GetAvailableSlotsQueryHandlerTest.java`

```java
@ExtendWith(MockitoExtension.class)
class GetAvailableSlotsQueryHandlerTest {

    @Mock DealershipRepository dealershipRepo;
    @Mock ServiceTypeRepository serviceTypeRepo;
    @Mock ServiceBayRepository serviceBayRepo;
    @Mock TechnicianRepository technicianRepo;
    @Mock AppointmentRepository appointmentRepo;

    @InjectMocks GetAvailableSlotsQueryHandler handler;

    private Dealership dealership;
    private ServiceType oilChange;
    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        dealership = /* Dealership with openTime=08:00, closeTime=17:00 */;
        oilChange = /* ServiceType: "Oil Change", 60 min, GENERAL */;
        tomorrow = LocalDate.now().plusDays(1);
    }

    @Test
    void shouldReturnSlotsWhenNoExistingAppointments() {
        // Given: 1 bay, 1 technician, no appointments
        // 60-min service, 08:00–17:00, 30-min granularity
        // Last valid start: 16:00 (16:00 + 60 = 17:00)
        // Total: 08:00, 08:30, ..., 16:00 = 17 slots × 1 bay = 17

        var query = new GetAvailableSlotsQuery(1L, 1L, tomorrow, 0, 20);
        var result = handler.handle(query);

        assertThat(result.items()).hasSize(17);
        assertThat(result.totalCount()).isEqualTo(17);
    }

    @Test
    void shouldExcludeBookedSlots() {
        // Given: 1 bay, 1 tech, 1 existing appointment at 09:00–10:00
        // 09:00 and 09:30 slots excluded (overlap with 09:00–10:00)
        // Result: 17 - 2 = 15 slots

        var query = new GetAvailableSlotsQuery(1L, 1L, tomorrow, 0, 20);
        var result = handler.handle(query);

        assertThat(result.items()).hasSize(15);
        assertThat(result.items()).noneMatch(s ->
            s.startTime().toLocalTime().equals(LocalTime.of(9, 0))
            || s.startTime().toLocalTime().equals(LocalTime.of(9, 30)));
    }

    @Test
    void shouldThrowWhenDealershipNotFound() {
        var query = new GetAvailableSlotsQuery(999L, 1L, tomorrow, 0, 20);

        assertThatThrownBy(() -> handler.handle(query))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void shouldThrowWhenDateInPast() {
        var yesterday = LocalDate.now().minusDays(1);
        var query = new GetAvailableSlotsQuery(1L, 1L, yesterday, 0, 20);

        assertThatThrownBy(() -> handler.handle(query))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("past");
    }
}
```

## Commit message

```
feat(app): add GetAvailableSlotsQueryHandler with CQRS pattern
```
