# Implementation Tasks: Backend Architecture Refactoring

## Phase 1: Configuration & Dependency Discovery (1-2 hours)

### 1.1 Update DatabaseConfig Repository Scanning
- [ ] Open `src/main/kotlin/org/example/config/DatabaseConfig.kt`
- [ ] Change `@EnableJpaRepositories(basePackages = ["org.example.repository"])` to `@EnableJpaRepositories(basePackages = ["org.example"])`
- [ ] Change `@EntityScan(basePackages = ["org.example.model"])` to `@EntityScan(basePackages = ["org.example"])`
- [ ] Verify formatting matches project conventions
- [ ] Run `mvn clean compile` to verify no compilation errors

### 1.2 Add Configuration for Caching
- [ ] Verify `@EnableCaching` is present in `DatabaseConfig` (should already be)
- [ ] Create `src/main/resources/application-cache.yml` with Caffeine cache configuration (if not present)
- [ ] Add cache manager bean with default TTL (5 minutes) if needed

### 1.3 Test Configuration Changes
- [ ] Run `mvn test` to ensure all tests pass
- [ ] Verify Spring application starts without repository discovery errors
- [ ] Check that all repositories across modules are properly registered

---

## Phase 2: ApplicationService Refactoring (4-6 hours)

### 2.1 Refactor UserApplicationService
- [ ] Open `src/main/kotlin/org/example/user/service/UserApplicationService.kt`
- [ ] Remove direct `@Autowired UserRepository` dependency
- [ ] Verify all Repository calls are moved to UserService layer
- [ ] Update `getUserStats()` to call `UserService` for all count operations
- [ ] Remove `countUserCreatedRoutes()`, etc. calls to userRepository
- [ ] Run `mvn test` for user module

### 2.2 Refactor RouteApplicationService
- [ ] Open `src/main/kotlin/org/example/route/service/RouteApplicationService.kt`
- [ ] Remove direct Repository dependencies except what's needed for DTO conversion
- [ ] Move all business logic calls to RouteService (DomainService)
- [ ] Run `mvn test` for route module

### 2.3 Refactor EquipmentApplicationService
- [ ] Open `src/main/kotlin/org/example/equipment/service/EquipmentApplicationService.kt`
- [ ] Remove direct Repository dependencies
- [ ] Move business logic to EquipmentService
- [ ] Run `mvn test` for equipment module

### 2.4 Refactor TripApplicationService
- [ ] Open `src/main/kotlin/org/example/trip/service/TripApplicationService.kt`
- [ ] Remove direct Repository dependencies
- [ ] Move business logic to TripService
- [ ] Run `mvn test` for trip module

### 2.5 Refactor MealApplicationService
- [ ] Similar refactoring for meal module
- [ ] Run tests

### 2.6 Add Service Layer Tests
- [ ] Create unit tests verifying ApplicationService only calls DomainService
- [ ] Create integration tests for use case flows
- [ ] Ensure all tests pass

---

## Phase 3: Repository Query Corrections (6-8 hours)

### 3.1 Fix UserRepository Queries
- [ ] Open `src/main/kotlin/org/example/user/repository/UserRepository.kt`
- [ ] Review `countUserFavoriteRoutes()` query (line 112-115)
  - [ ] Check if it assumes `ufr.user` relationship
  - [ ] Rewrite to use proper JOIN without assuming backward reference
  - [ ] Example fix: `SELECT COUNT(ufr) FROM UserFavoriteRoute ufr WHERE ufr.userId = :userId`
- [ ] Review `countUserTripParticipations()` query
  - [ ] Verify it doesn't assume bidirectional relationships
  - [ ] Fix if needed
- [ ] Test queries with sample data

### 3.2 Fix RouteRepository Queries
- [ ] Open route repository files in `src/main/kotlin/org/example/route/repository/`
- [ ] Audit all `@Query` methods for bidirectional assumptions
- [ ] Rewrite problematic queries
- [ ] Add comments explaining the single-directional relationship design

### 3.3 Fix EquipmentRepository Queries
- [ ] Audit equipment repository queries
- [ ] Fix any queries that assume collections in entities

### 3.4 Fix TripRepository Queries
- [ ] Audit trip repository queries
- [ ] Fix any queries that assume collections in entities

### 3.5 Add Query Integration Tests
- [ ] Create integration tests for each corrected query
- [ ] Verify queries return correct results
- [ ] Test with realistic data sets
- [ ] Run full test suite

