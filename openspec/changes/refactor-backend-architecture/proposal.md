# Change: Refactor Backend Architecture for Consistency and Correctness

## Why
The current backend architecture has several inconsistencies and violations of the intended layering patterns:
1. `DatabaseConfig` only scans `org.example.repository` instead of all repository packages, risking unregistered repositories
2. `ApplicationService` directly depends on both `DomainService` AND `Repository`, breaking the layering principle
3. `UserRepository` queries conflict with the single-directional association design pattern
4. No caching is implemented despite `@EnableCaching` configuration
5. DTO classes lack comprehensive validation annotations

These issues reduce maintainability, increase coupling, and waste configured features.

## What Changes
- Fix `DatabaseConfig` repository scanning to recursively scan `org.example` 
- Refactor `ApplicationService` implementations to depend only on `DomainService`, not directly on `Repository`
- Correct `Repository` query methods that assume bidirectional associations
- Add `@Cacheable` annotations to frequently-accessed read operations
- Add Bean Validation annotations (`@NotNull`, `@Email`, etc.) to DTO classes
- Document transaction management strategy and isolation levels

## Impact
- **Affected specs**: 
  - `backend-architecture` (modified to clarify layering rules)
  - `user-management` (new spec defining user-related requirements)
  - `data-access-layer` (new spec defining Repository patterns)
- **Affected code**: 
  - `src/main/kotlin/org/example/config/DatabaseConfig.kt`
  - All `*ApplicationService.kt` files across modules (User, Route, Trip, etc.)
  - All `*Repository.kt` interfaces
  - All DTO classes in `*.dto` packages

## Breaking Changes
- None. These are purely internal refactorings.

## Rollout
1. Phase 1: Fix repository scanning and ApplicationService dependencies
2. Phase 2: Correct Repository queries to match single-directional design
3. Phase 3: Add caching and DTO validation
4. Phase 4: Test and validate all changes
