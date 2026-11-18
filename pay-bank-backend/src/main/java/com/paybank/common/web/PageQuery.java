package com.paybank.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Shared pagination parameters of the list endpoints (zero-based page, page size and optional
 * search text). The resulting {@link Pageable} is never sorted: each query fixes its own order.
 */
public record PageQuery(int page, int size, String q) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidPageQueryException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPageQueryException("size must be between 1 and " + MAX_SIZE);
        }
    }

    /** Builds a query applying the defaults for missing values. */
    public static PageQuery of(Integer page, Integer size, String q) {
        return new PageQuery(page == null ? DEFAULT_PAGE : page, size == null ? DEFAULT_SIZE : size, q);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
