# T12 — Integration Tests (Testcontainers) + Architecture Tests

**Type:** Test | **Priority:** P1
**Depends on:** T11
**Blocks:** T13

---

## Description

Create integration tests running against **real PostgreSQL** (Testcontainers) to verify:
1. API endpoints work end-to-end (including idempotency and PATCH endpoints)
2. Double-booking prevention (pessimistic locking) works correctly under concurrent load
3. Flyway migrations apply successfully
4. Architecture constraints are enforced via ArchUnit

## Acceptance Criteria

- [x] Base test class with Testcontainers PostgreSQL configuration
- [x] Test: create appointment → 201 + correct response (with `Idempotency-Key`)
- [x] Test: create appointment with invalid data → 400
- [x] Test: create + cancel → 204
- [x] Test: check-in, complete, no-show lifecycle transitions
- [x] Test: get availability → returns paginated slots
- [x] Test: search appointments → paginated results with filters
- [x] Test: **concurrent booking** — multiple threads book same slot → some succeed, rest get 409
- [x] Test: duplicate `Idempotency-Key` → returns cached response
- [x] ArchUnit tests for Clean Architecture layer constraints
- [x] All tests pass: `mvn test`

## Implementation Details

### Base class: `BaseIntegrationTest.java`

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("scheduler_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;
}
```

### `AppointmentApiIntegrationTest.java`

```java
class AppointmentApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldCreateAppointmentSuccessfully() {
        var request = Map.of(
            "customerId", 1,
            "vehicleId", 1,
            "dealershipId", 1,
            "serviceTypeId", 1,
            "startTime", LocalDateTime.now().plusDays(1)
                .withHour(9).withMinute(0).withSecond(0).withNano(0).toString()
        );

        var headers = new HttpHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var entity = new HttpEntity<>(request, headers);

