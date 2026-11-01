package com.keyloop.scheduler.domain.dealership;

import java.util.List;

public interface ServiceBayRepository {
    List<ServiceBay> findActiveByDealershipId(DealershipId dealershipId);

    /**
     * Same as {@link #findActiveByDealershipId} but acquires a pessimistic write
     * lock (SELECT ... FOR UPDATE) on the bay rows, serializing concurrent
     * appointment creation for the same dealership.
     */
    List<ServiceBay> findActiveByDealershipIdWithLock(DealershipId dealershipId);
}
