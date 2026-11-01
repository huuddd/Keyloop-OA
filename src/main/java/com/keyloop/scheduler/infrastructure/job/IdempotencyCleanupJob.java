package com.keyloop.scheduler.infrastructure.job;

import com.keyloop.scheduler.infrastructure.persistence.repository.SpringDataIdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class IdempotencyCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupJob.class);

    private final SpringDataIdempotencyKeyRepository repository;
    private final Clock clock;

    public IdempotencyCleanupJob(SpringDataIdempotencyKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void cleanupExpiredKeys() {
        var now = LocalDateTime.now(clock);
        var deleted = repository.deleteExpired(now);
        if (deleted > 0) {
            log.info("Cleaned up {} expired idempotency keys (expired before {})",
                deleted, now);
        }
    }
}
