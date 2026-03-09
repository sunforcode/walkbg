# Tasks: Complete Route Detail API Implementation

## Phase 1: Data Enrichment (RouteDetailResponse Completion)

### Task 1.1: Add Repository Injections to RouteApplicationService
**Objective**: Inject all required repositories for fetching associated data
**Location**: `src/main/kotlin/org/example/route/service/RouteApplicationService.kt`

**Items**:
1. Add DI for SegmentRepository (already injected, verify)
2. Add DI for RouteTagRepository
3. Add DI for CampsiteRepository
4. Add DI for SupplyRepository
5. Add DI for WaterSourceRepository
6. Add DI for MarkerPointRepository
7. Add DI for DailyPlanRepository
8. Add DI for HitchhikeContactRepository
9. Add DI for UserRepository (for creator data)
10. Add DI for MapDataRepository (for route metrics)

**Validation**: All repositories can be resolved by Spring DI container

---

### Task 1.2: Implement Data Assembly in RouteApplicationService
**Objective**: Create a helper method to assemble complete RouteDetailResponse with all associated data
**Location**: `src/main/kotlin/org/example/route/service/RouteApplicationService.kt`

**Items**:
1. Create new method `enrichRouteDetail(route: Route, userId: String?): RouteDetailResponse`
2. Fetch all 10 associated data types by routeId and related foreign keys
3. Apply N+1 optimization: Use batch queries where applicable
4. Build complete RouteDetailResponse with populated nested objects
5. Handle null safety for optional fields

**Method Structure**:
```kotlin
@Transactional(readOnly = true)
private fun enrichRouteDetail(route: Route, userId: String?): RouteDetailResponse {
    val tags = routeTagRepository.findByRouteId(route.id)
    val segments = segmentRepository.findByRouteId(route.id)
    val campsites = campsiteRepository.findByRouteId(route.id)
    // ... fetch other associated data
    return RouteDetailResponse(/* build with fetched data */)
}
```

**Validation**: Unit test with mock repositories, integration test with real data

---

### Task 1.3: Update getRouteFullDetails() to Use Data Enrichment
**Objective**: Modify getRouteFullDetails() to call enrichRouteDetail()
**Location**: `src/main/kotlin/org/example/route/service/RouteApplicationService.kt`

**Current Code** (lines 33-46):
```kotlin
fun getRouteFullDetails(routeId: String, userId: String?): RouteDetailResponse? {
    val route = routeService.getRouteWithAccessCheck(routeId, userId) ?: return null
    routeService.recordRouteVisitIfNeeded(route, userId)
    val isFavorite = userId?.let { routeService.isRouteFavorited(routeId, it) } ?: false
    return org.example.route.dto.RouteDetailResponse.fromRoute(route, isFavorite)  // OLD
}
```

**New Implementation**:
```kotlin
fun getRouteFullDetails(routeId: String, userId: String?): RouteDetailResponse? {
    val route = routeService.getRouteWithAccessCheck(routeId, userId) ?: return null
    routeService.recordRouteVisitIfNeeded(route, userId)
    val isFavorite = userId?.let { routeService.isRouteFavorited(routeId, it) } ?: false
    return enrichRouteDetail(route, userId).copy(isFavorite = isFavorite)  // NEW
}
```

**Validation**: Integration test verifying all fields are populated

---

### Task 1.4: Query Repositories for Associated Data
**Objective**: Create or verify repository query methods for all associated data types
**Location**: Multiple repository files

**Items to Verify/Create**:
1. `RouteTagRepository.findByRouteId(routeId: String): List<RouteTag>`
2. `SegmentRepository.findByRouteId(routeId: String): List<Segment>` (likely exists)
3. `CampsiteRepository.findByRouteId(routeId: String): List<Campsite>`
4. `SupplyRepository.findByRouteId(routeId: String): List<Supply>`
5. `WaterSourceRepository.findByRouteId(routeId: String): List<WaterSource>`
6. `MarkerPointRepository.findByRouteId(routeId: String): List<MarkerPoint>`
7. `DailyPlanRepository.findByRouteId(routeId: String): List<DailyPlan>`
8. `HitchhikeContactRepository.findByRouteId(routeId: String): List<HitchhikeContact>`
9. `MapDataRepository.findByRouteId(routeId: String): MapData?` (for distance, duration, elevation)

**Validation**: Each repository query returns expected results in unit test

---

### Task 1.5: Update RouteDetailResponse.fromRoute() Fallback
**Objective**: Keep fromRoute() as fallback, but document it's deprecated
**Location**: `src/main/kotlin/org/example/route/dto/RouteDetailResponse.kt`

**Items**:
1. Add @Deprecated annotation with migration note
2. Update comments to explain enrichRouteDetail is preferred
3. Maintain backward compatibility for now

---

## Phase 2: User Interaction APIs

