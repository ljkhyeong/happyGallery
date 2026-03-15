# U1. Customer Auth Foundation

이 단위는 "고객 회원가입/로그인/세션"이라는 기반만 만드는 작업이다.

성격:
- 백엔드 우선
- 프론트는 최소한의 로그인/회원가입 진입점만 포함
- 이후 모든 회원 전용 조회/결제 흐름의 선행 조건

---

## 1. 목표

- 고객용 회원가입, 로그인, 로그아웃, 현재 사용자 조회 API를 추가한다.
- 고객 세션은 관리자 세션과 분리된 저장소/전략으로 관리한다.
- `users` 테이블을 실제 고객 계정 모델로 승격한다.

---

## 2. 범위

포함:
- `users` 엔티티/리포지토리/서비스 추가 또는 현재 미사용 구조 연결
- 고객 인증 API
- 고객 세션 필터 또는 Spring Security 기반 인증 계층
- 프론트 `/login`, `/signup` 화면의 최소 동작
- `phone_verified`를 포함한 회원 모델 보강

제외:
- 주문/예약/8회권 회원 전용 조회
- guest 이력 claim
- 상품 상세 구매 패널

---

## 3. 권장 설계

- 고객 인증은 관리자 인증과 분리한다.
- 권장안은 `HttpOnly` 쿠키 기반 세션이다.
- 세션 저장은 인메모리 대신 DB 지속 저장을 권장한다.
- 사용자 식별자는 이메일 로그인 + 휴대폰 검증 보조 수단 조합으로 간다.

권장 DB 보강:
- `users.phone_verified`
- `users.last_login_at`
- `user_sessions(id, user_id, session_token_hash, expires_at, created_at)`

---

## 4. 주요 작업

백엔드:
- 고객 인증용 migration 추가
- `User` 도메인/리포지토리 실제 연결
- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/me`
- 고객 인증 필터 또는 Security config

프론트:
- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/SignupPage.tsx`
- 전역 `customer auth` 상태 저장 훅
- Layout 상단 로그인/회원가입/내정보 진입 링크

문서:
- `docs/PRD/0002_member_store_transition/spec.md`
- 필요 시 고객 인증 전략 ADR 초안 추가

---

## 5. 변경 후보 파일

백엔드:
- `domain/`
- `infra/`
- `app/src/main/java/com/personal/happygallery/app/web/**`
- `app/src/main/resources/db/migration/**`

프론트:
- `frontend/src/app/App.tsx`
- `frontend/src/shared/ui/Layout.tsx`
- `frontend/src/shared/api/**`
- `frontend/src/pages/**`

---

## 6. 완료 기준

- 회원가입 후 로그인 가능
- 로그인 상태에서 `GET /api/v1/me`가 정상 응답
- 브라우저 새로고침 후 세션 유지 정책이 문서와 구현에 일치
- 관리자 인증과 고객 인증이 서로 간섭하지 않음

---

## 7. 최소 검증

- `./gradlew --no-daemon :app:test --tests ...Auth...`
- `cd frontend && npm run build`

---

## 8. 다음 단위로 넘길 산출물

- 고객 인증 API 계약
- 프론트에서 사용할 현재 사용자 응답 타입
- 쿠키/세션 만료 정책
- `401` 발생 시 고객 프론트 공통 처리 규칙
