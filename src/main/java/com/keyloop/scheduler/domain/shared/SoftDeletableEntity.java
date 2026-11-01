package com.keyloop.scheduler.domain.shared;

import java.time.Clock;
import java.time.LocalDateTime;

public abstract class SoftDeletableEntity<TId> extends AuditableEntity<TId>
        implements SoftDeletable {

    private boolean deleted = false;
    private LocalDateTime deletedAt;
    private String deletedBy;

    @Override public boolean isDeleted() { return deleted; }
    @Override public LocalDateTime getDeletedAt() { return deletedAt; }
    @Override public String getDeletedBy() { return deletedBy; }

    @Override
    public void softDelete(String deletedBy) {
        if (this.deleted) throw new IllegalStateException("Entity is already deleted");
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    public void softDelete(String deletedBy, Clock clock) {
        if (this.deleted) throw new IllegalStateException("Entity is already deleted");
        this.deleted = true;
        this.deletedAt = LocalDateTime.now(clock);
        this.deletedBy = deletedBy;
    }

    @Override
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
