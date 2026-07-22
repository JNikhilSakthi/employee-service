# employee-service

**Employee Management System** — a MySQL + JPA/Hibernate demo built on Spring Boot 4 / Java 25, covering every core relational-mapping style a relational-DB service needs: many-to-one, self-referencing one-to-many, owning many-to-many, embeddables, auditing and dynamic search.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Hibernate](https://img.shields.io/badge/Hibernate-JPA-59666C)
![Testcontainers](https://img.shields.io/badge/Testcontainers-MySQL-1e88e5)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

**Learning Track:** `springboot-jpa-crud-demo` (Project 1 of 17)
**Real-World Service Name:** `employee-service`

---

## 1. Project Overview

Every company that isn't purely event-driven still has a system of record sitting on a boring relational database, and that system of record almost always looks like an HR / org-chart / catalog problem: entities that belong to other entities, entities that reference each other in a hierarchy, and entities that share a many-to-many relationship through a join table. `employee-service` is a deliberately "textbook" domain — employees, departments, projects — chosen specifically so every JPA relationship style can be demonstrated in one cohesive, realistic API instead of a dozen disconnected toy examples.

**Problem it solves:** manage a company's employees, the departments they belong to, the projects they're staffed on, and the manager/direct-report hierarchy between them, with correct relational integrity (you cannot delete a department that still has employees, you cannot delete a manager who still has reports, an employee cannot manage themselves) and correctly-shaped read APIs (no runaway JSON graphs, no N+1 queries on the hot paths).

**Why MySQL / JPA / Hibernate:** this is still the default backend stack for the vast majority of business-CRUD services in industry — anything from an internal HR tool to an e-commerce catalog to a SaaS multi-tenant admin panel. Learning to map inheritance-free entity graphs (`@ManyToOne`, self-referencing FKs, owning vs. inverse `@ManyToMany`, `@Embeddable`), to avoid N+1 via `@EntityGraph`, and to build dynamic filters with the `Specification` API is the single highest-leverage skill for this class of service.

**Where this pattern shows up in real companies:** HRIS/HRMS systems (Workday, BambooHR-style org charts), CRM contact/account hierarchies, e-commerce product/catalog/category graphs, project-management tools (Jira-style issue↔sprint↔project many-to-many), and any admin back-office with "assign X to Y" semantics.

---

## 2. Architecture

### High-Level Design (HLD)

```
                                   ┌─────────────────────────┐
                                   │        Clients          │
                                   │ (curl / Postman / UI /  │
                                   │  Swagger UI)             │
                                   └────────────┬─────────────┘
                                                │ HTTP/JSON
                                                ▼
                                   ┌─────────────────────────┐
                                   │   employee-service       │
                                   │   (Spring Boot 4, :8080) │
                                   │                          │
                                   │  Controller → Service    │
                                   │  → Repository → Entity   │
                                   └────────────┬─────────────┘
                                                │ JDBC (Hikari pool)
                                                ▼
                                   ┌─────────────────────────┐
                                   │        MySQL 8.0         │
                                   │   (employee_db, :3306)   │
                                   │  departments / employees │
                                   │  / projects /            │
                                   │  employee_projects       │
                                   └─────────────────────────┘
```

A single Spring Boot module talking directly to one MySQL instance — there is no cross-service messaging, no gateway, no separate auth service in this project (that's deliberately out of scope; this repo is about the JPA/Hibernate mapping layer).

### Low-Level Design (LLD) — request flow for a write endpoint

```
HTTP request
   │
   ▼
@RestController (EmployeeController / DepartmentController / ProjectController)
   │  - binds JSON to a request record
   │  - @Valid triggers Jakarta Bean Validation
   ▼
Service interface (EmployeeService / DepartmentService / ProjectService)
   │  - @Transactional business method
   ▼
ServiceImpl (EmployeeServiceImpl / DepartmentServiceImpl / ProjectServiceImpl)
   │  - loads related entities (Department, Manager, Projects) via repositories
   │  - enforces business rules (uniqueness, non-empty-before-delete, no self-management)
   │  - throws ResourceNotFoundException / DuplicateResourceException / InvalidOperationException
   ▼
Repository (Spring Data JPA - JpaRepository / JpaSpecificationExecutor)
   │  - derived queries, @EntityGraph fetch plans, Specification-based dynamic search
   ▼
Hibernate → MySQL 8
   │
   ▼
Entity graph mapped back to a *Response record by a hand-written Mapper (no reflection/MapStruct)
   │
   ▼
@RestControllerAdvice (GlobalExceptionHandler) intercepts any thrown exception
   and converts it to a consistent ErrorResponse JSON shape if one occurred
   │
   ▼
JSON response
```

### Entity-relationship design

```
 ┌───────────────┐        1        many ┌───────────────┐
 │  Department   │───────────────────────│   Employee    │
 │ id, name,     │  department_id FK     │ id, code,     │
 │ code,         │  (owning: Employee)   │ name, email,  │
 │ description   │                        │ salary,       │
 └───────────────┘                        │ status,       │
                                           │ address (embedded) │
                                           │                │
                     ┌─────────────────────┤ manager_id FK  │◄──┐  self-reference
                     │  manager (many-to-one)              │   │  (manager / directReports)
                     │                                       │───┘
                     ▼
                (another Employee row)

 ┌───────────────┐   employee_projects  ┌───────────────┐
 │   Employee    │◄────────join table──►│    Project    │
 │ (owning side  │  employee_id,        │ (inverse/     │
 │  of the M:M)  │  project_id          │  mappedBy side)│
 └───────────────┘                       └───────────────┘
```

- **Department (1) ↔ (many) Employee** — standard many-to-one/one-to-many, FK `employee.department_id`, `Department.employees` is the read-only inverse (`mappedBy`) side.
- **Employee ↔ Employee (self-referencing)** — `manager_id` FK on `employees` points back at `employees.id`; `Employee.manager` is the owning many-to-one, `Employee.directReports` is the inverse one-to-many.
- **Employee ↔ Project (many-to-many)** — join table `employee_projects(employee_id, project_id)` with a unique constraint on the pair; `Employee.projects` is the owning side, `Project.employees` is `mappedBy`.
- **Address** — an `@Embeddable` inlined directly into the `employees` table (`address_street`, `address_city`, `address_state`, `address_postal_code`, `address_country` columns) — no separate table, no join.
- **BaseEntity** — `@MappedSuperclass` (not its own table) supplying `id` (`IDENTITY` generation), `createdAt`/`updatedAt` populated by Spring Data JPA auditing (`@EnableJpaAuditing` + `AuditingEntityListener`), and id-based `equals`/`hashCode`.

### Folder structure

```
employee-service/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── .gitignore
├── src/main/resources/
│   └── application.yml
└── src/main/java/com/medha/employeeservice/
    ├── EmployeeServiceApplication.java
    ├── config/
    │   └── OpenApiConfig.java
    ├── domain/                 # JPA entities + enums + embeddable
    │   ├── BaseEntity.java
    │   ├── Address.java
    │   ├── Department.java
    │   ├── Employee.java
    │   ├── Project.java
    │   └── EmployeeStatus.java
    ├── dto/
    │   ├── AddressDto.java
    │   ├── request/            # Java records with Bean Validation
    │   └── response/           # Java records, incl. *SummaryResponse + PageResponse
    ├── mapper/                 # hand-written entity <-> DTO mappers
    ├── repository/             # Spring Data JPA repositories + Specifications
    ├── service/                # interfaces
    │   └── impl/                # implementations (@Transactional business logic)
    ├── controller/              # @RestController classes
    └── exception/                # custom exceptions + GlobalExceptionHandler
└── src/test/java/com/medha/employeeservice/
    ├── integration/AbstractIntegrationTest.java   # shared Testcontainers MySQL base
    ├── repository/EmployeeRepositoryTest.java      # @DataJpaTest
    ├── service/*.java                               # Mockito unit tests
    └── controller/EmployeeControllerIntegrationTest.java  # @SpringBootTest + MockMvc
```

---

## 3. Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 25 | Records for DTOs, modern language features |
| Framework | Spring Boot 4.0.6 | Auto-configuration, embedded Tomcat, actuator |
| Persistence | Spring Data JPA + Hibernate | ORM, repository abstraction, `Specification` API |
| Database | MySQL 8.0 | Real-world relational engine, run via Docker for local dev |
| Connection pool | HikariCP (Spring Boot default) | Fast, production-grade pooling |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) | Declarative request validation, incl. custom `@AssertTrue` |
| API docs | springdoc-openapi (Swagger UI) | Interactive API exploration at `/swagger-ui.html` |
| Boilerplate reduction | Lombok | Getters/setters/constructors on entities |
| Observability | Spring Boot Actuator | `/actuator/health` for Docker healthcheck |
| Testing | JUnit 5, Mockito, AssertJ | Unit tests for service layer |
| Integration testing | Testcontainers (MySQL module) | Real MySQL in `@DataJpaTest` / `@SpringBootTest`, no H2 substitute |
| Containerization | Docker multi-stage build + docker-compose | App + MySQL 8 wired together with a healthcheck gate |

---

## 4. Configuration Explained (`src/main/resources/application.yml`)

| Property | Value | Why |
|---|---|---|
| `server.port` | `${SERVER_PORT:8080}` | Overridable per environment; defaults to 8080 for local/demo |
| `spring.application.name` | `employee-service` | Identifies the app in logs/actuator/metrics |
| `spring.datasource.url` | `jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:employee_db}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true` | Env-var driven so the same jar works unmodified in Docker Compose (`DB_HOST=mysql`) or bare-metal local (`DB_HOST=localhost`); `createDatabaseIfNotExist=true` removes a manual setup step for a demo project; `useSSL=false` avoids local cert friction; `serverTimezone=UTC` avoids date/time skew bugs |
| `spring.datasource.username` / `password` | `${DB_USERNAME:employee_user}` / `${DB_PASSWORD:employee_pass}` | Matches the non-root user docker-compose provisions in the MySQL container |
| `spring.datasource.driver-class-name` | `com.mysql.cj.jdbc.Driver` | Explicit driver class for the `mysql-connector-j` dependency |
| `spring.datasource.hikari.pool-name` | `employee-service-pool` | Named pool makes HikariCP metrics/logs identifiable |
| `spring.datasource.hikari.maximum-pool-size` | `${DB_POOL_SIZE:10}` | 10 is a sane default for a single-instance demo service; tunable via env var without a rebuild |
| `spring.datasource.hikari.connection-timeout` | `20000` (ms) | Fail fast rather than hang indefinitely if the DB is unreachable |
| `spring.jpa.hibernate.ddl-auto` | `${JPA_DDL_AUTO:update}` | **Deliberate simplification for local/demo friction-free schema sync** — Hibernate keeps the schema in step with the entities automatically. The YAML comment in the file explicitly calls out that a real production service should switch this to `validate` and manage schema changes with an external migration tool (Flyway/Liquibase) instead |
| `spring.jpa.show-sql` | `${JPA_SHOW_SQL:false}` | Off by default to keep logs clean; flip to `true` locally to see generated SQL |
| `spring.jpa.open-in-view` | `false` | Disabled deliberately — prevents the "open session in view" anti-pattern where lazy associations are silently fetched during view/JSON rendering outside the service transaction; forces all needed data to be fetched intentionally (via `@EntityGraph` or explicit mapping) inside the transactional service method |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Readable multi-line SQL in logs when `show-sql` is on |
| `spring.jpa.properties.hibernate.dialect` | `org.hibernate.dialect.MySQLDialect` | Ensures MySQL-specific SQL generation (types, pagination syntax, etc.) |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | `25` | Batches inserts/updates into groups of 25 round-trips instead of one-by-one, cutting DB chattiness |
| `spring.jpa.properties.hibernate.order_inserts` / `order_updates` | `true` | Groups statements by table so the batching above is actually effective |
| `spring.jackson.default-property-inclusion` | `non_null` | Omits null fields from JSON responses (e.g. a manager-less employee has no `manager` key rather than `"manager": null` noise) |
| `spring.jackson.serialization.write-dates-as-timestamps` | `false` | Dates/instants serialize as ISO-8601 strings, not epoch millis — human-readable and JS-`Date`-friendly (this is also Jackson 3's own default under Spring Boot 4, so the property is now a belt-and-braces explicit setting rather than an override) |
| `management.endpoints.web.exposure.include` | `health,info` | Minimal actuator surface exposed — just enough for the Docker healthcheck and basic ops info, no metrics/env dumps in a demo |
| `management.endpoint.health.show-details` | `when-authorized` | Doesn't leak DB connection details to unauthenticated callers |
| `logging.level.com.medha.employeeservice` | `${APP_LOG_LEVEL:INFO}` | App package log level overridable per environment |
| `logging.level.org.hibernate.SQL` | `${SQL_LOG_LEVEL:INFO}` | Separate knob for Hibernate's own SQL logger |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Where the interactive API explorer is served |
| `springdoc.api-docs.path` | `/v3/api-docs` | Raw OpenAPI JSON document location |

---

## 5. Project Structure Explained

| Path | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot 4.0.6 parent, Java 25, web/data-jpa/validation/actuator starters, MySQL driver, springdoc-openapi, Lombok, JUnit 5 + Testcontainers (MySQL module) for tests |
| `.gitignore` / `.dockerignore` | Keep build artifacts (`target/`), IDE files, and logs out of git/image layers |
| `Dockerfile` | Multi-stage build: `maven:3.9-eclipse-temurin-25` compiles the jar, `eclipse-temurin:25-jre-alpine` runs it as a non-root `spring` user; ships a `HEALTHCHECK` that polls `/actuator/health` |
| `docker-compose.yml` | Wires a `mysql:8.0` container (with a `mysqladmin ping` healthcheck and a named volume `employee-mysql-data`) plus the `employee-service` app container, which `depends_on: mysql: condition: service_healthy` so the app never starts against a DB that isn't ready yet |
| `src/main/resources/application.yml` | All runtime configuration — see section 4 |
| `EmployeeServiceApplication.java` | `@SpringBootApplication` + `@EnableJpaAuditing` entry point; the Javadoc on this class enumerates every relationship style demonstrated in the domain model |
| `config/OpenApiConfig.java` | Registers a custom `OpenAPI` bean (title/description/version/contact) used by springdoc to render Swagger UI |
| `domain/BaseEntity.java` | `@MappedSuperclass` with `id`, JPA-audited `createdAt`/`updatedAt`, id-based `equals`/`hashCode`, and an `isNew()` helper |
| `domain/Address.java` | `@Embeddable` value object inlined into the `employees` table |
| `domain/EmployeeStatus.java` | Enum: `ACTIVE`, `ON_LEAVE`, `TERMINATED` |
| `domain/Department.java` | Entity with unique `code`; inverse (`mappedBy`) one-to-many to `Employee` |
| `domain/Project.java` | Entity with unique `code`, `startDate`/`endDate`; inverse (`mappedBy`) many-to-many to `Employee` |
| `domain/Employee.java` | The aggregate root — owning many-to-one to `Department`, self-referencing `manager`/`directReports`, owning many-to-many to `Project` via `employee_projects`, embedded `Address`; carries `assignProject`/`removeProject` helper methods that keep both sides of the many-to-many in sync |
| `dto/AddressDto.java` | Shared request/response record mirroring `Address`, with `@Size` constraints |
| `dto/request/*.java` | `DepartmentRequest`, `ProjectRequest` (with a custom `@AssertTrue isDateRangeValid()` cross-field check), `EmployeeRequest`, `EmployeeStatusUpdateRequest` — all Java records carrying Jakarta Bean Validation annotations |
| `dto/response/*SummaryResponse.java` | Lightweight nested projections (`EmployeeSummaryResponse`, `DepartmentSummaryResponse`, `ProjectSummaryResponse`) used inside other responses instead of full nested entities, to avoid unbounded graph serialization |
| `dto/response/EmployeeResponse.java` / `DepartmentResponse.java` / `ProjectResponse.java` | Full response records; `DepartmentResponse`/`ProjectResponse` carry computed `employeeCount`/`memberCount` fields (never a loaded collection) |
| `dto/response/PageResponse.java` | Stable JSON pagination envelope (`content`, `page`, `size`, `totalElements`, `totalPages`, `last`) that decouples the API contract from Spring Data's `Page` implementation details |
| `mapper/*.java` | Hand-written, dependency-free entity↔DTO mappers (`EmployeeMapper`, `DepartmentMapper`, `ProjectMapper`) — no MapStruct/reflection, so mapping logic is explicit and easy to read |
| `repository/DepartmentRepository.java`, `ProjectRepository.java` | `JpaRepository` + code-uniqueness derived queries (`existsByCode`, `existsByCodeAndIdNot`) |
| `repository/EmployeeRepository.java` | `JpaRepository` + `JpaSpecificationExecutor`; derived queries for uniqueness (`existsByEmployeeCode`, `existsByEmail`), counts (`countByDepartmentId`, `countByProjectsId` — the latter traverses the many-to-many join table), and `@EntityGraph(attributePaths = {"department","manager"})`-annotated paged lookups to avoid N+1 |
| `repository/EmployeeSpecifications.java` | Null-safe composable `Specification<Employee>` predicates (`hasDepartment`, `hasStatus`, `nameOrEmailContains`) used by the dynamic search endpoint |
| `service/*.java` | Interfaces: `EmployeeService`, `DepartmentService`, `ProjectService` |
| `service/impl/*.java` | `@Transactional` implementations enforcing business rules (uniqueness checks, "can't delete non-empty department/project", "can't delete a manager with direct reports", "can't be your own manager", "can't double-assign a project") |
| `controller/*.java` | `@RestController` classes exposing the HTTP API (see section 6) |
| `exception/ResourceNotFoundException.java` | → HTTP 404 |
| `exception/DuplicateResourceException.java` | → HTTP 409 |
| `exception/InvalidOperationException.java` | → HTTP 409 (business-rule conflicts) |
| `exception/ApiFieldError.java` / `ErrorResponse.java` | The consistent error JSON contract (`timestamp`, `status`, `error`, `message`, `path`, `fieldErrors[]`) |
| `exception/GlobalExceptionHandler.java` | Single `@RestControllerAdvice` mapping every exception type above (plus `MethodArgumentNotValidException`, `ConstraintViolationException`, `DataIntegrityViolationException`, and a catch-all `Exception`) to `ErrorResponse` |
| `test/.../integration/AbstractIntegrationTest.java` | Shared Testcontainers MySQL base class (`@Testcontainers` + `@DynamicPropertySource`) reused by both the repository test and the controller test |
| `test/.../repository/EmployeeRepositoryTest.java` | `@DataJpaTest` against real MySQL: derived queries, `@EntityGraph`, and `Specification` composition |
| `test/.../service/*ImplTest.java` | Mockito unit tests for all three service implementations |
| `test/.../controller/EmployeeControllerIntegrationTest.java` | `@SpringBootTest` + `MockMvc` full-stack test: create→get→update→delete flow, duplicate-email conflict, validation-error contract, not-found contract |

---

## 6. Getting Started

### Prerequisites

- Docker + Docker Compose
- (For local non-Docker dev) JDK 25 and Maven 3.9+
- Port `8080` and `3306` free on your machine

### Run everything with Docker Compose

```bash
# from the repository root
docker compose up --build
```

This will:
1. Start a MySQL 8.0 container, wait for its healthcheck (`mysqladmin ping`) to pass.
2. Build the `employee-service` image (multi-stage Maven build → JRE Alpine runtime).
3. Start the app container only once MySQL reports healthy (`depends_on: condition: service_healthy`).

The API is then available at `http://localhost:8080`, and Swagger UI at `http://localhost:8080/swagger-ui.html`.

To stop and remove containers (keeping the named volume/data):
```bash
docker compose down
```

To also wipe the database volume:
```bash
docker compose down -v
```

### Run locally without Docker (against a Docker-only MySQL)

```bash
docker compose up -d mysql
mvn spring-boot:run
```
(The app reads `DB_HOST`/`DB_PORT`/etc. from env vars, defaulting to `localhost:3306` — matching the port docker-compose exposes.)

### Build the jar directly

```bash
mvn clean package
java -jar target/employee-service.jar
```

> **Note:** this codebase was authored and internally consistency-checked (imports, record component order, method signatures across interfaces/impls/tests) without running Maven in the generation environment. Run `mvn clean verify` before first real use to confirm a clean compile and test pass.

---

## 7. API Documentation

Base URL: `http://localhost:8080`. Interactive docs: `/swagger-ui.html` (raw spec at `/v3/api-docs`).

### Departments — `/api/departments`

| Method | Path | Description |
|---|---|---|
| POST | `/api/departments` | Create a department |
| GET | `/api/departments/{id}` | Get one department (includes computed `employeeCount`) |
| GET | `/api/departments` | Paged list (`?page=&size=&sort=`, default sort `name`) |
| PUT | `/api/departments/{id}` | Update a department |
| DELETE | `/api/departments/{id}` | Delete — 409 if it still has employees |
| GET | `/api/departments/{id}/employees` | Paged `EmployeeSummaryResponse` list for that department |

**Create request**
```json
POST /api/departments
{
  "name": "Engineering",
  "code": "ENG",
  "description": "Builds the product"
}
```
**Response — 201**
```json
{
  "id": 1,
  "name": "Engineering",
  "code": "ENG",
  "description": "Builds the product",
  "employeeCount": 0,
  "createdAt": "2026-07-22T09:00:00Z",
  "updatedAt": "2026-07-22T09:00:00Z"
}
```

### Projects — `/api/projects`

| Method | Path | Description |
|---|---|---|
| POST | `/api/projects` | Create a project |
| GET | `/api/projects/{id}` | Get one project (includes computed `memberCount`) |
| GET | `/api/projects` | Paged list (default sort `startDate`) |
| PUT | `/api/projects/{id}` | Update a project |
| DELETE | `/api/projects/{id}` | Delete — 409 if it still has members |
| GET | `/api/projects/{id}/employees` | Paged `EmployeeSummaryResponse` list of members |

**Create request** (note: `endDate` before `startDate` fails validation via a custom `@AssertTrue`)
```json
POST /api/projects
{
  "name": "Platform Migration",
  "code": "PRJ-1",
  "description": "Move to cloud",
  "startDate": "2026-08-01",
  "endDate": "2026-12-31"
}
```

### Employees — `/api/employees`

| Method | Path | Description |
|---|---|---|
| POST | `/api/employees` | Create an employee |
| GET | `/api/employees/{id}` | Get one employee |
| GET | `/api/employees?departmentId=&status=&q=` | Dynamic search via Spring Data JPA `Specification`, all filters optional and composable, paged, default sort `lastName` |
| PUT | `/api/employees/{id}` | Full update |
| PATCH | `/api/employees/{id}/status` | Update just the `status` field |
| DELETE | `/api/employees/{id}` | Delete — 409 if they still manage direct reports |
| POST | `/api/employees/{id}/projects/{projectId}` | Assign the employee to a project — 409 if already assigned |
| DELETE | `/api/employees/{id}/projects/{projectId}` | Remove the employee from a project — 404 if not assigned |
| GET | `/api/employees/{id}/direct-reports` | Paged `EmployeeSummaryResponse` list of this manager's direct reports |

**Create request**
```json
POST /api/employees
{
  "employeeCode": "EMP-100",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "phone": "1234567890",
  "dateOfBirth": "1990-01-01",
  "hireDate": "2022-01-01",
  "jobTitle": "Engineer",
  "salary": 95000.00,
  "status": "ACTIVE",
  "address": { "city": "Bengaluru", "country": "India" },
  "departmentId": 1,
  "managerId": null,
  "projectIds": []
}
```
**Response — 201**
```json
{
  "id": 10,
  "employeeCode": "EMP-100",
  "firstName": "Ada",
  "lastName": "Lovelace",
  "email": "ada@example.com",
  "phone": "1234567890",
  "dateOfBirth": "1990-01-01",
  "hireDate": "2022-01-01",
  "jobTitle": "Engineer",
  "salary": 95000.00,
  "status": "ACTIVE",
  "address": { "city": "Bengaluru", "country": "India" },
  "department": { "id": 1, "name": "Engineering", "code": "ENG" },
  "projects": [],
  "createdAt": "2026-07-22T09:05:00Z",
  "updatedAt": "2026-07-22T09:05:00Z"
}
```
(`manager` is omitted entirely thanks to `default-property-inclusion: non_null` when the employee has no manager.)

**Error contract (any 4xx/5xx)**
```json
{
  "timestamp": "2026-07-22T09:06:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Employee not found with id='999999'",
  "path": "/api/employees/999999",
  "fieldErrors": []
}
```

**Validation error (400)** includes populated `fieldErrors`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields.",
  "fieldErrors": [
    { "field": "email", "message": "email must be a well-formed email address" },
    { "field": "salary", "message": "salary must be positive" }
  ]
}
```

---

## 8. Testing

Run all tests (requires Docker running locally, since Testcontainers spins up real MySQL containers — no H2/in-memory substitute is used anywhere in this project):

```bash
mvn test
```

| Test class | Type | What it covers |
|---|---|---|
| `EmployeeRepositoryTest` | `@DataJpaTest` + Testcontainers MySQL | `findByDepartmentId` + `@EntityGraph`, `countByDepartmentId`, `Specification` composition (department + status, case-insensitive name/email search), `existsByEmployeeCode` |
| `EmployeeServiceImplTest` | JUnit 5 + Mockito (mocked repositories) | create success/duplicate-code/missing-department, get-by-id not-found, delete blocked when employee manages others, assign-project success/duplicate-assignment |
| `DepartmentServiceImplTest` | JUnit 5 + Mockito | create success/duplicate-code, get-by-id not-found, delete blocked/allowed based on employee count |
| `ProjectServiceImplTest` | JUnit 5 + Mockito | create success/duplicate-code, get-by-id not-found, delete blocked when members exist |
| `EmployeeControllerIntegrationTest` | `@SpringBootTest` + `MockMvc` + Testcontainers MySQL | full create→get→update→delete HTTP flow, duplicate-email → 409, invalid payload → 400 with `fieldErrors`, missing employee → 404 with the standard error contract |

All Testcontainers-backed tests share `AbstractIntegrationTest`, which starts one `MySQLContainer` per JVM and wires its JDBC URL/credentials in via `@DynamicPropertySource`.

---

## 9. Docker

**`Dockerfile`** — two stages:
1. **Build stage** (`maven:3.9-eclipse-temurin-25`): copies `pom.xml` first and runs `dependency:go-offline` so dependency downloads are cached in their own layer, then copies `src/` and runs `clean package -DskipTests` to produce `target/employee-service.jar`.
2. **Runtime stage** (`eclipse-temurin:25-jre-alpine`): much smaller final image; creates and switches to a non-root `spring:spring` user before copying the jar in; exposes port 8080; declares a `HEALTHCHECK` that curls `/actuator/health` and greps for `"status":"UP"`.

**`docker-compose.yml`** — two services on a shared bridge network (`employee-network`):
- **`mysql`** — `mysql:8.0` image, environment-driven root password/db name/user/password (with sane defaults), a named volume `employee-mysql-data` for persistence across restarts, and a `mysqladmin ping` healthcheck (10s interval, 10 retries, 30s start period) that gates the app's startup.
- **`employee-service`** — built from the local `Dockerfile`, `depends_on: mysql: condition: service_healthy` (won't start until MySQL is actually accepting connections, not just "container running"), env vars for DB host/port/name/credentials plus `JPA_DDL_AUTO` and `JPA_SHOW_SQL` overrides, port `8080` published to the host.

---

## 10. Interview Preparation

**Q: What's the difference between the owning side and the inverse (`mappedBy`) side of a relationship, and why does it matter?**
The owning side holds the foreign-key column and is the side Hibernate actually writes SQL for when the association changes. Here, `Employee.department`, `Employee.manager`, and `Employee.projects` are all owning sides; `Department.employees`, `Employee.directReports`, and `Project.employees` are `mappedBy` inverse sides that exist for convenient navigation but must never be mutated directly — changes have to go through the owning side (see `Employee.assignProject`/`removeProject`, which update both sides of the in-memory graph while only the owning side actually gets persisted).

**Q: Why is a self-referencing association (manager/directReports) modeled as two `@ManyToOne`/`@OneToMany` mappings on the same entity instead of a separate join table?**
Because it's a single FK column (`manager_id`) on the `employees` table pointing back at `employees.id` — exactly the shape of a plain many-to-one, just where "one" happens to be the same table. A separate join table would only be needed if an employee could have *multiple* managers (a true many-to-many), which isn't the business rule here.

**Q: Why is `Address` an `@Embeddable` instead of its own entity/table?**
Because it has no identity of its own, no independent lifecycle (it's deleted the instant the employee is), and nothing else references it. Modeling it as a full entity with a `@OneToOne` would add a needless join for data that always travels 1:1 with its parent row. `@Embeddable` inlines the columns directly into `employees`.

**Q: Why do `EmployeeResponse`/`DepartmentResponse`/`ProjectResponse` use `*SummaryResponse` nested records instead of nesting the full response type?**
To bound the JSON graph. If `EmployeeResponse.department` were a full `DepartmentResponse`, and `DepartmentResponse` tried to list employees, you'd either get infinite recursion or an accidental N-employees-deep payload for every single employee fetched. Summaries expose just enough for a client to display/link the related entity, and a dedicated endpoint (`/api/departments/{id}/employees`) is used when the full paged list is actually needed.

**Q: Why are `employeeCount`/`memberCount` computed via `countByDepartmentId`/`countByProjectsId` instead of `department.getEmployees().size()`?**
Calling `.size()` on a lazy collection forces Hibernate to load the *entire* collection into memory just to count it — wasteful and, outside a transaction, an N+1/`LazyInitializationException` risk. A dedicated `SELECT COUNT(*)` derived query is a single, cheap, indexable statement regardless of how many employees/projects exist.

**Q: What does `@EntityGraph(attributePaths = {"department","manager"})` buy you, and where is it *not* applied?**
It tells Hibernate to fetch those two lazy `@ManyToOne` associations via a single JOIN FETCH query instead of firing a separate SELECT per row when the (lazy-loaded) getter is later accessed — the classic N+1 problem. It's applied on the simple paged-lookup methods (`findByDepartmentId`, `findByStatus`, `findByManagerId`, `findByProjectsId`). It is **deliberately not** applied on the `Specification`-based dynamic search path (`employeeRepository.findAll(spec, pageable)`) — combining `@EntityGraph` with `Specification` queries is more delicate in Spring Data, and this project documents that gap as a known simplification rather than silently hiding it.

**Q: Why is `open-in-view` disabled, and what breaks if you leave it on (the Spring Boot default)?**
With OSIV on, the Hibernate session stays open through view/serialization, so a lazy association accessed accidentally during JSON marshalling triggers an extra (often N+1) query *outside* the transactional service method — invisible in code review, expensive in production, and a common source of "why is this endpoint slow" tickets. Disabling it forces every needed association to be fetched deliberately inside the `@Transactional` service method (via `@EntityGraph` or explicit mapping), surfacing fetch decisions instead of hiding them.

**Q: `ddl-auto: update` is used here — is that production-safe?**
No, and the `application.yml` comment says so explicitly. `update` is fine for local/demo iteration because Hibernate keeps the schema in sync automatically, but it can produce surprising, hard-to-review, sometimes destructive DDL in a shared/production database. A real service should run with `ddl-auto: validate` (Hibernate refuses to start if the schema doesn't match the entities) and manage all actual schema changes through Flyway or Liquibase migrations that are reviewed and versioned like code.

**Q: Common JPA/Hibernate mistakes this codebase deliberately avoids:**
- Mutating only one side of a bidirectional association (would leave the persistence context and the DB out of sync until a flush/reload) — avoided via `assignProject`/`removeProject` helper methods.
- Using `.size()`/`.isEmpty()` on lazy collections for existence/count checks — avoided via dedicated `countBy...` derived queries.
- Serializing full entity graphs directly (`@JsonIgnore` is used defensively on entities, but the DTO/mapper layer is the real boundary — entities are never returned from controllers).
- Letting bean-validation and business-rule failures leak framework-specific exception shapes to clients — centralized into one `ErrorResponse` contract via `GlobalExceptionHandler`.
- Skipping database-level uniqueness (`@UniqueConstraint`) and relying only on service-layer checks — both are present here, and `DataIntegrityViolationException` is caught as a 409 fallback for races the service-layer check missed.

**Q: Performance notes / production considerations for scaling this further:**
- Add composite indexes to match real query patterns once traffic profiling is available (e.g. `(department_id, status)` if that filter combination dominates).
- Consider read replicas once `GET` traffic dominates writes; nothing in this design precludes routing reads elsewhere.
- The `Specification` search path currently lacks the `@EntityGraph` optimization the simple paged lookups have — worth fixing with a `Specification`-aware fetch-join or a projection DTO query before this pattern goes to production scale.
- `hibernate.jdbc.batch_size=25` + `order_inserts`/`order_updates` reduce round-trips for bulk writes; if bulk-loading became a real workload, consider `StatelessSession` or JDBC batch inserts outside the entity manager entirely.
- Pagination is applied everywhere lists are returned (`PageResponse`) — never an unbounded `findAll()` on a growing table.

---

## 11. License

MIT — see [LICENSE](./LICENSE).
