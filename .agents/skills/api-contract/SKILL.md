---
name: api-contract
description: happyGallery의 엔드포인트·웹 DTO·HTTP 오류·REST Docs·OpenAPI·생성 TypeScript 계약을 변경할 때 사용한다. 동작도 바뀌면 해당 도메인 스킬을 함께 사용한다.
---

# happyGallery API 계약

## 원본과 코드

- 계약은 `docs/PRD/0004_API_계약/spec.md`, 생성 방식은 ADR-0038을 확인한다.
- HTTP 코드는 `adapter-in-web/src/main/java/com/personal/happygallery/adapter/in/web/`, 생성 설정은 `frontend/orval.config.ts`에 있다.

## 계약 규칙

- 경로는 `/api/v1`을 사용한다. 성공 응답은 별도 DTO로 반환하고 controller 내부 DTO·raw Map은 만들지 않는다.
- 오류는 `code`와 `message`를 사용한다. 400·401·403·404·409 매핑은 `GlobalExceptionHandler`의 예외 정책과 맞춘다.
- 관리자 API는 관리자 SecurityFilterChain, `/me/**`는 `ROLE_CUSTOMER`로 보호한다. 관리자 Bearer/API-key 인증과 비관리자 상태 변경의 SPA CSRF 규칙을 유지한다.
- 필드 제거·이름·타입·enum·페이지 의미 변경은 호환성 변경으로 다룬다. `operationId`는 명시적이고 고유하게 유지하며 생성기가 붙인 숫자 접미사를 그대로 받아들이지 않는다.
- 필수값·nullable·enum·path·query·header를 명세에 정확히 표시한다. 항상 반환하는 nullable 필드는 required와 nullable을 모두 표시한다.
- 다른 필드 구성을 가진 중첩 record에는 서로 다른 schema 이름을 붙인다. 다형성은 웹 DTO에 두고 application command로 변환한다. discriminator·`oneOf`·`allOf`가 순환 참조를 만들지 않게 확인한다.
- 문서 생성을 위해 같은 조건의 Bean Validation을 중복하지 않는다. `@Schema`로 표현하되, springdoc이 이를 덮어쓰면 실행 검증과 생성 계약을 함께 유지하는 최소 annotation을 남긴다.
- 새 Orval tag는 포함된 모든 operation을 확인한다. 잘못 생성된 optional·nullable은 서버 명세에서 고친다. feature는 생성 함수를 감싸고 query key·캐시·인증 처리를 유지한다.
- 서버 DTO는 생성 타입을 사용하고, 화면 form·view model만 직접 정의한다.

## 변경과 검증

1. 웹 DTO·Controller와 해당 REST Docs 시나리오, PRD-0004를 수정한다.
2. 영향받는 `:adapter-in-web:restDocsTest --tests "*대상클래스*"`를 `--no-daemon`으로 실행한다.
3. `./gradlew --no-daemon :adapter-in-web:openapi3` 후 `frontend`에서 `npm run api:generate`를 실행한다.
4. `npm run api:check`를 확인하고, 클라이언트 타입이 바뀌면 `npm run build`로 사용처를 검증한다.
