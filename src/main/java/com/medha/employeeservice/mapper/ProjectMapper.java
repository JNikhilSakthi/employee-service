package com.medha.employeeservice.mapper;

import com.medha.employeeservice.domain.Project;
import com.medha.employeeservice.dto.response.ProjectResponse;
import com.medha.employeeservice.dto.response.ProjectSummaryResponse;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectResponse toResponse(Project project, long memberCount) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getCode(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                memberCount,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public static ProjectSummaryResponse toSummary(Project project) {
        return new ProjectSummaryResponse(project.getId(), project.getName(), project.getCode());
    }
}
