package com.medha.employeeservice.mapper;

import com.medha.employeeservice.domain.Address;
import com.medha.employeeservice.domain.Employee;
import com.medha.employeeservice.dto.AddressDto;
import com.medha.employeeservice.dto.response.EmployeeResponse;
import com.medha.employeeservice.dto.response.EmployeeSummaryResponse;
import com.medha.employeeservice.dto.response.ProjectSummaryResponse;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static EmployeeResponse toResponse(Employee employee) {
        Set<ProjectSummaryResponse> projectSummaries = employee.getProjects().stream()
                .map(ProjectMapper::toSummary)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getDateOfBirth(),
                employee.getHireDate(),
                employee.getJobTitle(),
                employee.getSalary(),
                employee.getStatus(),
                toAddressDto(employee.getAddress()),
                employee.getDepartment() == null ? null : DepartmentMapper.toSummary(employee.getDepartment()),
                employee.getManager() == null ? null : toSummary(employee.getManager()),
                projectSummaries,
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }

    public static EmployeeSummaryResponse toSummary(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getJobTitle()
        );
    }

    public static Address toAddress(AddressDto dto) {
        if (dto == null) {
            return null;
        }
        return Address.builder()
                .street(dto.street())
                .city(dto.city())
                .state(dto.state())
                .postalCode(dto.postalCode())
                .country(dto.country())
                .build();
    }

    private static AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressDto(address.getStreet(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry());
    }
}
