package com.keyloop.scheduler.infrastructure.filter;

import com.keyloop.scheduler.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import com.keyloop.scheduler.infrastructure.persistence.repository.SpringDataIdempotencyKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final int TTL_HOURS = 24;

    private final SpringDataIdempotencyKeyRepository idempotencyRepo;

    public IdempotencyFilter(SpringDataIdempotencyKeyRepository idempotencyRepo) {
        this.idempotencyRepo = idempotencyRepo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Idempotency-Key header is required\"}");
            return;
        }

        // Check if key was already processed
        var existing = idempotencyRepo.findById(key);
        if (existing.isPresent()) {
            var cached = existing.get();
            response.setStatus(cached.getResponseStatus());
            response.setContentType(cached.getResponseContentType() != null
                ? cached.getResponseContentType() : "application/json");
            response.getWriter().write(
                cached.getResponseBody() != null ? cached.getResponseBody() : "");
            return;
        }

        // Wrap response to capture output
        var wrappedResponse = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrappedResponse);

        // Store response for this key (24h TTL)
        var now = LocalDateTime.now();
        var entity = new IdempotencyKeyJpaEntity(
            key,
            wrappedResponse.getStatus(),
            new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8),
            wrappedResponse.getContentType(),
            now,
            now.plusHours(TTL_HOURS)
        );
        idempotencyRepo.save(entity);

        wrappedResponse.copyBodyToResponse();
    }
}