### Task 2.1: Implement Favorite/Unfavorite Routes
**Objective**: Persist user route favorite relationships
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 109-130)

**Items**:
1. Check if UserRouteFavorite table exists; if not, create it
2. Implement `favoriteRoute()` endpoint (currently TODO)
3. Implement `unfavoriteRoute()` endpoint (currently TODO)
4. Add validation: prevent duplicate favorites, handle non-existent routes
5. Update user statistics

**DB Schema** (if needed):
```sql
CREATE TABLE user_route_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    route_id VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_route (user_id, route_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (route_id) REFERENCES routes(id)
);
```

**Validation**: Unit test with mock repositories, integration test with real DB

---

### Task 2.2: Implement Route Completion Tracking
**Objective**: Track when users complete routes
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 135-143)

**Items**:
1. Check if UserRouteCompletion table exists; if not, create it
2. Implement `completeRoute()` endpoint
3. Add business logic to update route usage_count
4. Add business logic to record completion timestamp
5. Handle edge cases (route already marked complete by user)

**DB Schema** (if needed):
```sql
CREATE TABLE user_route_completion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    route_id VARCHAR(50) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration_minutes INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (route_id) REFERENCES routes(id)
);
```

**Validation**: Unit test, integration test verifying usage_count increment

---

### Task 2.3: Implement Personal Route Lists - My Routes
**Objective**: Fetch routes created by current user
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 148-162)

**Current Implementation**:
```kotlin
fun getMyRoutes(...): ResponseEntity<ApiResponse<Page<RouteBasicResponse>>> {
    // TODO: implement
    return ResponseUtil.successPage(emptyPage)
}
```

**New Implementation**:
1. Add query in RouteRepository: `findByCreatedBy(userId: String, pageable: Pageable)`
2. Call RouteApplicationService to execute query
3. Return paginated results with proper sorting (by created_at DESC)
4. Handle pagination parameters validation

**Validation**: Integration test with test data

---

### Task 2.4: Implement Personal Route Lists - Favorite Routes
**Objective**: Fetch routes favorited by current user
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 167-181)

**Items**:
1. Query UserRouteFavorite table by user_id
2. Join with Route table to get route details
3. Return paginated results sorted by favorite date (DESC)
4. Include isFavorite=true in response for all results

**Query Strategy**:
```sql
SELECT r.* FROM routes r
INNER JOIN user_route_favorite urf ON r.id = urf.route_id
WHERE urf.user_id = ? AND r.status != 'DELETED'
ORDER BY urf.created_at DESC
LIMIT ? OFFSET ?
```

**Validation**: Integration test

---

### Task 2.5: Implement Personal Route Lists - Completed Routes
**Objective**: Fetch routes completed by current user
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 186-200)

**Items**:
1. Query UserRouteCompletion table by user_id
2. Join with Route table to get route details
3. Return paginated results sorted by completion date (DESC)
4. Include completion details if available

**Query Strategy**: Similar to Task 2.4, using user_route_completion table

**Validation**: Integration test

---

## Phase 3: Advanced Features

### Task 3.1: Implement Route Recommendations
**Objective**: Suggest routes to user based on preferences
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 205-220)

**Simple Initial Algorithm**:
1. Get user's favorite routes
2. Extract common attributes (region, difficulty, route_type)
3. Query routes with matching attributes but not yet favorited
4. Sort by popularity
5. Limit results

**Items**:
1. Add new method in RouteService for recommendation logic
2. Implement in RouteApplicationService.getRecommendedRoutes()
3. Handle case when no favorites exist (fallback to popular routes)
4. Support optional type parameter for filtering

**Validation**: Unit test with mock data, integration test

---

### Task 3.2: Implement Nearby Routes Search
**Objective**: Find routes near a geographical location
**Location**: `src/main/kotlin/org/example/route/controller/RouteController.kt` (lines 225-241)

**Implementation Strategy**:
1. Extract start/end waypoints for each route (or use route centroid if available)
2. Calculate Haversine distance to given latitude/longitude
3. Filter routes within radius
4. Sort by distance ascending
5. Return paginated results

**Items**:
1. Add new query in WaypointRepository for geographical queries
2. Implement distance calculation (consider using database geospatial functions)
3. Support configurable radius parameter
4. Optimize with spatial indexing if available

**Validation**: Unit test with mock coordinates, integration test

---

### Task 3.3: Implement Related Routes Suggestions
**Objective**: Show similar routes on detail page
**Location**: Extend RouteDetailResponse to include related routes (new field)

**Algorithm**:
1. Find routes with same region and similar difficulty (±1 level)
2. Exclude current route
3. Sort by popularity
4. Limit to top 5
5. Return as nested list in RouteDetailResponse

**Items**:
1. Add `relatedRoutes: List<RouteBasicResponse>` field to RouteDetailResponse
2. Populate in enrichRouteDetail() method
3. Update API contract documentation

