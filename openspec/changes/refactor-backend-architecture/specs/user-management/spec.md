# User Management Specification - New Capability

## ADDED Requirements

### Requirement: User Service Layer Separation
User management operations SHALL be divided into two service layers with clear responsibilities:
- `UserService` (DomainService): Business rules, validation, domain logic
- `UserApplicationService`: Use case orchestration, DTO conversion

#### Scenario: User creation with validation
- **WHEN** a user creation request arrives
- **THEN** `UserApplicationService` SHALL call `UserService.createUserWithValidation()`
- **AND** `UserService` SHALL verify business rules (unique username, unique email)
- **AND** only `UserService` SHALL interact with `UserRepository`

#### Scenario: User search operation
- **WHEN** searching for users
- **THEN** `UserApplicationService` SHALL call `UserService.searchUsers()`
- **AND** the result SHALL be paginated and converted to DTO format
- **AND** no Repository calls SHALL occur at ApplicationService level

### Requirement: User Query Performance
User repository queries SHALL optimize data retrieval and respect entity design patterns.

#### Scenario: Efficient relationship counting
- **WHEN** calculating user statistics
- **THEN** counts for routes, trips, favorites SHALL use SQL aggregation, not entity traversal
- **EXAMPLE** `countUserFavoriteRoutes()` SHALL use JOIN on `UserFavoriteRoute` table, not access a non-existent `user.favoriteRoutes` property

#### Scenario: User cache population
- **WHEN** a user is retrieved by ID or username
- **THEN** the result SHALL be cached for subsequent accesses
- **AND** cache TTL SHALL be 5 minutes for standard operations
- **AND** cache invalidation SHALL occur on user update

### Requirement: User Input Validation
User creation and update requests SHALL validate all input fields.

#### Scenario: UserCreateRequest validation
- **WHEN** a user creation request is processed
- **THEN** all fields in `UserCreateRequest` SHALL be validated:
  - username: `@NotBlank`, `@Size(min=3, max=50)`
  - email: `@NotBlank`, `@Email`
  - nickname: `@NotBlank`, `@Size(max=50)`
  - avatarUrl: `@Size(max=500)` (optional)
  - phone: `@Size(max=20)` (optional)

#### Scenario: Duplicate field detection
- **WHEN** creating a user with an existing username or email
- **THEN** the system SHALL return HTTP 409 (Conflict)
- **AND** the error response SHALL indicate which field caused the conflict

### Requirement: User Statistics Query
User statistics SHALL be calculated efficiently without loading entire entity graphs.

#### Scenario: User stats retrieval
- **WHEN** `GET /api/v1/users/{id}/stats` is called
- **THEN** the response SHALL include:
  - routeCount: Routes created by the user
  - tripCount: Trips the user participates in
  - favoriteCount: Routes marked as favorite
  - completedCount: Routes marked as completed
- **AND** all counts SHALL be calculated via SQL queries, not object traversal
