---
name: api-contract
description: >
  Workflow for API contract design, error code conventions, response schema standards, and HTTP status
  mapping in the happyGallery backend. Always use this skill when the request involves: 새 API 엔드포인트
  설계, 응답 DTO 필드 추가/변경, 에러 코드 정의 (UPPER_SNAKE_CASE), HTTP 상태 코드 선택 (409 vs 400 등),
  /api/v1/ 경로 규칙 확인, ErrorResponse 형태 통일, GlobalExceptionHandler 변경, API 버전 관리 전략,
  "이 에러는 어떤 HTTP 상태 코드로 반환해야 해요?", "응답에 필드를 추가하면 하위 호환성에 문제가 있나요?",
  또는 admin endpoint 보호 여부 검토. Also use this skill when someone asks "어떤 HTTP 상태 코드를 써야
  해요?" / "에러 응답 형식이 어떻게 돼요?" / "API 경로 컨벤션이 어떻게 돼요?" / "새 에러 코드 이름을 어떻게
  지어야 해요?". Use this skill alongside the domain skill when adding a new endpoint — this skill
  governs the contract design, while the domain skill governs the business logic.
---

# happyGallery API Contract

## Core references

- Use `docs/PRD/0001_기준_스펙/spec.md` as the source of truth for all endpoint paths, request/response shapes, and error codes.
- Read the needed ADRs:
  - `docs/ADR/0016_API_버전_전략/adr.md`
  - `docs/ADR/0002_state-transition-guard/adr.md` (for domain error → HTTP status mapping)

## URL and versioning conventions

- All API paths use `/api/v1/` prefix.
- Public endpoints: `/api/v1/classes`, `/api/v1/slots`, `/api/v1/bookings`, `/api/v1/passes`, `/api/v1/products`, `/api/v1/orders`
- Admin endpoints: `/api/v1/admin/**` (protected by `AdminAuthFilter`)
- Path variables use `kebab-case` for multi-word segments.

## Response schema conventions

**성공 응답**: 원하는 DTO 직접 반환 (wrapper 없음). HTTP 2xx.

**에러 응답** — `ErrorResponse` 형태로 통일:
```json
{
  "code": "BOOKING_NOT_FOUND",
  "message": "해당 예약을 찾을 수 없습니다."
}
```

## HTTP status mapping

| 상황 | HTTP Status |
|------|-------------|
| 성공 생성 | 201 Created |
| 성공 조회/수정 | 200 OK |
| 요청 파라미터 오류 | 400 Bad Request |
| 인증 실패 (Admin-Key 없음/오류) | 401 Unauthorized |
| 도메인 불가 전이, 비즈니스 규칙 위반 | 409 Conflict |
| 리소스 없음 | 404 Not Found |
| 서버 오류 | 500 Internal Server Error |

## Error code naming conventions

- `UPPER_SNAKE_CASE` 형태.
- 도메인 prefix 붙이기: `BOOKING_NOT_FOUND`, `ORDER_NOT_APPROVABLE`, `PASS_EXPIRED`, `SLOT_FULL`.
- 상태 전이 실패: `{DOMAIN}_NOT_{ACTIONABLE}` 패턴.

## Non-negotiable invariants

- Do not break existing response DTO field names or types — clients depend on them.
- New fields may be added (additive), but existing fields must not be renamed or removed without a versioning plan.
- Admin endpoints must stay behind `AdminAuthFilter`; never expose admin actions on public paths.
- Error codes must be documented in `docs/PRD/0001_기준_스펙/spec.md` when they become part of the public contract.
- Do not declare request/response DTO records (or classes) inline inside controller files. Always place them in the corresponding `dto/` package (`web/customer/dto/`, `web/admin/dto/`, etc.).
- Do not return raw `Map<String, ?>` from controller methods. Always define a named response DTO record so that the API contract is explicit and type-safe.

## Likely code locations

- `app/src/main/java/com/personal/happygallery/app/web/` — Public controllers
- `app/src/main/java/com/personal/happygallery/app/web/admin/` — Admin controllers
- `app/src/main/java/com/personal/happygallery/app/web/admin/dto/` — Request/response DTOs
- `common/src/main/java/com/personal/happygallery/common/exception/` — Error codes and ErrorResponse
- `app/src/main/java/com/personal/happygallery/app/web/GlobalExceptionHandler.java` — HTTP status mapping

## High-value tests (for reference)

- `app/src/test/java/com/personal/happygallery/app/web/admin/AdminSlotUseCaseIT.java`
- `app/src/test/java/com/personal/happygallery/app/web/AdminAuthFilterTest.java`

## Verification workflow

- New endpoint or DTO change: `./gradlew --no-daemon :app:useCaseTest --tests "*UseCaseIT*"`
- Auth filter or error mapping change: `./gradlew :app:test --tests "*AdminAuthFilterTest" --tests "*RateLimitFilterTest"`

## Doc sync checklist

- Endpoint path, request/response shape, error codes: `docs/PRD/0001_기준_스펙/spec.md`
- Versioning strategy changes: `docs/ADR/0016_API_버전_전략/adr.md`
- Session status: `HANDOFF.md`
