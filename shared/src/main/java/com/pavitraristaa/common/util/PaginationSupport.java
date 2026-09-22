package com.pavitraristaa.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PaginationSupport {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private PaginationSupport() {
    }

    public static Pageable pageable(Integer page, Integer size) {
        int resolvedPage = page == null || page < 0 ? 0 : page;
        int resolvedSize = size == null ? DEFAULT_SIZE : size;
        if (resolvedSize < 1) {
            resolvedSize = DEFAULT_SIZE;
        }
        if (resolvedSize > MAX_SIZE) {
            resolvedSize = MAX_SIZE;
        }
        return PageRequest.of(resolvedPage, resolvedSize);
    }
}
