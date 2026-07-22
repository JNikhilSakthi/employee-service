package com.medha.employeeservice.repository;

import com.medha.employeeservice.domain.Employee;
import com.medha.employeeservice.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    boolean existsByEmployeeCodeAndIdNot(String employeeCode, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByManagerId(Long managerId);

    long countByDepartmentId(Long departmentId);

    /** Derived query traversing the owning many-to-many collection ("projects") to the join table. */
    long countByProjectsId(Long projectId);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findByManagerId(Long managerId, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "manager"})
    Page<Employee> findByProjectsId(Long projectId, Pageable pageable);
}
