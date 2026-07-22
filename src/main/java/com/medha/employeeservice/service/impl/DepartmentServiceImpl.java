package com.medha.employeeservice.service.impl;

import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.dto.request.DepartmentRequest;
import com.medha.employeeservice.dto.response.DepartmentResponse;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import com.medha.employeeservice.exception.DuplicateResourceException;
import com.medha.employeeservice.exception.InvalidOperationException;
import com.medha.employeeservice.exception.ResourceNotFoundException;
import com.medha.employeeservice.mapper.DepartmentMapper;
import com.medha.employeeservice.mapper.EmployeeMapper;
import com.medha.employeeservice.repository.DepartmentRepository;
import com.medha.employeeservice.repository.EmployeeRepository;
import com.medha.employeeservice.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Department", "code", request.code());
        }

        Department department = new Department();
        department.setName(request.name());
        department.setCode(request.code());
        department.setDescription(request.description());

        Department saved = departmentRepository.save(department);
        return DepartmentMapper.toResponse(saved, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department department = findDepartmentOrThrow(id);
        long employeeCount = employeeRepository.countByDepartmentId(id);
        return DepartmentMapper.toResponse(department, employeeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> getAll(Pageable pageable) {
        return PageResponse.from(departmentRepository.findAll(pageable)
                .map(d -> DepartmentMapper.toResponse(d, employeeRepository.countByDepartmentId(d.getId()))));
    }

    @Override
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findDepartmentOrThrow(id);

        if (departmentRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException("Department", "code", request.code());
        }

        department.setName(request.name());
        department.setCode(request.code());
        department.setDescription(request.description());

        Department saved = departmentRepository.save(department);
        long employeeCount = employeeRepository.countByDepartmentId(id);
        return DepartmentMapper.toResponse(saved, employeeCount);
    }

    @Override
    public void delete(Long id) {
        Department department = findDepartmentOrThrow(id);

        if (employeeRepository.countByDepartmentId(id) > 0) {
            throw new InvalidOperationException(
                    "Department '%s' cannot be deleted while it still has employees assigned to it."
                            .formatted(department.getCode()));
        }

        departmentRepository.delete(department);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> getEmployees(Long departmentId, Pageable pageable) {
        findDepartmentOrThrow(departmentId);
        return PageResponse.from(employeeRepository.findByDepartmentId(departmentId, pageable)
                .map(EmployeeMapper::toSummary));
    }

    private Department findDepartmentOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }
}
