package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SpringDataIdempotencyKeyRepository
        extends JpaRepository<IdempotencyKeyJpaEntity, String> {

    @Modifying
    @Query("DELETE FROM IdempotencyKeyJpaEntity e WHERE e.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
