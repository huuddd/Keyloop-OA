-- Idempotency key storage for preventing duplicate request processing.
-- Stores cached responses for POST/PUT requests with Idempotency-Key header.

CREATE TABLE idempotency_keys (
    idempotency_key       VARCHAR(64)   PRIMARY KEY,
    response_status       INTEGER       NOT NULL,
    response_body         TEXT,
    response_content_type VARCHAR(128),
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    expires_at            TIMESTAMP     NOT NULL
);

CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys (expires_at);
