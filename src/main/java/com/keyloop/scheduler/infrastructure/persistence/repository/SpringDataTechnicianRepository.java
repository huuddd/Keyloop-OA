package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.infrastructure.persistence.entity.TechnicianJpaEntity;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataTechnicianRepository extends JpaRepository<TechnicianJpaEntity, Long> {

    @Query("""
        SELECT t FROM TechnicianJpaEntity t
        JOIN t.specializations s
        WHERE t.dealershipId = :dealershipId
          AND s = :specialization
        """)
    List<TechnicianJpaEntity> findByDealershipIdAndSpecialization(
        @Param("dealershipId") Long dealershipId,
        @Param("specialization") Specialization specialization);
}
