package com.medha.employeeservice.service;

import com.medha.employeeservice.dto.request.ProjectRequest;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import com.medha.employeeservice.dto.response.ProjectResponse;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    ProjectResponse create(ProjectRequest request);

    ProjectResponse getById(Long id);

    PageResponse<ProjectResponse> getAll(Pageable pageable);

    ProjectResponse update(Long id, ProjectRequest request);

    void delete(Long id);

    PageResponse<EmployeeSummaryResponse> getMembers(Long projectId, Pageable pageable);
}
