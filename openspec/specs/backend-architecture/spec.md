# Backend Architecture Specification

## ADDED Requirements

### Requirement: Layered Architecture
The backend service SHALL follow a four-layer architecture pattern:
1. Controller Layer - HTTP interface, request validation, response formatting
2. ApplicationService Layer - Business use case orchestration, DTO conversion, cross-domain coordination
3. DomainService Layer - Core business rules, domain logic, validation
4. Repository Layer - Data access abstraction

#### Scenario: Request processing flow
- **WHEN** an HTTP request arrives at the Controller
- **THEN** it SHALL be routed through ApplicationService → DomainService → Repository in sequence
- **AND** responses SHALL be wrapped in unified `ApiResponse<T>` format

### Requirement: Single-Directional Entity Associations
Domain entities SHALL NOT hold bidirectional or collection references to avoid N+1 queries and circular dependencies.

#### Scenario: Accessing related entities
- **WHEN** a service needs related entity data
- **THEN** it SHALL query the appropriate Repository instead of traversing entity references
- **EXAMPLE** User stats are calculated via `UserRepository.countUserCreatedRoutes()` not `user.routes.size()`

### Requirement: Exception Handling
All exceptions SHALL be caught at the global exception handler level and return standardized error responses.

#### Scenario: Business rule violation
- **WHEN** a business exception occurs
- **THEN** the system SHALL return an `ApiResponse` with error code, HTTP status, and trace ID
- **AND** the error details SHALL NOT expose sensitive information

### Requirement: Transaction Management
Business operations SHALL define transaction boundaries at the Service layer using `@Transactional` annotations.

#### Scenario: Create operation success
- **WHEN** a create request is processed
- **THEN** the entire operation SHALL be atomic within a single transaction
- **AND** rollback SHALL occur if any validation or persistence fails

#### Scenario: Read operation performance
- **WHEN** a read-only query is executed
- **THEN** it SHALL use `@Transactional(readOnly = true)` to optimize performance
