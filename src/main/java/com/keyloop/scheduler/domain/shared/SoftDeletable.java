package com.keyloop.scheduler.domain.shared;

import java.time.LocalDateTime;

public interface SoftDeletable {
    boolean isDeleted();
    LocalDateTime getDeletedAt();
    String getDeletedBy();
    void softDelete(String deletedBy);
    void restore();
}
