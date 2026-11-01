package com.keyloop.scheduler.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test double-booking prevention (Deep Dive #1).
 *
 * Scenario: Book ALL bays for the same time slot.
 * Seed data: 3 bays, 5 techs (all 5 qualified for GENERAL).
 * When 3 appointments fill all bays, appointment #4+ must get 409 Conflict.
 */
class ConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldPreventDoubleBookingUnderConcurrentRequests() throws Exception {
        var startTime = LocalDateTime.now().plusDays(30)
            .withHour(14).withMinute(0).withSecond(0).withNano(0);

        int threadCount = 6; // more than 3 bays
        var executor = Executors.newFixedThreadPool(threadCount);
        var latch = new CountDownLatch(1);
        var futures = new ArrayList<Future<HttpStatusCode>>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.await(); // all threads start at same time
                var request = Map.of(
                    "customerId", 1, "vehicleId", 1, "dealershipId", 1,
                    "serviceTypeId", 1, "startTime", startTime.toString()
                );
                var headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Idempotency-Key", UUID.randomUUID().toString());
                var response = restTemplate.postForEntity(
                    "/api/v1/appointments",
                    new HttpEntity<>(request, headers), Map.class);
                return response.getStatusCode();
            }));
        }

        latch.countDown(); // fire all at once
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        var statuses = futures.stream()
            .map(f -> {
                try { return f.get(); }
                catch (Exception e) { return HttpStatus.INTERNAL_SERVER_ERROR; }
            })
            .toList();

        // At most 3 should succeed (3 bays), rest should be 409
        long created = statuses.stream()
            .filter(s -> s == HttpStatus.CREATED).count();
        long conflict = statuses.stream()
            .filter(s -> s == HttpStatus.CONFLICT).count();

        assertThat(created)
            .as("At most 3 appointments should be created (3 bays)")
            .isGreaterThanOrEqualTo(1)
            .isLessThanOrEqualTo(3);
        assertThat(created + conflict)
            .as("All responses should be either 201 Created or 409 Conflict, no 500s")
            .isEqualTo(threadCount);
    }
}
