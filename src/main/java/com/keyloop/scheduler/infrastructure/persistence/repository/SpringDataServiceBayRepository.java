package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.ServiceBayJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataServiceBayRepository extends JpaRepository<ServiceBayJpaEntity, Long> {

    List<ServiceBayJpaEntity> findByDealershipIdAndIsActiveTrue(Long dealershipId);
}
