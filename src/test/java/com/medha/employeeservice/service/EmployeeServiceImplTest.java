package com.medha.employeeservice.service;

import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.domain.Employee;
import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.domain.Project;
import com.medha.employeeservice.dto.request.EmployeeRequest;
import com.medha.employeeservice.dto.response.EmployeeResponse;
import com.medha.employeeservice.exception.DuplicateResourceException;
import com.medha.employeeservice.exception.InvalidOperationException;
import com.medha.employeeservice.exception.ResourceNotFoundException;
import com.medha.employeeservice.repository.DepartmentRepository;
import com.medha.employeeservice.repository.EmployeeRepository;
import com.medha.employeeservice.repository.ProjectRepository;
import com.medha.employeeservice.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Department engineering;
    private EmployeeRequest request;

    @BeforeEach
    void setUp() {
        engineering = new Department();
        engineering.setId(1L);
        engineering.setName("Engineering");
        engineering.setCode("ENG");

        request = new EmployeeRequest(
                "EMP-100", "Ada", "Lovelace", "ada@example.com", "1234567890",
                LocalDate.of(1990, 1, 1), LocalDate.of(2022, 1, 1), "Engineer",
                new BigDecimal("95000.00"), EmployeeStatus.ACTIVE, null, 1L, null, Set.of()
        );
    }

    @Test
    void create_savesEmployee_whenCodeAndEmailAreUnique() {
        when(employeeRepository.existsByEmployeeCode("EMP-100")).thenReturn(false);
        when(employeeRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee toSave = invocation.getArgument(0);
            toSave.setId(10L);
            return toSave;
        });

        EmployeeResponse response = employeeService.create(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.employeeCode()).isEqualTo("EMP-100");
        assertThat(response.department().id()).isEqualTo(1L);
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void create_throwsDuplicate_whenEmployeeCodeAlreadyExists() {
        when(employeeRepository.existsByEmployeeCode("EMP-100")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("EMP-100");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFound_whenDepartmentDoesNotExist() {
        when(employeeRepository.existsByEmployeeCode("EMP-100")).thenReturn(false);
        when(employeeRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenEmployeeMissing() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsInvalidOperation_whenEmployeeManagesOthers() {
        Employee manager = new Employee();
        manager.setId(5L);
        manager.setEmployeeCode("EMP-005");

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(manager));
        when(employeeRepository.existsByManagerId(5L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.delete(5L))
                .isInstanceOf(InvalidOperationException.class);

        verify(employeeRepository, never()).delete(any());
    }

    @Test
    void assignProject_addsProjectAndSaves_whenNotAlreadyAssigned() {
        Employee employee = new Employee();
        employee.setId(2L);
        employee.setEmployeeCode("EMP-002");
        employee.setProjects(new java.util.HashSet<>());

        Project project = new Project();
        project.setId(7L);
        project.setCode("PRJ-1");

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.assignProject(2L, 7L);

        assertThat(response.projects()).extracting("id").containsExactly(7L);
    }

    @Test
    void assignProject_throwsDuplicate_whenAlreadyAssigned() {
        Employee employee = new Employee();
        employee.setId(2L);
        employee.setEmployeeCode("EMP-002");

        Project project = new Project();
        project.setId(7L);
        project.setCode("PRJ-1");
        employee.setProjects(new java.util.HashSet<>(Set.of(project)));

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> employeeService.assignProject(2L, 7L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(employeeRepository, never()).save(any());
    }
}
