package com.medha.employeeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Employee Management System.
 *
 * This service is the "star of the show" for MySQL + JPA/Hibernate: it demonstrates
 * one-to-many (Department -&gt; Employee), many-to-one (Employee -&gt; Department),
 * a self-referencing association (Employee -&gt; manager / direct reports),
 * a many-to-many association (Employee &lt;-&gt; Project), an @Embeddable value object
 * (Address), JPA auditing (created/updated timestamps) and the Specification API for
 * dynamic search.
 */
@SpringBootApplication
@EnableJpaAuditing
public class EmployeeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
