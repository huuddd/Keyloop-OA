package com.keyloop.scheduler.infrastructure.persistence.repository;

import com.keyloop.scheduler.domain.dealership.DealershipId;
import com.keyloop.scheduler.domain.dealership.Technician;
import com.keyloop.scheduler.domain.dealership.TechnicianId;
import com.keyloop.scheduler.domain.dealership.TechnicianRepository;
import com.keyloop.scheduler.domain.servicetype.Specialization;
import com.keyloop.scheduler.infrastructure.persistence.entity.TechnicianJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTechnicianRepository implements TechnicianRepository {

    private final SpringDataTechnicianRepository springRepo;

    public JpaTechnicianRepository(SpringDataTechnicianRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public List<Technician> findByDealershipIdAndSpecialization(
            DealershipId dealershipId, Specialization specialization) {
        return springRepo.findByDealershipIdAndSpecialization(
                dealershipId.value(), specialization).stream()
            .map(TechnicianJpaEntity::toDomain).toList();
    }
}
