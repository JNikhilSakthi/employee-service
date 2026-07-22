package com.medha.employeeservice.dto.response;

/** Lightweight employee projection used to avoid infinite/deep nesting (manager, direct reports, project members). */
public record EmployeeSummaryResponse(
        Long id,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        String jobTitle
) {
}
