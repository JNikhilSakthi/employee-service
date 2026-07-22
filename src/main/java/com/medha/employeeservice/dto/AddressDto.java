package com.medha.employeeservice.dto;

import jakarta.validation.constraints.Size;

/**
 * Shared between request and response payloads - maps 1:1 onto the {@code Address}
 * {@code @Embeddable}. All fields are optional (an employee may not have a home address
 * on file yet).
 */
public record AddressDto(
        @Size(max = 150, message = "street must be at most 150 characters") String street,
        @Size(max = 100, message = "city must be at most 100 characters") String city,
        @Size(max = 100, message = "state must be at most 100 characters") String state,
        @Size(max = 20, message = "postalCode must be at most 20 characters") String postalCode,
        @Size(max = 100, message = "country must be at most 100 characters") String country
) {
}
