package com.medha.employeeservice.repository;

import com.medha.employeeservice.domain.Address;
import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.domain.Employee;
import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises derived queries, {@code @EntityGraph} fetch plans and the Specification API
 * against a real MySQL container (see {@link AbstractIntegrationTest}) rather than an
 * in-memory database, so the SQL Hibernate generates is guaranteed MySQL-compatible.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EmployeeRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Department engineering;
    private Department sales;

    @BeforeEach
    void setUp() {
        engineering = departmentRepository.save(department("Engineering", "ENG"));
        sales = departmentRepository.save(department("Sales", "SAL"));

        employeeRepository.save(employee("EMP-001", "Ada", "Lovelace", "ada@example.com", engineering, EmployeeStatus.ACTIVE));
        employeeRepository.save(employee("EMP-002", "Alan", "Turing", "alan@example.com", engineering, EmployeeStatus.ON_LEAVE));
        employeeRepository.save(employee("EMP-003", "Grace", "Hopper", "grace@example.com", sales, EmployeeStatus.ACTIVE));
    }

    @Test
    void findByDepartmentId_returnsOnlyEmployeesInThatDepartment() {
        Page<Employee> page = employeeRepository.findByDepartmentId(engineering.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Employee::getEmployeeCode)
                .containsExactlyInAnyOrder("EMP-001", "EMP-002");
    }

    @Test
    void countByDepartmentId_reflectsCurrentAssignments() {
        assertThat(employeeRepository.countByDepartmentId(engineering.getId())).isEqualTo(2);
        assertThat(employeeRepository.countByDepartmentId(sales.getId())).isEqualTo(1);
    }

    @Test
    void specification_combinesDepartmentStatusAndNameFilters() {
        Specification<Employee> spec = Specification
                .where(EmployeeSpecifications.hasDepartment(engineering.getId()))
                .and(EmployeeSpecifications.hasStatus(EmployeeStatus.ACTIVE));

        Page<Employee> page = employeeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmployeeCode()).isEqualTo("EMP-001");
    }

    @Test
    void specification_nameOrEmailContains_isCaseInsensitive() {
        Specification<Employee> spec = EmployeeSpecifications.nameOrEmailContains("TURING");

        Page<Employee> page = employeeRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmployeeCode()).isEqualTo("EMP-002");
    }

    @Test
    void existsByEmployeeCode_isTrueOnlyForKnownCodes() {
        assertThat(employeeRepository.existsByEmployeeCode("EMP-001")).isTrue();
        assertThat(employeeRepository.existsByEmployeeCode("EMP-999")).isFalse();
    }

    private Department department(String name, String code) {
        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setDescription(name + " department");
        return department;
    }

    private Employee employee(String code, String firstName, String lastName, String email,
                               Department department, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setHireDate(LocalDate.of(2021, 1, 1));
        employee.setSalary(new BigDecimal("75000.00"));
        employee.setStatus(status);
        employee.setDepartment(department);
        employee.setAddress(Address.builder().city("Bengaluru").country("India").build());
        return employee;
    }
}