        var response = restTemplate.postForEntity(
            "/api/v1/appointments", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    void shouldReturn400ForMissingFields() {
        var request = Map.of("customerId", 1); // missing other fields
        var headers = new HttpHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        var response = restTemplate.postForEntity(
            "/api/v1/appointments", new HttpEntity<>(request, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCancelAppointment() {
        // Create first
        var appointmentId = createTestAppointment(10, 0);

        // Cancel
        restTemplate.delete("/api/v1/appointments/" + appointmentId);

        // Verify status changed
        var fetched = restTemplate.getForEntity(
            "/api/v1/appointments/" + appointmentId, Map.class);
        assertThat(fetched.getBody().get("status")).isEqualTo("CANCELLED");
    }

    @Test
    void shouldCheckInAppointment() {
        var appointmentId = createTestAppointment(11, 0);

        var response = restTemplate.exchange(
            "/api/v1/appointments/" + appointmentId + "/check-in",
            HttpMethod.PATCH, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var fetched = restTemplate.getForEntity(
            "/api/v1/appointments/" + appointmentId, Map.class);
        assertThat(fetched.getBody().get("status")).isEqualTo("IN_PROGRESS");
    }

    @Test
    void shouldCompleteAppointmentAfterCheckIn() {
        var appointmentId = createTestAppointment(12, 0);

        // Check in first
        restTemplate.exchange(
            "/api/v1/appointments/" + appointmentId + "/check-in",
            HttpMethod.PATCH, null, Void.class);

        // Then complete
        var response = restTemplate.exchange(
            "/api/v1/appointments/" + appointmentId + "/complete",
            HttpMethod.PATCH, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldSearchAppointmentsWithPagination() {
        var response = restTemplate.getForEntity(
            "/api/v1/appointments?pageIndex=0&pageSize=5&sortBy=startTime&sortDirection=desc",
            Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("items");
        assertThat(response.getBody()).containsKey("totalCount");
        assertThat(response.getBody()).containsKey("pageIndex");
    }

    @Test
    void shouldReturnCachedResponseForDuplicateIdempotencyKey() {
        var idempotencyKey = UUID.randomUUID().toString();
        var request = Map.of(
            "customerId", 1, "vehicleId", 1, "dealershipId", 1,
            "serviceTypeId", 1,
            "startTime", LocalDateTime.now().plusDays(5)
                .withHour(9).withMinute(0).withSecond(0).withNano(0).toString()
        );

        var headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        var entity = new HttpEntity<>(request, headers);

        // First request
        var first = restTemplate.postForEntity("/api/v1/appointments", entity, Map.class);
        // Second request with same key
        var second = restTemplate.postForEntity("/api/v1/appointments", entity, Map.class);

        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
    }

    private String createTestAppointment(int dayOffset, int hourOffset) {
        var request = Map.of(
            "customerId", 1, "vehicleId", 1, "dealershipId", 1,
            "serviceTypeId", 1,
            "startTime", LocalDateTime.now().plusDays(dayOffset)
                .withHour(9 + hourOffset).withMinute(0).withSecond(0).withNano(0).toString()
        );
        var headers = new HttpHeaders();
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var response = restTemplate.postForEntity(
            "/api/v1/appointments", new HttpEntity<>(request, headers), Map.class);
        return response.getBody().get("id").toString();
    }
}
```

### `AvailabilityIntegrationTest.java`

```java
class AvailabilityIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldReturnAvailableSlotsWithPagination() {
        var tomorrow = LocalDate.now().plusDays(1);
        var response = restTemplate.getForEntity(
            "/api/v1/dealerships/1/availability?serviceTypeId=1&date=" + tomorrow
                + "&pageIndex=0&pageSize=10",
            Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("items");
        assertThat(response.getBody()).containsKey("totalCount");
    }
}
```

### `ConcurrencyIntegrationTest.java` (Key test)

```java
/**
 * Test double-booking prevention (Deep Dive #1).
 *
 * Scenario: Book ALL bays for the same time slot.
 * Seed data: 3 bays, 5 techs (3 qualified for GENERAL).
 * When 3 appointments fill all bays, appointment #4 must get 409 Conflict.
 */
class ConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldPreventDoubleBookingUnderConcurrentRequests() throws Exception {
        var startTime = LocalDateTime.now().plusDays(3)
            .withHour(14).withMinute(0).withSecond(0).withNano(0);

        int threadCount = 6; // more than 3 bays
        var executor = Executors.newFixedThreadPool(threadCount);
        var latch = new CountDownLatch(1);
        var futures = new ArrayList<Future<HttpStatus>>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await(); // all threads start at same time
                var request = Map.of(
                    "customerId", 1, "vehicleId", 1, "dealershipId", 1,
                    "serviceTypeId", 1, "startTime", startTime.toString()
                );
                var headers = new HttpHeaders();
                headers.set("Idempotency-Key", UUID.randomUUID().toString());
                var response = restTemplate.postForEntity(
                    "/api/v1/appointments",
                    new HttpEntity<>(request, headers), Map.class);
                return (HttpStatus) response.getStatusCode();
            }));
        }

        latch.countDown(); // fire all at once
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        var statuses = futures.stream()
            .map(f -> {
                try { return f.get(); }
                catch (Exception e) { return HttpStatus.INTERNAL_SERVER_ERROR; }
            })
            .toList();

        // At most 3 should succeed (3 bays), rest should be 409
        long created = statuses.stream().filter(s -> s == HttpStatus.CREATED).count();
        long conflict = statuses.stream().filter(s -> s == HttpStatus.CONFLICT).count();

        assertThat(created).isLessThanOrEqualTo(3);
        assertThat(created + conflict).isEqualTo(threadCount);
        // No 500 errors — contention handled gracefully
    }
}
```

### ArchUnit Tests

```java
class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter()
        .importPackages("com.keyloop.scheduler");

    @Test
    void domainShouldNotDependOnOtherLayers() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..application..", "..api..", "..infrastructure..")
            .check(classes);
    }

    @Test
    void applicationShouldNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..infrastructure..")
            .check(classes);
    }

    @Test
    void domainShouldNotUseSpringAnnotations() {
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "jakarta.persistence..")
            .check(classes);
    }
}
```

## Commit message

```
test: add integration tests with Testcontainers, concurrency, and ArchUnit
```
