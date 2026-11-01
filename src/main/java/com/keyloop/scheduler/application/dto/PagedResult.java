package com.keyloop.scheduler.application.dto;

import java.util.List;

public record PagedResult<T>(
    List<T> items,
    long totalCount,
    int pageIndex,
    int pageSize,
    int totalPages,
    boolean hasPrevious,
    boolean hasNext
) {
    public PagedResult(List<T> items, long totalCount, int pageIndex, int pageSize) {
        this(items, totalCount, pageIndex, pageSize,
            (int) Math.ceil((double) totalCount / pageSize),
            pageIndex > 0,
            (long) (pageIndex + 1) * pageSize < totalCount);
    }
}
