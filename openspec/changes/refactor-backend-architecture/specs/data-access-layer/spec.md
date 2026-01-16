# Data Access Layer Specification - New Capability

## ADDED Requirements

### Requirement: Repository Configuration
Spring Data repositories SHALL be automatically discovered and configured across all modules.

#### Scenario: Application startup
- **WHEN** the Spring Boot application starts
- **THEN** all repositories in `org.example` package tree SHALL be scanned and registered
- **AND** repositories in subpackages (user.repository, route.repository, etc.) SHALL all be discovered
- **AND** no manual @Bean configuration SHALL be required for repositories

### Requirement: Single-Directional Association Query Pattern
Repository queries SHALL be designed to work with single-directional entity associations.

#### Scenario: User favorite routes count
- **WHEN** `userRepository.countUserFavoriteRoutes(userId)` is called
- **THEN** the query SHALL perform a JOIN on the `UserFavoriteRoute` table
- **AND** it SHALL NOT attempt to access a `user.favoriteRoutes` collection
- **AND** the User entity SHALL not define a bidirectional reference to `UserFavoriteRoute`

#### Scenario: Route waypoints retrieval
- **WHEN** retrieving a route's waypoints
- **THEN** the query SHALL use `waypointRepository.findByRouteId(routeId)` instead of traversing `route.waypoints`
- **AND** this pattern SHALL apply to all one-to-many relationships

### Requirement: Query Method Naming and Documentation
Repository query methods SHALL follow consistent naming and include clear documentation.

#### Scenario: Semantic method names
- **WHEN** a repository query is written
- **THEN** the method name SHALL be semantic: `countUserCreatedRoutes()`, not `getCount()`
- **AND** the method SHALL include `@Query` annotation with explicit JPQL/SQL for clarity
- **AND** JavaDoc SHALL document the entity relationships assumed by the query

### Requirement: Custom Query Optimization
Complex repository queries SHALL use `@Query` annotations to ensure optimal SQL execution.

#### Scenario: Multi-criteria search
- **WHEN** searching with multiple filters (keyword, status, region, etc.)
- **THEN** a `@Query` annotation SHALL define the precise JPQL/SQL
- **AND** the query SHALL include appropriate indexes to avoid full table scans
- **AND** pagination SHALL be applied at the query level

#### Scenario: Aggregate queries
- **WHEN** calculating statistics or aggregates
- **THEN** the query SHALL use COUNT, SUM, or GROUP BY at the SQL level
- **AND** the result SHALL be mapped to a DTO or scalar value, not a full entity

### Requirement: Transaction Propagation
Repository operations SHALL respect transaction boundaries defined at the Service layer.

#### Scenario: Operation within transaction
- **WHEN** a repository method is called from a `@Transactional` service
- **THEN** the operation SHALL be part of the same transaction by default
- **AND** `@Transactional(readOnly=true)` SHALL be used for query-only operations
- **AND** no new transaction SHALL be created for read operations
