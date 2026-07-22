package com.medha.employeeservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medha.employeeservice.domain.Department;
import com.medha.employeeservice.domain.EmployeeStatus;
import com.medha.employeeservice.dto.request.EmployeeRequest;
import com.medha.employeeservice.integration.AbstractIntegrationTest;
import com.medha.employeeservice.repository.DepartmentRepository;
import com.medha.employeeservice.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test: real Spring context, real MySQL (Testcontainers), MockMvc drives
 * the HTTP layer exactly as a client would. Verifies the CRUD flow, uniqueness
 * constraints and error-handling contract end to end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Long departmentId;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
        departmentRepository.deleteAll();

        Department department = new Department();
        department.setName("Engineering");
        department.setCode("ENG");
        department.setDescription("Builds the product");
        departmentId = departmentRepository.save(department).getId();
    }

    @Test
    void createGetUpdateAndDeleteEmployee_endToEnd() throws Exception {
        EmployeeRequest createRequest = new EmployeeRequest(
                "EMP-100", "Ada", "Lovelace", "ada@example.com", "1234567890",
                LocalDate.of(1990, 1, 1), LocalDate.of(2022, 1, 1), "Engineer",
                new BigDecimal("95000.00"), EmployeeStatus.ACTIVE, null, departmentId, null, Set.of()
        );

        String createResponseJson = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode", is("EMP-100")))
                .andExpect(jsonPath("$.department.code", is("ENG")))
                .andReturn().getResponse().getContentAsString();

        Long employeeId = objectMapper.readTree(createResponseJson).get("id").asLong();

        mockMvc.perform(get("/api/employees/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("ada@example.com")));

        EmployeeRequest updateRequest = new EmployeeRequest(
                "EMP-100", "Ada", "Lovelace-Byron", "ada@example.com", "1234567890",
                LocalDate.of(1990, 1, 1), LocalDate.of(2022, 1, 1), "Senior Engineer",
                new BigDecimal("105000.00"), EmployeeStatus.ACTIVE, null, departmentId, null, Set.of()
        );

        mockMvc.perform(put("/api/employees/{id}", employeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName", is("Lovelace-Byron")))
                .andExpect(jsonPath("$.jobTitle", is("Senior Engineer")));

        mockMvc.perform(delete("/api/employees/{id}", employeeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/{id}", employeeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEmployee_withDuplicateEmail_returnsConflict() throws Exception {
        EmployeeRequest first = new EmployeeRequest(
                "EMP-200", "Grace", "Hopper", "grace@example.com", null,
                null, LocalDate.of(2020, 1, 1), "Rear Admiral",
                new BigDecimal("120000.00"), EmployeeStatus.ACTIVE, null, departmentId, null, Set.of()
        );
        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        EmployeeRequest duplicateEmail = new EmployeeRequest(
                "EMP-201", "Grace", "Hopper-Duplicate", "grace@example.com", null,
                null, LocalDate.of(2020, 1, 1), "Rear Admiral",
                new BigDecimal("120000.00"), EmployeeStatus.ACTIVE, null, departmentId, null, Set.of()
        );

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateEmail)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    void createEmployee_withInvalidPayload_returnsBadRequestWithFieldErrors() throws Exception {
        String invalidJson = """
                {
                  "employeeCode": "",
                  "firstName": "",
                  "lastName": "Doe",
                  "email": "not-an-email",
                  "hireDate": "2022-01-01",
                  "salary": -5,
                  "status": "ACTIVE",
                  "departmentId": %d
                }
                """.formatted(departmentId);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void getEmployee_notFound_returnsErrorContract() throws Exception {
        mockMvc.perform(get("/api/employees/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.path", is("/api/employees/999999")));
    }
}
