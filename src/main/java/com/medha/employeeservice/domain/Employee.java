package com.medha.employeeservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregate root of the domain. Demonstrates several JPA relationship styles at once:
 * <ul>
 *   <li>many-to-one to {@link Department} (owning side, {@code department_id} FK)</li>
 *   <li>a self-referencing many-to-one/one-to-many for the manager / direct-reports
 *       hierarchy ({@code manager_id} FK back onto the same table)</li>
 *   <li>an owning many-to-many to {@link Project} via the {@code employee_projects}
 *       join table</li>
 *   <li>an {@code @Embedded} value object ({@link Address})</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"department", "manager", "directReports", "projects"})
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "employees", uniqueConstraints = {
        @UniqueConstraint(name = "uk_employee_code", columnNames = "employee_code"),
        @UniqueConstraint(name = "uk_employee_email", columnNames = "email")
})
public class Employee extends BaseEntity {

    @Column(name = "employee_code", nullable = false, length = 20)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    @Embedded
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_employee_department"))
    private Department department;

    /** Self-referencing many-to-one: the employee's manager (nullable - top of the org chart has none). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", foreignKey = @ForeignKey(name = "fk_employee_manager"))
    private Employee manager;

    /** Inverse side of the self-reference above. */
    @JsonIgnore
    @OneToMany(mappedBy = "manager", fetch = FetchType.LAZY)
    private List<Employee> directReports = new ArrayList<>();

    /** Owning side of the employee &lt;-&gt; project many-to-many association. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "employee_projects",
            joinColumns = @JoinColumn(name = "employee_id", foreignKey = @ForeignKey(name = "fk_employee_projects_employee")),
            inverseJoinColumns = @JoinColumn(name = "project_id", foreignKey = @ForeignKey(name = "fk_employee_projects_project")),
            uniqueConstraints = @UniqueConstraint(name = "uk_employee_project", columnNames = {"employee_id", "project_id"})
    )
    private Set<Project> projects = new LinkedHashSet<>();

    public void assignProject(Project project) {
        this.projects.add(project);
        project.getEmployees().add(this);
    }

    public void removeProject(Project project) {
        this.projects.remove(project);
        project.getEmployees().remove(this);
    }
}
