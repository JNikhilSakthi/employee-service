package com.medha.employeeservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** Thin, stable JSON wrapper around Spring Data's {@link Page} so the API contract
 *  does not leak Spring-specific page metadata field names. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
