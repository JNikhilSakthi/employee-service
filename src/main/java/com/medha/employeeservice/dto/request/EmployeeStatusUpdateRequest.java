package com.medha.employeeservice.dto.request;

import com.medha.employeeservice.domain.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record EmployeeStatusUpdateRequest(
        @NotNull(message = "status is required")
        EmployeeStatus status
) {
}
