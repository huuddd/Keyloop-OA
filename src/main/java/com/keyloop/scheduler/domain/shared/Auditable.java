package com.keyloop.scheduler.domain.shared;

import java.time.LocalDateTime;

public interface Auditable {
    LocalDateTime getCreatedAt();
    String getCreatedBy();
    LocalDateTime getUpdatedAt();
    String getUpdatedBy();
}
