# Project Context: WalkBG Backend Service

## Purpose
WalkBG is a hiking trip planning assistant backend service. It provides APIs for route management, user management, trip planning, and related features to support the frontend mobile application.

## Tech Stack
- **Language**: Kotlin
- **Framework**: Spring Boot 3.2.3
- **Database**: MySQL (production), H2 (development)
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Java Version**: 17
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Caching**: Spring Cache with Caffeine

## Project Conventions

### Code Style
- **Language**: Kotlin with Spring all-open compiler plugin
- **Naming**: 
  - Packages: `org.example.<module>.<layer>`
  - Classes: PascalCase for entities and services
  - Functions: camelCase
  - Constants: UPPER_SNAKE_CASE
- **Formatting**: Kotlin official code style

### Architecture Patterns
- **Layered Architecture**: Controller → ApplicationService → DomainService → Repository
- **Entity Design**: Single-directional associations (no bidirectional relationships)
- **Data Access**: Query-on-demand pattern (entities don't hold collection references)
- **Exception Handling**: Custom `BusinessException` with HTTP status codes
- **Response Format**: Unified `ApiResponse<T>` wrapper for all API responses
- **Transaction Management**: Service-level `@Transactional` annotations
- **Caching**: Spring Cache annotations (configured but not yet widely used)

### Testing Strategy
- Unit tests for business logic
- Integration tests with `@DataJpaTest` and `@SpringBootTest`
- Mock external dependencies with Mockito

### Git Workflow
- Feature branches with descriptive names
- Conventional commits (feat:, fix:, refactor:)
- Pull requests with review before merge

## Domain Context
The system manages:
- **Users**: User profiles with statistics
- **Routes**: Hiking routes with waypoints, segments, campsites
- **Trips**: Organized hiking trips with participants
- **Water/Supplies**: Water sources and supply points along routes
- **Equipment**: User equipment lists and items
- **Contacts**: Guides and transport contacts

Key domain constraints:
- Routes can have multiple waypoints and segments
- Users participate in trips
- Routes can be favorited or marked as completed
- Equipment lists are owned by users

## Important Constraints
- Single-directional associations to prevent circular dependencies and N+1 queries
- No collection references in entities (use Repository queries instead)
- All business logic must go through DomainService layer
- ApplicationService handles DTO conversion and orchestration only
- Transactional boundaries at Service layer

## API Contract
**Frontend Application**: Walk (Flutter/Dart mobile app)
- API Base URL: `/walkbg/api/v1`
- API Contract Specification: See `specs/api-contract/spec.md`

**API Contract Guidelines**:
- **Response Format**: All APIs MUST use `ApiResponse<T>` wrapper
- **Field Naming**: Kotlin uses camelCase, JSON uses snake_case (with `@JsonProperty` annotation)
- **Enum Values**: Use integer values (0, 1, 2...) with `@JsonValue` annotation
- **Pagination**: Parameters: page (starts from 1), size (default 20), sort (format: field,direction)
- **DateTime Format**: ISO 8601 format in UTC timezone (e.g., `2025-01-15T10:00:00.000Z`)
- **Authentication**: JWT Token via `Authorization: Bearer {token}` header
- **Error Codes**: Use standard error codes (INVALID_REQUEST, RESOURCE_NOT_FOUND, UNAUTHORIZED, FORBIDDEN, CONFLICT, INTERNAL_ERROR)
- **CORS**: Allow all origins in development, restrict to frontend domain in production
- **API Version**: Include `X-API-Version: v1` in response headers
- **Detailed Specification**: `openspec/specs/api-contract/spec.md`