**Validation**: Integration test

---

## Cross-Cutting Concerns

### Task 4.1: Error Handling and Edge Cases
**Objective**: Handle all edge cases gracefully
**Location**: All modified code

**Items**:
1. Handle route not found (HTTP 404)
2. Handle user not found (HTTP 404 or treat as anonymous)
3. Handle duplicate favorite attempt (HTTP 409 or silent success)
4. Handle null/missing associated data (don't break response)
5. Add proper exception handling in try-catch blocks

**Validation**: Test each edge case scenario

---

### Task 4.2: Unit Tests
**Objective**: Achieve >80% code coverage for new/modified code
**Location**: `src/test/kotlin/org/example/route/`

**Items**:
1. Unit tests for RouteApplicationService enrichment methods
2. Unit tests for recommendation algorithm
3. Unit tests for distance calculation
4. Mock all repository dependencies

**Validation**: Run `mvn test` and verify coverage

---

### Task 4.3: Integration Tests
**Objective**: Verify all endpoints work with real database
**Location**: `src/test/kotlin/org/example/route/` (integration tests)

**Items**:
1. Test complete route detail API response
2. Test favorite/unfavorite flow
3. Test route completion tracking
4. Test all personal list endpoints
5. Test recommendations with various user profiles
6. Test nearby routes with different coordinates

**Validation**: All integration tests pass

---

### Task 4.4: Database Migration (if needed)
**Objective**: Create migration files for new tables
**Location**: Database migration folder (likely `src/main/resources/db/migration/`)

**Items**:
1. Create migration for UserRouteFavorite table (if needed)
2. Create migration for UserRouteCompletion table (if needed)
3. Add necessary indices for performance
4. Document schema in migration comments

**Validation**: Migration runs successfully on test database

---

### Task 4.5: API Documentation Update
**Objective**: Update Swagger/OpenAPI documentation
**Location**: Controller Javadoc and OpenAPI annotations

**Items**:
1. Update @Operation descriptions for all modified endpoints
2. Document response examples with complete data
3. Document error codes and scenarios
4. Update parameter documentation

**Validation**: Swagger UI shows complete documentation

---

## Task Dependencies

```
Phase 1 (Sequential):
  1.1 → 1.2 → 1.3 → 1.4 → 1.5

Phase 2 (Sequential):
  2.1 → 2.2 → 2.3 → 2.4 → 2.5
  (Requires Phase 1 completion)

Phase 3 (Can partially parallel):
  3.1 (depends on Phase 2)
  3.2 (depends on Phase 1)
  3.3 (depends on Phase 1 and 2)

Cross-Cutting:
  4.1, 4.2, 4.3 (run continuously)
  4.4 (run when new tables needed)
  4.5 (final step)
```

## Estimated Effort

| Task | Complexity | Effort | Notes |
|------|-----------|--------|-------|
| 1.1  | Low       | 30min  | Straightforward DI |
| 1.2  | Medium    | 1.5h   | Coordinate multiple repos |
| 1.3  | Low       | 20min  | One-line change (almost) |
| 1.4  | Medium    | 1h     | Verify queries exist or create |
| 1.5  | Low       | 15min  | Documentation |
| 2.1  | Medium    | 1.5h   | New DB table + validation |
| 2.2  | Medium    | 1h     | Similar to 2.1 |
| 2.3  | Low       | 45min  | Simple query |
| 2.4  | Low       | 45min  | Join query |
| 2.5  | Low       | 45min  | Join query |
| 3.1  | Medium    | 1.5h   | Algorithm logic |
| 3.2  | Medium    | 1.5h   | Geo calculations |
| 3.3  | Low       | 1h     | Build on existing |
| 4.1  | Low       | 1h     | Error handling |
| 4.2  | Medium    | 2h     | Unit tests |
| 4.3  | Medium    | 2h     | Integration tests |
| 4.4  | Low       | 30min  | Migrations |
| 4.5  | Low       | 30min  | Documentation |
| **Total** | | **18.5h** | |

## Success Criteria

✅ **Phase 1 Completion**:
- RouteDetailResponse returns all 12 associated data fields populated
- No N+1 queries detected in profiling
- Integration test confirms complete data

✅ **Phase 2 Completion**:
- All 5 personal list endpoints return actual data (not empty)
- Favorite/unfavorite/complete operations persist to database
- All endpoints validated with integration tests

✅ **Phase 3 Completion** (Optional):
- Recommendations endpoint returns relevant suggestions
- Nearby routes search works with real coordinates
- Related routes suggestions appear on detail page

✅ **Cross-Cutting**:
- >80% test coverage for modified code
- Zero unhandled exceptions in error scenarios
- Swagger documentation complete and accurate
- Zero linter warnings for new code
