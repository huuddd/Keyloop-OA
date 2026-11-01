package com.keyloop.scheduler.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys")
public class IdempotencyKeyJpaEntity {

    @Id
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_content_type", length = 128)
    private String responseContentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    protected IdempotencyKeyJpaEntity() {}

    public IdempotencyKeyJpaEntity(String idempotencyKey, int responseStatus,
                                    String responseBody, String responseContentType,
                                    LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseContentType = responseContentType;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    // Getters
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public String getResponseContentType() { return responseContentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