---

## Phase 4: Caching Implementation (2-3 hours)

### 4.1 Add @Cacheable to UserService
- [ ] Open `src/main/kotlin/org/example/user/service/UserServiceImpl.kt`
- [ ] Add `@Cacheable(value = "users", key = "#userId")` to `getUserById()`
- [ ] Add `@Cacheable(value = "users", key = "#username")` to `getUserByUsername()`
- [ ] Consider caching `searchUsers()` with key = keyword + page

### 4.2 Add @CacheEvict to User Mutation Operations
- [ ] Add `@CacheEvict(value = "users", allEntries = true)` to `createUserWithValidation()`
- [ ] Add `@CacheEvict(value = "users", key = "#userId")` to `updateUser(userId, ...)`
- [ ] Add `@CacheEvict(value = "users", key = "#userId")` to `activateUser()`
- [ ] Add `@CacheEvict(value = "users", key = "#userId")` to `deactivateUser()`

### 4.3 Add Caching to Other Modules
- [ ] Apply same pattern to RouteService read/write operations
- [ ] Apply to EquipmentService
- [ ] Apply to TripService
- [ ] Apply to WaterService
- [ ] Apply to MealService

### 4.4 Test Caching Behavior
- [ ] Verify cache hits improve performance
- [ ] Verify cache eviction on updates works correctly
- [ ] Test cache invalidation scenarios
- [ ] Monitor cache statistics

---

## Phase 5: DTO Validation Annotations (2-3 hours)

### 5.1 Add Validation to UserCreateRequest
- [ ] Open DTO: `src/main/kotlin/org/example/user/dto/UserCreateRequest.kt`
- [ ] Add `@NotBlank` to username
- [ ] Add `@Size(min=3, max=50)` to username
- [ ] Add `@NotBlank` to email
- [ ] Add `@Email` to email
- [ ] Add `@NotBlank` to nickname
- [ ] Add `@Size(max=50)` to nickname
- [ ] Add `@Size(max=500)` to avatarUrl (optional)
- [ ] Add `@Size(max=20)` to phone (optional)
- [ ] Add necessary imports for validation annotations

### 5.2 Add Validation to RouteCreateRequest
- [ ] Similar validation annotations for route DTOs
- [ ] Include size, difficulty, route type validations

### 5.3 Add Validation to Other DTOs
- [ ] Equipment DTOs
- [ ] Trip DTOs
- [ ] Meal DTOs
- [ ] Water DTOs

### 5.4 Test DTO Validation
- [ ] Create tests for valid DTOs
- [ ] Create tests for invalid inputs
- [ ] Verify GlobalExceptionHandler properly catches validation errors
- [ ] Verify error responses include field-level details

---

## Phase 6: Testing & Validation (2-4 hours)

### 6.1 Unit Tests
- [ ] Verify all service layer tests pass
- [ ] Verify all DTO validation tests pass
- [ ] Verify all repository tests pass

### 6.2 Integration Tests
- [ ] Full user management flow test
- [ ] Full route management flow test
- [ ] Full trip management flow test
- [ ] Cross-module interactions

### 6.3 API Tests
- [ ] Test all endpoints with valid inputs
- [ ] Test all endpoints with validation errors
- [ ] Verify error responses follow standard format
- [ ] Verify cache behavior via API

### 6.4 Documentation
- [ ] Update code comments to reflect new patterns
- [ ] Update architecture documentation if needed
- [ ] Add usage examples for caching and validation

### 6.5 Final Validation
- [ ] Run `mvn clean test` to ensure all tests pass
- [ ] Run `mvn clean package` to verify packaged jar
- [ ] Verify application starts successfully
- [ ] Manual smoke testing of key APIs

---

## Success Criteria

- [ ] All phases completed
- [ ] All tests passing (unit, integration, API)
- [ ] ApplicationService depends only on DomainService (no Repository)
- [ ] All repositories auto-discovered by Spring
- [ ] All queries respect single-directional associations
- [ ] Caching implemented and validated
- [ ] DTOs include validation annotations
- [ ] Code review completed and approved
- [ ] No performance regression

## Timeline Estimate
- **Total**: 15-22 hours
- **Parallel work possible**: Limited (phases must be sequential)
- **Risk level**: Medium (high for Phase 3, low for others)
- **Testing time**: ~25% of total effort
