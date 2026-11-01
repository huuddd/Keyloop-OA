package com.keyloop.scheduler.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers.
 *
 * Uses the "singleton container" pattern: one PostgreSQL container is started
 * once (on first use) and shared across all test classes in the same JVM run.
 * This avoids the container being stopped between test classes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scheduler_test")
            .withUsername("test")
            .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @BeforeEach
    void configureHttpClient() {
        // Use JDK HttpClient (java.net.http) to support PATCH method.
        // The default SimpleClientHttpRequestFactory uses HttpURLConnection
        // which does not support PATCH.
        restTemplate.getRestTemplate().setRequestFactory(
            new JdkClientHttpRequestFactory());
    }
}
