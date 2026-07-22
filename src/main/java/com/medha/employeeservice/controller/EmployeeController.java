package com.medha.employeeservice.controller;

import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.dto.request.EmployeeRequest;
import com.medha.employeeservice.dto.request.EmployeeStatusUpdateRequest;
import com.medha.employeeservice.dto.response.EmployeeResponse;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import com.medha.employeeservice.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "CRUD and relationship-management operations for employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @GetMapping
    public PageResponse<EmployeeResponse> search(@RequestParam(required = false) Long departmentId,
                                                  @RequestParam(required = false) EmployeeStatus status,
                                                  @RequestParam(required = false) String q,
                                                  @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return employeeService.search(departmentId, status, q, pageable);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public EmployeeResponse updateStatus(@PathVariable Long id, @Valid @RequestBody EmployeeStatusUpdateRequest request) {
        return employeeService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/projects/{projectId}")
    public EmployeeResponse assignProject(@PathVariable Long id, @PathVariable Long projectId) {
        return employeeService.assignProject(id, projectId);
    }

    @DeleteMapping("/{id}/projects/{projectId}")
    public EmployeeResponse removeProject(@PathVariable Long id, @PathVariable Long projectId) {
        return employeeService.removeProject(id, projectId);
    }

    @GetMapping("/{id}/direct-reports")
    public PageResponse<EmployeeSummaryResponse> getDirectReports(@PathVariable Long id,
                                                                   @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return employeeService.getDirectReports(id, pageable);
    }
}
