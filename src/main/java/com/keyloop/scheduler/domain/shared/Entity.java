package com.keyloop.scheduler.domain.shared;

import java.util.Objects;

public abstract class Entity<TId> {

    public abstract TId getId();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return getId() != null && getId().equals(entity.getId());
    }

    @Override
    public int hashCode() {
        return getId() != null ? Objects.hash(getId()) : super.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + getId() + "]";
    }
}
