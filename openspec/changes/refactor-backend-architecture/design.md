# Design Document: Backend Architecture Refactoring

## Context

The WalkBG backend service has grown to multiple modules (User, Route, Trip, Equipment, Meal, Water) with varying adherence to the intended layered architecture. Several architectural inconsistencies have emerged:

1. **Configuration Issues**: `DatabaseConfig` only scans `org.example.repository` instead of all repository packages recursively
2. **Layering Violations**: `ApplicationService` classes directly depend on `Repository` in addition to `DomainService`
3. **Entity Design Inconsistencies**: Repository queries assume bidirectional associations that don't exist in entities
4. **Unused Features**: `@EnableCaching` is configured but not implemented
5. **Validation Gaps**: DTOs lack comprehensive Bean Validation annotations

These inconsistencies create maintenance burden and technical debt. This refactoring addresses them systematically.

## Goals

- **Primary**: Enforce consistent layering across all modules (DomainService ↔ Repository, ApplicationService → DomainService only)
- **Secondary**: Fix repository configuration to auto-discover all modules
- **Tertiary**: Enable caching for read operations
- **Quaternary**: Add comprehensive DTO validation

## Non-Goals

- Changing API contracts or external behavior
- Rewriting existing business logic (only restructuring it)
- Changing database schema
- Adding new features

## Decisions

### Decision 1: ApplicationService → DomainService Only Pattern
**What**: ApplicationService shall only depend on DomainService, not Repository.

**Why**: 
- Maintains clear separation of concerns
- ApplicationService orchestrates use cases, DomainService enforces business rules
- Prevents bypassing validation logic by directly calling Repository
- Easier to test and mock

**Alternatives Considered**:
- Allow ApplicationService to call Repository directly: Simpler initially but violates layering and creates duplication
- Remove DomainService layer: Loses validation layer, couples business logic to data access
- Make ApplicationService pure orchestration: Chosen approach

**Implementation**:
- All ApplicationService methods SHALL compose DomainService methods
- ApplicationService SHALL NOT have `@Autowired Repository` fields
- DomainService implementations SHALL be the sole consumers of Repository

### Decision 2: Repository Configuration - Recursive Scanning
**What**: Change `DatabaseConfig` to scan `org.example` recursively instead of only `org.example.repository`.

**Why**:
- Current scanning misses repositories in subpackages (user.repository, route.repository, etc.)
- Recursive scanning auto-discovers new modules without config changes
- Follows Spring best practice for multi-module applications

**Alternatives Considered**:
- Explicitly list each repository package: Breaks new module scaling
- Use separate config per module: Creates fragmentation and duplication
- Scan root package recursively: Chosen approach

**Current Code**:
```kotlin
@EnableJpaRepositories(basePackages = ["org.example.repository"])
```

**Fixed Code**:
```kotlin
@EnableJpaRepositories(basePackages = ["org.example"])
```

### Decision 3: Single-Directional Association Query Pattern
**What**: All Repository queries SHALL use JOIN syntax, not traverse entity object references.

**Why**:
- Entities are designed without bidirectional references to prevent N+1 queries
- Queries must be explicit about relationships
- Supports Aggregate Root pattern

**Implementation Example**:
- Current (broken): `SELECT u FROM User u WHERE size(u.favoriteRoutes) > :count`
- Fixed: `SELECT u FROM User u LEFT JOIN UserFavoriteRoute ufr ON u.id = ufr.userId GROUP BY u.id HAVING COUNT(ufr) > :count`

### Decision 4: Caching Strategy
**What**: Add `@Cacheable` to frequently-accessed read operations with appropriate invalidation.

**Why**:
- `@EnableCaching` is already configured but unused
- Read operations like `getUserById()`, `getUserByUsername()` can benefit significantly
- Cache invalidation on update/delete ensures data consistency

**Caching Policy**:
- `@Cacheable` for: `getUserById()`, `getUserByUsername()`, `searchUsers()` (by keyword)
- `@CacheEvict` for: `createUser()`, `updateUser()`, `deleteUser()`
- Cache name: `users`, TTL: 5 minutes (via Spring Cache manager)

**Alternatives Considered**:
- No caching: Leaves performance on table
- Redis instead of Caffeine: Overkill for current scale
- Caffeine in-memory cache: Chosen - good for single-instance deployment

### Decision 5: DTO Validation Annotations
**What**: Add Bean Validation annotations to all DTO classes.

**Why**:
- Declarative validation is more maintainable than procedural checks
- Generates better error responses via `GlobalExceptionHandler`
- Follows Spring best practices

**Fields to Validate**:
- Username: `@NotBlank`, `@Size(min=3, max=50)`
- Email: `@NotBlank`, `@Email`
- Nickname: `@NotBlank`, `@Size(max=50)`
- Optional fields: `@Size` only when provided

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Breaking existing code during refactoring | High | Test all changes thoroughly; deploy in phases |
| Repository queries using removed associations | High | Audit all queries before fixing; add tests |
| Cache invalidation bugs | Medium | Implement careful cache key strategy; test edge cases |
| Performance regression from deeper call chains | Low | Monitor metrics; DomainService overhead is minimal |

## Migration Plan

### Phase 1: Configuration & Dependency Fixes (Low Risk)
1. Update `DatabaseConfig` to scan `org.example` recursively
2. Run tests to verify all repositories are discovered
3. Deploy to test environment

### Phase 2: ApplicationService Refactoring (Medium Risk)
1. Identify all ApplicationService → Repository direct calls
2. Move logic to corresponding DomainService
3. Update ApplicationService to call DomainService only
4. Add comprehensive unit tests
5. Deploy to test environment

### Phase 3: Repository Query Corrections (High Risk)
1. Audit all `@Query` methods that use entity references
2. Rewrite queries to use JOIN syntax
3. Add integration tests for each corrected query
4. Deploy to test environment
5. Monitor production for errors

### Phase 4: Caching & Validation (Low Risk)
1. Add `@Cacheable` and `@CacheEvict` annotations
2. Add Bean Validation annotations to DTOs
3. Test cache invalidation scenarios
4. Deploy to test environment

### Rollback Plan
- Each phase can be independently rolled back via git revert
- Tests should catch issues before production
- Monitoring alerts on database query performance

## Open Questions

1. **Cache TTL**: Should we make cache TTL configurable via properties?
2. **Partial Cache Invalidation**: Should updating a user evict only that user's cache or all user caches?
3. **Multi-Instance Deployment**: How should caching work if deployed to multiple instances? (Current Caffeine is in-memory only)
