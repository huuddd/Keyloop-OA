package com.keyloop.scheduler.domain.dealership;

import com.keyloop.scheduler.domain.servicetype.Specialization;
import java.util.List;

public interface TechnicianRepository {
    List<Technician> findByDealershipIdAndSpecialization(
        DealershipId dealershipId, Specialization specialization);
}
