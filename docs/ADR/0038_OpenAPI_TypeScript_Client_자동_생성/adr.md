# ADR-0038: OpenAPI와 TypeScript API client 자동 생성

**날짜**: 2026-07-21
**상태**: Accepted

---

## 컨텍스트

백엔드 Controller와 웹 DTO, 프론트엔드의 요청 함수와 TypeScript DTO를 각각 수동으로 관리해 계약 변경이 한쪽에만 반영될 수 있었다. 기존 Spring REST Docs 테스트는 실제 요청·응답과 상태 코드를 잘 검증하지만 필드 descriptor가 없어, 그대로 OpenAPI 생성 원본으로 사용하면 응답 schema가 빈 객체가 된다.

프론트 공통 `api()`는 세션 cookie, CSRF, 관리자·비회원 헤더, timeout, `ApiError`, Sentry를 책임진다. 생성 client가 별도 `fetch` 경계를 만들면 이 동작이 갈라진다.

## 결정

### 1. Controller와 웹 DTO에서 OpenAPI를 생성한다

- Springdoc은 테스트 클래스패스에서만 전체 `/api/v1/**` 명세를 생성한다. 운영 애플리케이션에는 Swagger UI나 명세 endpoint를 노출하지 않는다.
- `:adapter-in-web:openApiTest`가 속성 키를 정렬한 `adapter-in-web/build/api-spec/openapi3.json`을 만들고, `:adapter-in-web:openapi3`가 커밋 대상 `docs/PRD/0004_API_계약/openapi3.json`을 갱신한다. 정렬은 프레임워크의 속성 순서 차이로 생기는 거짓 drift를 막는다.
- `verifyOpenApi`는 생성 결과와 커밋된 스냅샷을 비교하며 `check`에 포함된다.
- REST Docs는 실제 HTTP 예시·상태·에러 계약 검증으로 계속 유지한다. 같은 정보를 두 도구에 모두 손으로 기술하지 않는다.

### 2. Orval 생성 client는 기존 HTTP 경계를 사용한다

- Orval은 커밋된 OpenAPI 스냅샷에서 TypeScript 요청 함수와 서버 DTO를 `frontend/src/generated/api`에 생성한다.
- 생성 함수는 `generatedApiClient` custom mutator를 통해 기존 `api()`를 호출한다. 인증, CSRF, timeout, 오류 변환과 관측 동작은 바뀌지 않는다.
- React Query hook은 생성하지 않는다. query key, cache, invalidation과 화면 흐름은 기존 feature code가 소유한다.
- 생성 파일은 수동 편집하지 않고 프론트 독립 Docker build를 위해 Git에 커밋한다.
- 여러 Orval 대상이 같은 `src/generated/api`를 사용하므로 생성 명령이 디렉터리를 시작 시 한 번만 비운다. 각 대상의 `clean`은 끄고 서로의 결과를 삭제하지 않게 한다. 공통 input·mutator 설정은 `generatedApi(target, tags)`가 한 곳에서 소유한다.

### 3. 엔드포인트는 schema 정확성을 확인하며 전환한다

- 생성 client 실사용 범위는 공개 상품 조회, 회원 소셜 계정 관리, 회원 알림함, 고객 결제 상태·8회권 가격 정책 조회, 관리자 인증·MFA, 예약, 주문 클레임, 상품 Q&A 조회·상태 변경이다. 기능 코드는 생성 함수를 얇게 감싸고 React Query 상태와 화면 흐름만 소유한다.
- 연동 대상은 고유하고 안정적인 `operationId`를 사용하고, 응답의 필수값·nullable·enum을 OpenAPI에 정확히 표현한다. 기존 고유 ID는 호환성을 위해 유지하고, 이름이 충돌하는 엔드포인트만 Controller에 명시적 ID를 둔다. 생성 테스트는 Springdoc 충돌 회피용 숫자 접미사(`*_1`, `*_2`)가 남으면 실패시킨다.
- 연동된 서버 request/response DTO는 생성 타입을 원본으로 사용한다. 화면 form state와 view model은 수동 타입으로 유지할 수 있다.
- 결제 `prepare`의 공개 union은 `ORDER/BOOKING/PASS`만 포함한다. 암호화 저장용 `PREPARED_*`는 별도
  application 타입으로 두어 OpenAPI와 생성 client 후보 schema에 노출하지 않는다.
- 결제 `prepare` 호출 자체와 multipart, 나머지 관리자 API는 생성 결과와 호출 옵션을 확인한 뒤 순차 전환한다. 단순 조회와 명시적으로 모델링된 비회원 인증 헤더는 schema가 정확하면 먼저 전환한다.
- 관리자 API는 도메인 단위로 전환한다. 관리자 예약·인증·MFA·주문 클레임·상품 Q&A는 웹 응답 DTO에서 필수값·nullable·enum을 명시하고, 수동 요청 함수와 서버 DTO 선언을 제거했다.

### 4. CI에서 생성물 drift를 차단한다

- 백엔드 `build`는 OpenAPI 스냅샷 drift를 검증한다.
- 프론트 CI는 `npm run api:check`로 생성 코드를 다시 만든 뒤 Git diff가 없는지 확인하고 `npm run build`를 실행한다.
- Controller, 웹 DTO 또는 전환된 API 계약을 바꾸면 OpenAPI 스냅샷과 생성 client를 같은 커밋에서 갱신한다.

## 결과

### 장점

- Controller/DTO와 프론트 서버 타입의 불일치를 빌드에서 발견한다.
- 생성 요청 코드도 기존 보안·오류·관측 경계를 그대로 사용한다.
- REST Docs 테스트에 대량의 schema descriptor를 중복 작성하지 않는다.

### 단점

- OpenAPI 생성은 전체 Spring context와 Testcontainers를 사용하므로 단순 unit test보다 느리다.
- 아직 전환하지 않은 API의 생성 schema는 프론트 사용 전에 필수값·nullable·enum을 추가 확인해야 한다.
- 생성 파일과 명세 스냅샷이 저장소 크기를 늘린다.

## 참고

- [API 계약 PRD](../../PRD/0004_API_계약/spec.md)
- [ADR-0027 테스트 전략](../0027_테스트_전략과_최소_테스트_세트_기준선/adr.md)
