package com.medha.employeeservice.mapper;

import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.dto.response.DepartmentResponse;
import com.medha.employeeservice.dto.response.DepartmentSummaryResponse;

public final class DepartmentMapper {

    private DepartmentMapper() {
    }

    public static DepartmentResponse toResponse(Department department, long employeeCount) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getDescription(),
                employeeCount,
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }

    public static DepartmentSummaryResponse toSummary(Department department) {
        return new DepartmentSummaryResponse(department.getId(), department.getName(), department.getCode());
    }
}
