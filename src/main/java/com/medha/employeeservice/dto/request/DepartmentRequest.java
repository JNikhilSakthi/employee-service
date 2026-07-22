package com.medha.employeeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "code is required")
        @Size(max = 20, message = "code must be at most 20 characters")
        String code,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description
) {
}
