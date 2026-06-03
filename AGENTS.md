# WalkBG Backend AI Instructions

This file guides AI assistants working in the Kotlin/Spring Boot backend.

## Read Before Editing

1. `../AGENTS.md`
2. `../DEVELOPMENT_PARADIGM.md`
3. `../AI_DEVELOPMENT.md`
4. `openspec/project.md`
5. Relevant specs under `openspec/specs/`
6. Relevant module files under `src/main/kotlin/org/example/`

If the change modifies API behavior, DTOs, enums, pagination, errors, SSE, callback payloads, permissions, schema, or cross-end behavior, create or update a root OpenSpec change first.

## Architecture Rules

Standard backend flow:

```text
Controller -> ApplicationService -> DomainService -> Repository -> Entity
```

- Controller handles HTTP, validation, OpenAPI annotations, and response wrapping only.
- ApplicationService handles use-case orchestration, DTO conversion, and cross-domain coordination.
- DomainService handles business rules, invariants, and state transitions.
- Repository handles data access.
- Entity associations should remain single-directional and avoid collection references.
- Transaction boundaries belong at service layer.
- All externally visible responses must follow the agreed API contract.

## API Contract Rules

- REST API lives under `/walkbg/api/v1` unless a spec says otherwise.
- JSON fields use `snake_case`; Kotlin fields use `camelCase`.
- Enums use stable integer values across ends.
- Pagination origin and response shape must be explicit in specs.
- Errors must be structured and safe for production logs and clients.

## Verification

Use:

```bash
mvn test
```

For API contract changes, add or update controller/service tests and regenerate/check OpenAPI docs when relevant.
