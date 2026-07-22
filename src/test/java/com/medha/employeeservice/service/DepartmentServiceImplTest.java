package com.medha.employeeservice.service;

import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.dto.request.DepartmentRequest;
import com.medha.employeeservice.dto.response.DepartmentResponse;
import com.medha.employeeservice.exception.DuplicateResourceException;
import com.medha.employeeservice.exception.InvalidOperationException;
import com.medha.employeeservice.exception.ResourceNotFoundException;
import com.medha.employeeservice.repository.DepartmentRepository;
import com.medha.employeeservice.repository.EmployeeRepository;
import com.medha.employeeservice.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    @Test
    void create_savesDepartment_whenCodeIsUnique() {
        DepartmentRequest request = new DepartmentRequest("Engineering", "ENG", "Builds the product");
        when(departmentRepository.existsByCode("ENG")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> {
            Department d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        DepartmentResponse response = departmentService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("ENG");
        assertThat(response.employeeCount()).isZero();
    }

    @Test
    void create_throwsDuplicate_whenCodeAlreadyExists() {
        DepartmentRequest request = new DepartmentRequest("Engineering", "ENG", "Builds the product");
        when(departmentRepository.existsByCode("ENG")).thenReturn(true);

        assertThatThrownBy(() -> departmentService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(departmentRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        when(departmentRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_throwsInvalidOperation_whenDepartmentHasEmployees() {
        Department department = new Department();
        department.setId(3L);
        department.setCode("ENG");

        when(departmentRepository.findById(3L)).thenReturn(Optional.of(department));
        when(employeeRepository.countByDepartmentId(3L)).thenReturn(2L);

        assertThatThrownBy(() -> departmentService.delete(3L))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void delete_removesDepartment_whenEmpty() {
        Department department = new Department();
        department.setId(4L);
        department.setCode("SAL");

        when(departmentRepository.findById(4L)).thenReturn(Optional.of(department));
        when(employeeRepository.countByDepartmentId(4L)).thenReturn(0L);

        departmentService.delete(4L);
        // no exception -> deletion proceeded
    }
}
