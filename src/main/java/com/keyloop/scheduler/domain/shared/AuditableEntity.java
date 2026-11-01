package com.keyloop.scheduler.domain.shared;

import java.time.LocalDateTime;

public abstract class AuditableEntity<TId> extends Entity<TId> implements Auditable {

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Override public LocalDateTime getCreatedAt() { return createdAt; }
    @Override public String getCreatedBy() { return createdBy; }
    @Override public LocalDateTime getUpdatedAt() { return updatedAt; }
    @Override public String getUpdatedBy() { return updatedBy; }

    protected void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    protected void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    protected void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    protected void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
