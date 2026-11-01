package com.keyloop.scheduler.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(response.getBody()).containsKey("pageIndex");
    }

    @Test
    void shouldReturnEmptyForPastDate() {
        var yesterday = LocalDate.now().minusDays(1);
        var response = restTemplate.getForEntity(
            "/api/v1/dealerships/1/availability?serviceTypeId=1&date=" + yesterday
                + "&pageIndex=0&pageSize=10",
            Map.class);

        // Past date should return 400 (IllegalArgumentException)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn404ForNonExistentDealership() {
        var tomorrow = LocalDate.now().plusDays(1);
        var response = restTemplate.getForEntity(
            "/api/v1/dealerships/999/availability?serviceTypeId=1&date=" + tomorrow
                + "&pageIndex=0&pageSize=10",
            Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
