package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.DealershipJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDealershipRepository extends JpaRepository<DealershipJpaEntity, Long> {
}
