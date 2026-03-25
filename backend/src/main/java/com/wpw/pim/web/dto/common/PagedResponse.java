package com.wpw.pim.web.dto.common;

import java.util.List;

public record PagedResponse<T>(
    List<T> items,
    long total,
    int page,
    int perPage,
    int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> items, long total, int page, int perPage) {
        int totalPages = perPage > 0 ? (int) Math.ceil((double) total / perPage) : 0;
        return new PagedResponse<>(items, total, page, perPage, totalPages);
    }
}
