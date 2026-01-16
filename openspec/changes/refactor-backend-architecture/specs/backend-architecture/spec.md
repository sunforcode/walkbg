# Backend Architecture Delta - Refactor for Consistency

## MODIFIED Requirements

### Requirement: Layered Architecture
The backend service SHALL follow a four-layer architecture pattern with strict dependency direction:
1. Controller Layer - HTTP interface, request validation, response formatting
2. ApplicationService Layer - Business use case orchestration, DTO conversion, cross-domain coordination
3. DomainService Layer - Core business rules, domain logic, validation
4. Repository Layer - Data access abstraction

ApplicationService SHALL ONLY depend on DomainService, never directly on Repository.

#### Scenario: Request processing flow
- **WHEN** an HTTP request arrives at the Controller
- **THEN** it SHALL be routed through ApplicationService → DomainService → Repository in strict sequence
- **AND** responses SHALL be wrapped in unified `ApiResponse<T>` format
- **AND** DomainService implementations MAY directly use Repository for data access

#### Scenario: ApplicationService composition
- **WHEN** a business use case requires multiple operations
- **THEN** ApplicationService SHALL orchestrate DomainService calls, not Repository calls
- **AND** Repository calls SHALL be encapsulated within DomainService only

### Requirement: Dependency Injection Configuration
Spring SHALL be configured to automatically discover and register all Repository beans across the entire `org.example` package tree.

#### Scenario: Multi-module repository discovery
- **WHEN** the application starts
- **THEN** `DatabaseConfig` SHALL scan `org.example` recursively for Repository implementations
- **AND** all modules (User, Route, Trip, Equipment, etc.) repositories SHALL be auto-wired without explicit configuration

## ADDED Requirements

### Requirement: Repository Query Design
Repository queries SHALL respect single-directional entity associations and SHALL NOT assume bidirectional relationships.

#### Scenario: Cross-entity counting without references
- **WHEN** counting related entities (e.g., user's favorite routes)
- **THEN** the query SHALL use JOIN on foreign keys, not traverse entity object references
- **AND** the query SHALL function even if the entity has no collection property for the relationship

#### Scenario: Query performance optimization
- **WHEN** a repository method retrieves aggregate data
- **THEN** it SHALL use appropriate JOIN strategies and FETCH hints
- **AND** N+1 queries SHALL be avoided through proper query design

### Requirement: Caching Strategy
Frequently-accessed read operations SHALL use Spring Cache annotations to improve performance.

#### Scenario: Cached user lookup
- **WHEN** `getUserById()` or `getUserByUsername()` is called
- **THEN** the result SHALL be cached with appropriate TTL
- **AND** the cache SHALL be invalidated on create or update operations

#### Scenario: Cache invalidation
- **WHEN** a user record is updated or created
- **THEN** all related caches for that user SHALL be cleared
- **AND** subsequent queries SHALL retrieve fresh data from the database

### Requirement: DTO Validation
DTO classes SHALL include comprehensive Bean Validation annotations for input validation.

#### Scenario: Email validation in UserCreateRequest
- **WHEN** a user creation request is received
- **THEN** the email field SHALL be validated using `@Email` annotation
- **AND** the username SHALL be validated for length using `@Size` annotation
- **AND** all required fields SHALL be marked with `@NotNull` or `@NotBlank`

#### Scenario: Validation error reporting
- **WHEN** a DTO fails validation
- **THEN** the `GlobalExceptionHandler` SHALL catch the violation
- **AND** the response SHALL include field-level error details
