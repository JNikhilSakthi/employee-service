package com.medha.employeeservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object: its columns are inlined into the owning entity's table
 * (employees) rather than living in a table of their own. No identity of its own.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {

    @Column(name = "address_street", length = 150)
    private String street;

    @Column(name = "address_city", length = 100)
    private String city;

    @Column(name = "address_state", length = 100)
    private String state;

    @Column(name = "address_postal_code", length = 20)
    private String postalCode;

    @Column(name = "address_country", length = 100)
    private String country;
}
