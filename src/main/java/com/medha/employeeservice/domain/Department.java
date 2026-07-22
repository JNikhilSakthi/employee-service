package com.medha.employeeservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true, exclude = "employees")
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "departments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_code", columnNames = "code")
})
public class Department extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(length = 500)
    private String description;

    /**
     * Inverse side of the Employee -&gt; Department many-to-one association.
     * {@code mappedBy} means the "employees.department_id" foreign key column owns the
     * relationship; this collection is read-only from JPA's persistence point of view -
     * changes must be made through {@link Employee#setDepartment(Department)}.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();
}
