package com.medha.employeeservice.service;

import com.medha.employeeservice.dto.request.DepartmentRequest;
import com.medha.employeeservice.dto.response.DepartmentResponse;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface DepartmentService {

    DepartmentResponse create(DepartmentRequest request);

    DepartmentResponse getById(Long id);

    PageResponse<DepartmentResponse> getAll(Pageable pageable);

    DepartmentResponse update(Long id, DepartmentRequest request);

    void delete(Long id);

    PageResponse<EmployeeSummaryResponse> getEmployees(Long departmentId, Pageable pageable);
}
