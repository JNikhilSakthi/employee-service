package com.medha.employeeservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String name,
        String code,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        long memberCount,
        Instant createdAt,
        Instant updatedAt
) {
}
