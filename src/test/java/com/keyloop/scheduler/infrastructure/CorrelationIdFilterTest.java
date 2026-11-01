package com.keyloop.scheduler.infrastructure;

import com.keyloop.scheduler.infrastructure.filter.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generatesCorrelationIdWhenNotProvided() throws ServletException, IOException {
        // Arrange - no header set
        doAnswer(invocation -> {
            // During filter chain execution, MDC should have correlationId
            assertThat(MDC.get("correlationId")).isNotNull().isNotBlank();
            return null;
        }).when(chain).doFilter(request, response);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(chain).doFilter(request, response);
        assertThat(response.getHeader("X-Correlation-ID")).isNotNull().isNotBlank();
        // MDC should be cleaned up after filter
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void usesProvidedCorrelationIdFromHeader() throws ServletException, IOException {
        // Arrange
        var providedId = "test-correlation-id-123";
        request.addHeader("X-Correlation-ID", providedId);

        doAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo(providedId);
            return null;
        }).when(chain).doFilter(request, response);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(chain).doFilter(request, response);
        assertThat(response.getHeader("X-Correlation-ID")).isEqualTo(providedId);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void addsCorrelationIdToResponseHeader() throws ServletException, IOException {
        // Arrange - no header provided, filter generates one

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        var responseCorrelationId = response.getHeader("X-Correlation-ID");
        assertThat(responseCorrelationId).isNotNull();
        // Verify it looks like a UUID
        assertThat(responseCorrelationId).matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
