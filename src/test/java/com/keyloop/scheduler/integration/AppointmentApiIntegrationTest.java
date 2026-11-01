package com.keyloop.scheduler.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentApiIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldCreateAppointmentSuccessfully() {
        var request = Map.of(
            "customerId", 1,
            "vehicleId", 1,
            "dealershipId", 1,
            "serviceTypeId", 1,
            "startTime", futureTime(1, 9, 0)
        );

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        var response = restTemplate.postForEntity(
            "/api/v1/appointments", new HttpEntity<>(request, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCancelAppointment() {
        var appointmentId = createTestAppointment(10, 0);

        // Cancel via DELETE
        restTemplate.delete("/api/v1/appointments/" + appointmentId);

        // Verify status changed
        var fetched = restTemplate.getForEntity(
            "/api/v1/appointments/" + appointmentId, Map.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
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

        var fetched = restTemplate.getForEntity(
            "/api/v1/appointments/" + appointmentId, Map.class);
        assertThat(fetched.getBody().get("status")).isEqualTo("COMPLETED");
    }

    @Test
    void shouldMarkNoShow() {
        var appointmentId = createTestAppointment(13, 0);

        var response = restTemplate.exchange(
            "/api/v1/appointments/" + appointmentId + "/no-show",
            HttpMethod.PATCH, null, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var fetched = restTemplate.getForEntity(
            "/api/v1/appointments/" + appointmentId, Map.class);
        assertThat(fetched.getBody().get("status")).isEqualTo("NO_SHOW");
    }

    @Test
    void shouldSearchAppointmentsWithPagination() {
        // Create at least one appointment first to ensure results
        createTestAppointment(20, 0);

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
            "startTime", futureTime(5, 9, 0)
        );

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);
        var entity = new HttpEntity<>(request, headers);

        // First request
        var first = restTemplate.postForEntity("/api/v1/appointments", entity, Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Second request with same key
        var second = restTemplate.postForEntity("/api/v1/appointments", entity, Map.class);

        // Should return the same appointment id (cached response)
        assertThat(first.getBody().get("id")).isEqualTo(second.getBody().get("id"));
    }

    @Test
    void shouldReturn400WhenIdempotencyKeyMissing() {
        var request = Map.of(
            "customerId", 1, "vehicleId", 1, "dealershipId", 1,
            "serviceTypeId", 1,
            "startTime", futureTime(6, 9, 0)
        );

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Deliberately NOT setting Idempotency-Key
        var entity = new HttpEntity<>(request, headers);

        var response = restTemplate.postForEntity("/api/v1/appointments", entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- Helpers ---

    private String createTestAppointment(int dayOffset, int hourOffset) {
        var request = Map.of(
            "customerId", 1, "vehicleId", 1, "dealershipId", 1,
            "serviceTypeId", 1,
            "startTime", futureTime(dayOffset, 9 + hourOffset, 0)
        );
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var response = restTemplate.postForEntity(
            "/api/v1/appointments", new HttpEntity<>(request, headers), Map.class);
        assertThat(response.getStatusCode())
            .as("Failed to create test appointment: " + response.getBody())
            .isEqualTo(HttpStatus.CREATED);
        return response.getBody().get("id").toString();
    }

    /**
     * Generate a future LocalDateTime string within operating hours.
     */
    private String futureTime(int daysFromNow, int hour, int minute) {
        return LocalDateTime.now().plusDays(daysFromNow)
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            .toString();
    }
}
