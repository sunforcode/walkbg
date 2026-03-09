# Proposal: Complete Route Detail API Implementation

## Problem Statement

The Route Detail API (`GET /api/v1/routes/{id}`) currently returns incomplete data. The `RouteDetailResponse.fromRoute()` method initializes all associated data collections (segments, daily_plans, campsites, water_sources, supplies, etc.) as empty lists, even though the corresponding data exists in the database.

### Current Issues

1. **RouteDetailResponse Always Returns Empty Collections**
   - File: `src/main/kotlin/org/example/route/dto/RouteDetailResponse.kt` (lines 50-66, 99-110)
   - All associated data is initialized as `emptyList()` regardless of actual database content
   - Missing repository injections in `RouteApplicationService` to fetch related data

2. **Missing Controller Endpoints**
   - File: `src/main/kotlin/org/example/route/controller/RouteController.kt`
   - Lines 109-143: Favorite/unfavorite endpoints have `TODO` comments, return dummy success responses
   - Lines 167-200: Personal route lists (favorites, completed, my routes) return empty pages with `TODO`
   - Lines 205-219: Recommendations and nearby routes endpoints are empty stubs

3. **Incomplete User Interaction Features**
   - No implementation for favorite/unfavorite route functionality
   - No tracking of route completions by users
   - Missing user statistics updates

4. **Missing Related Data Queries**
   - No way to fetch related routes based on region, difficulty, or user preferences
   - Recommendations endpoint exists but has no implementation strategy

5. **Incomplete Trip Association**
   - No endpoint to fetch trips associated with a specific route
   - No way to see how many users have used this route in their trips

## Proposed Solution

This change will complete the Route Detail API implementation in three phases:

### Phase 1: Complete RouteDetailResponse Data Enrichment
- Inject required repositories into `RouteApplicationService`
- Populate all associated data fields in `RouteDetailResponse.fromRoute()`
- Optimize queries to avoid N+1 problems

### Phase 2: Implement User Interaction APIs
- Implement favorite/unfavorite route functionality
- Implement route completion tracking
- Implement personal route list queries (my routes, favorites, completed)

### Phase 3: Advanced Features
- Implement route recommendations based on user profile and history
- Implement nearby routes search using geographical coordinates
- Add related routes suggestions

## Scope

### In Scope
- ✅ Populate all 12 data fields in RouteDetailResponse
- ✅ Implement 5 user interaction endpoints (favorite, unfavorite, complete, get personal lists)
- ✅ Add database layer support for user-route relationships (likely UserRouteFavorite table)
- ✅ Add related routes/recommendations framework (can start with simple approach)

### Out of Scope
- Route CRUD operations (create, update, delete) - separate proposal
- Advanced recommendation algorithm - can be improved later
- Performance optimization/caching - follow-up work

## Impact Analysis

### Affected Components
- **Controllers**: RouteController (6 endpoints modified)
- **Services**: RouteApplicationService, RouteService (enhanced)
- **DTOs**: RouteDetailResponse, RouteBasicResponse (validation)
- **Repositories**: Multiple new queries needed for associated data
- **Database**: Potential new table for user-route relationships (UserRouteFavorite)
- **Frontend**: Route detail page will receive complete data

### Database Changes
- Possible new table: `user_route_favorite` (user_id, route_id, created_at)
- Possible new table: `user_route_completion` (user_id, route_id, completed_at)
- No schema changes to existing tables

### API Contract Changes
- `RouteDetailResponse` will have populated nested objects
- 5 new/fixed endpoints returning proper data
- No breaking changes to existing fields

## Questions for Clarification

1. **Related Routes Algorithm**: Should related routes be based on:
   - Same region + similar difficulty?
   - User preferences + history?
   - Simple SQL queries or machine learning model?

2. **Trip Association**: Should route detail show:
   - Count of trips using this route?
   - List of specific trips with their participants?
   - Just the count for now?

3. **Completion Tracking**: Should we track:
   - Only the latest completion date?
   - Full history of attempts?
   - Statistics (avg time taken, etc.)?

4. **Personal Lists Performance**: For users with thousands of favorites:
   - Use pagination (already in design)?
   - Add sorting options (by date, popularity, difficulty)?

## Timeline Estimate
- Phase 1 (Data Enrichment): 2-3 hours
- Phase 2 (User Interactions): 3-4 hours
- Phase 3 (Advanced Features): 4-5 hours
- Testing & Integration: 2-3 hours

**Total: 11-15 hours**

## Implementation Strategy

1. Start with Phase 1 (foundational data enrichment)
2. Proceed to Phase 2 (core user interactions)
3. Phase 3 can be deferred to a follow-up if time is limited

This modular approach allows earlier phases to be released independently.
