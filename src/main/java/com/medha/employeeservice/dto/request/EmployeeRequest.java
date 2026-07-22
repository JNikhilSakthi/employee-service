package com.medha.employeeservice.dto.request;

import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.dto.AddressDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record EmployeeRequest(

        @NotBlank(message = "employeeCode is required")
        @Size(max = 20, message = "employeeCode must be at most 20 characters")
        String employeeCode,

        @NotBlank(message = "firstName is required")
        @Size(max = 100, message = "firstName must be at most 100 characters")
        String firstName,

        @NotBlank(message = "lastName is required")
        @Size(max = 100, message = "lastName must be at most 100 characters")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a well-formed email address")
        @Size(max = 150, message = "email must be at most 150 characters")
        String email,

        @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "phone must be a valid phone number")
        String phone,

        @Past(message = "dateOfBirth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "hireDate is required")
        @PastOrPresent(message = "hireDate cannot be in the future")
        LocalDate hireDate,

        @Size(max = 100, message = "jobTitle must be at most 100 characters")
        String jobTitle,

        @NotNull(message = "salary is required")
        @Positive(message = "salary must be positive")
        @Digits(integer = 10, fraction = 2, message = "salary may have at most 10 integer and 2 fractional digits")
        BigDecimal salary,

        @NotNull(message = "status is required")
        EmployeeStatus status,

        @Valid
        AddressDto address,

        @NotNull(message = "departmentId is required")
        Long departmentId,

        Long managerId,

        Set<Long> projectIds
) {
}
