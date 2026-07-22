package com.medha.employeeservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotBlank(message = "code is required")
        @Size(max = 30, message = "code must be at most 30 characters")
        String code,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        LocalDate endDate
) {
    @AssertTrue(message = "endDate must not be before startDate")
    public boolean isDateRangeValid() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}
