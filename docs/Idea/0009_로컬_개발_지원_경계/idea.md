# 로컬 전용 코드의 위치와 경계

**날짜**: 2026-03-18
**상태**: 검토 메모 (현재 `@Profile("local")` + `src/main` 유지, 개선 방향 기록)

---

## 배경

로컬 개발 지원 코드(`LocalSeedConfig`, `LocalAdminSeedService`, `LocalBookingClassSeedService`, `LocalPhoneVerificationController`)가 현재 `src/main`에 있다.
`@Profile("local")`이 붙어 있어 운영 프로파일에서는 빈이 등록되지 않으므로 런타임 위험은 없다. 다만 `src/main`은 의미상 "운영 코드"에 가까워서 위치가 아주 깔끔하지는 않다.

---

## 현재 구성

| 클래스 | 역할 | 대체 가능 여부 |
|--------|------|---------------|
| `LocalSeedConfig` | 앱 시작 시 기본 데이터 자동 seed | Flyway repeatable migration으로 대체 가능하나, 패스워드 해시 등 Java 로직이 필요해 실익이 낮음 |
| `LocalAdminSeedService` | 로컬 관리자 계정 seed | 동일 |
| `LocalBookingClassSeedService` | 로컬 예약 클래스 데이터 seed | 동일 |
| `LocalPhoneVerificationController` | 인증 코드 DB 직접 조회 API | **대체 불가** — 로컬에 SMS가 없으므로 프론트 E2E 인증 흐름 테스트에 필수 |
| `LocalRefundFailureController` | 다음 환불 1건 실패 재현 API | 로컬 smoke/E2E에서 환불 실패와 관리자 재시도 흐름을 재현할 때 사용 |

현재 local dev 훅은 모두 `@Profile("local")`로 운영 프로필에서 제외한다.

---

## 개선 방향 (미적용)

### 옵션 1 — `local` 패키지로 집결
현재 `app.web.admin`, `app.booking`, `app.admin`, `config` 등에 흩어진 `Local*` 클래스를 `local` 패키지 하나로 모은다.
설정은 그대로 두고 탐색 노이즈만 줄이는 가장 낮은 비용의 개선이다.

### 옵션 2 — Gradle source set 분리 (`src/local/java`)
운영 jar 빌드 시 `src/local`을 classpath에서 제외하고, 로컬 실행 시에만 포함한다.
`build.gradle`에 별도 sourceSet 정의와 `bootRun` 태스크 조정이 필요하다.
jar 크기와 보안 노출이 실제로 문제가 되는 시점에 검토한다.

---

## 현재 결론

`@Profile("local")` + `src/main` 구성을 유지한다. 런타임 안전성은 확보되어 있고, 별도 source set 분리는 이 프로젝트 규모에서 설정 복잡도 대비 얻는 이익이 작다.
코드가 더 늘어나거나 보안 감사 요건이 생기면 옵션 2를 검토한다.
