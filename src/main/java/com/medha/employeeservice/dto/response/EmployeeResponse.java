package com.medha.employeeservice.dto.response;

import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.dto.AddressDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        LocalDate hireDate,
        String jobTitle,
        BigDecimal salary,
        EmployeeStatus status,
        AddressDto address,
        DepartmentSummaryResponse department,
        EmployeeSummaryResponse manager,
        Set<ProjectSummaryResponse> projects,
        Instant createdAt,
        Instant updatedAt
) {
}
