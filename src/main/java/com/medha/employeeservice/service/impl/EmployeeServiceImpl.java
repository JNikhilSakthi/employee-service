package com.medha.employeeservice.service.impl;

import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.domain.Employee;
import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.domain.Project;
import com.medha.employeeservice.dto.request.EmployeeRequest;
import com.medha.employeeservice.dto.response.EmployeeResponse;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.PageResponse;
import com.medha.employeeservice.exception.DuplicateResourceException;
import com.medha.employeeservice.exception.InvalidOperationException;
import com.medha.employeeservice.exception.ResourceNotFoundException;
import com.medha.employeeservice.mapper.EmployeeMapper;
import com.medha.employeeservice.repository.DepartmentRepository;
import com.medha.employeeservice.repository.EmployeeRepository;
import com.medha.employeeservice.repository.EmployeeSpecifications;
import com.medha.employeeservice.repository.ProjectRepository;
import com.medha.employeeservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;

    @Override
    public EmployeeResponse create(EmployeeRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new DuplicateResourceException("Employee", "employeeCode", request.employeeCode());
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Employee", "email", request.email());
        }

        Employee employee = new Employee();
        applyRequest(employee, request, null);

        Employee saved = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return EmployeeMapper.toResponse(findEmployeeOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(Long departmentId, EmployeeStatus status, String query, Pageable pageable) {
        Specification<Employee> spec = Specification
                .where(EmployeeSpecifications.hasDepartment(departmentId))
                .and(EmployeeSpecifications.hasStatus(status))
                .and(EmployeeSpecifications.nameOrEmailContains(query));

        return PageResponse.from(employeeRepository.findAll(spec, pageable).map(EmployeeMapper::toResponse));
    }

    @Override
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findEmployeeOrThrow(id);

        if (employeeRepository.existsByEmployeeCodeAndIdNot(request.employeeCode(), id)) {
            throw new DuplicateResourceException("Employee", "employeeCode", request.employeeCode());
        }
        if (employeeRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateResourceException("Employee", "email", request.email());
        }

        applyRequest(employee, request, id);

        Employee saved = employeeRepository.save(employee);
        return EmployeeMapper.toResponse(saved);
    }

    @Override
    public EmployeeResponse updateStatus(Long id, EmployeeStatus status) {
        Employee employee = findEmployeeOrThrow(id);
        employee.setStatus(status);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    public void delete(Long id) {
        Employee employee = findEmployeeOrThrow(id);

        if (employeeRepository.existsByManagerId(id)) {
            throw new InvalidOperationException(
                    "Employee '%s' cannot be deleted while they are the manager of other employees."
                            .formatted(employee.getEmployeeCode()));
        }

        employeeRepository.delete(employee);
    }

    @Override
    public EmployeeResponse assignProject(Long employeeId, Long projectId) {
        Employee employee = findEmployeeOrThrow(employeeId);
        Project project = findProjectOrThrow(projectId);

        boolean alreadyAssigned = employee.getProjects().stream()
                .anyMatch(p -> p.getId().equals(projectId));
        if (alreadyAssigned) {
            throw new DuplicateResourceException(
                    "Employee '%s' is already assigned to project '%s'.".formatted(employee.getEmployeeCode(), project.getCode()));
        }

        employee.assignProject(project);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    public EmployeeResponse removeProject(Long employeeId, Long projectId) {
        Employee employee = findEmployeeOrThrow(employeeId);
        Project project = findProjectOrThrow(projectId);

        boolean assigned = employee.getProjects().stream()
                .anyMatch(p -> p.getId().equals(projectId));
        if (!assigned) {
            throw new ResourceNotFoundException(
                    "Employee '%s' is not assigned to project '%s'.".formatted(employee.getEmployeeCode(), project.getCode()));
        }

        employee.removeProject(project);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeSummaryResponse> getDirectReports(Long managerId, Pageable pageable) {
        findEmployeeOrThrow(managerId);
        return PageResponse.from(employeeRepository.findByManagerId(managerId, pageable)
                .map(EmployeeMapper::toSummary));
    }

    private void applyRequest(Employee employee, EmployeeRequest request, Long currentEmployeeId) {
        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.departmentId()));

        Employee manager = null;
        if (request.managerId() != null) {
            if (currentEmployeeId != null && request.managerId().equals(currentEmployeeId)) {
                throw new IllegalArgumentException("An employee cannot be their own manager.");
            }
            manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.managerId()));
        }

        employee.setEmployeeCode(request.employeeCode());
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setDateOfBirth(request.dateOfBirth());
        employee.setHireDate(request.hireDate());
        employee.setJobTitle(request.jobTitle());
        employee.setSalary(request.salary());
        employee.setStatus(request.status());
        employee.setAddress(EmployeeMapper.toAddress(request.address()));
        employee.setDepartment(department);
        employee.setManager(manager);

        Set<Long> projectIds = request.projectIds() == null ? Set.of() : request.projectIds();
        Set<Project> projects = new HashSet<>();
        for (Long projectId : projectIds) {
            projects.add(projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId)));
        }
        employee.setProjects(projects);
    }

    private Employee findEmployeeOrThrow(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }
}
