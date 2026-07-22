package com.medha.employeeservice.repository;

import com.medha.employeeservice.domain.Employee;
import com.medha.employeeservice.domain.EmployeeStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable {@link Specification} predicates used by the dynamic employee search endpoint
 * ({@code GET /api/employees}). Each factory method returns {@code null} when its filter
 * argument is absent - Spring Data's {@code Specification.where(...).and(...)} composition
 * treats a null predicate as "no restriction", so callers can freely chain every filter
 * regardless of whether it was supplied.
 */
public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> hasDepartment(Long departmentId) {
        return (root, query, cb) -> departmentId == null
                ? null
                : cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employee> hasStatus(EmployeeStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Employee> nameOrEmailContains(String term) {
        return (root, query, cb) -> {
            if (term == null || term.isBlank()) {
                return null;
            }
            String pattern = "%" + term.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }
}
