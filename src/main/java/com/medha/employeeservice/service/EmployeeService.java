package com.medha.employeeservice.service;

import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.dto.request.EmployeeRequest;
import com.medha.employeeservice.dto.response.EmployeeResponse;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {

    EmployeeResponse create(EmployeeRequest request);

    EmployeeResponse getById(Long id);

    PageResponse<EmployeeResponse> search(Long departmentId, EmployeeStatus status, String query, Pageable pageable);

    EmployeeResponse update(Long id, EmployeeRequest request);

    EmployeeResponse updateStatus(Long id, EmployeeStatus status);

    void delete(Long id);

    EmployeeResponse assignProject(Long employeeId, Long projectId);

    EmployeeResponse removeProject(Long employeeId, Long projectId);

    PageResponse<EmployeeSummaryResponse> getDirectReports(Long managerId, Pageable pageable);
}
