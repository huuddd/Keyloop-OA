package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.ServiceBayJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataServiceBayRepository extends JpaRepository<ServiceBayJpaEntity, Long> {

    List<ServiceBayJpaEntity> findByDealershipIdAndIsActiveTrue(Long dealershipId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sb FROM ServiceBayJpaEntity sb
        WHERE sb.dealershipId = :dealershipId
          AND sb.isActive = true
        """)
    List<ServiceBayJpaEntity> findActiveByDealershipIdWithLock(
        @Param("dealershipId") Long dealershipId);
}
