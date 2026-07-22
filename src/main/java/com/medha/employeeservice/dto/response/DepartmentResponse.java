package com.medha.employeeservice.dto.response;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        String description,
        long employeeCount,
        Instant createdAt,
        Instant updatedAt
) {
}
