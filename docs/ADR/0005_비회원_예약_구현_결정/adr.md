# ADR-0005: §5.2 게스트 예약 구현 결정

- **날짜**: 2026-02-22
- **상태**: 확정

---

## 배경

비회원(게스트)이 체험 슬롯을 예약하는 §5.2를 구현하면서 다음 설계 결정이 필요했다.

---

## 결정 1 — Guest upsert by phone

**선택**: 동일 전화번호가 존재하면 기존 Guest 재사용 (upsert 패턴)

**이유**:
- 재예약 시 guest row 중복 생성 방지
- `(slot_id, guest_id)` 쌍으로 중복 예약 차단이 가능해짐 (BookingRepository.existsBySlotIdAndGuestId)
- 한 게스트의 예약 이력 조회가 단순해짐

**트레이드오프**: 전화번호 변경 시 기존 guest row를 재사용하게 됨 — MVP에서 허용 가능한 수준.

---

## 결정 2 — PhoneVerification을 domain 모듈에 위치

**선택**: `domain/booking/PhoneVerification.java`

**이유**:
- 프로젝트 관례상 JPA 엔티티는 domain 모듈에 위치
- CLAUDE.md: "domain ← 도메인 모델·열거형만. JPA API만 허용."
- 기술적 인프라 관심사(OTP)이지만 현재 모듈 경계상 domain이 가장 적합

**트레이드오프**: 순수 도메인 의미가 약한 엔티티가 domain에 포함됨 — 이후 별도 `verification` 도메인으로 분리 가능.

---

## 결정 3 — access_token을 bookings 컬럼으로 관리

**선택**: V3 마이그레이션으로 `bookings.access_token VARCHAR(64)` 컬럼 추가

**이유**:
- 비회원 조회는 항상 booking과 1:1 관계 → 별도 테이블 불필요
- `findByIdAndAccessToken` 단일 쿼리로 bookingId + token 동시 검증 가능
- UNIQUE 인덱스로 충돌 방지

**형식**: `UUID.randomUUID().toString().replace("-", "")` → 32자 hex 문자열 (VARCHAR 64 안에 여유)

---

## 결정 4 — MVP에서 인증 코드를 응답에 포함

**선택**: `SendVerificationResponse`에 `code` 필드 포함

**이유**:
- 실제 SMS 발송 연동 전 개발/테스트 가능
- 사용자 합의 (2026-02-22)

**위험**: 프로덕션 배포 전 반드시 제거해야 함.
`SendVerificationResponse.code` 필드와 `SendVerificationResponse.from()` 팩토리에서 code 반환 제거 필요.

---

## 결정 5 — confirmBooking을 GuestBookingService 트랜잭션 내에서 호출

**선택**: `@Transactional createGuestBooking()` 내에서 `slotManagementService.confirmBooking(slotId)` 호출

**이유**: ADR-0003 준수. `booked_count` 증가와 Booking 생성이 별도 트랜잭션이면 정원 초과 롤백 시 booking row 고아 발생.

**구현**: `SlotManagementService`의 `@Transactional`은 호출자 트랜잭션에 참여(REQUIRED, 기본값).

---

## 결정 6 — GlobalExceptionHandler에 DataIntegrityViolationException 핸들러 추가

**선택**: `DataIntegrityViolationException` → 409 `DUPLICATE_BOOKING` 반환

**이유**: TOCTOU 경쟁 조건에서 애플리케이션 수준 중복 체크를 통과해도 DB UNIQUE 제약이 최후 방어선 역할을 해야 함. 기존에는 500으로 떨어졌음 (HANDOFF 미해결 과제 해소).

**트레이드오프**: DataIntegrityViolationException이 다른 원인(FK 위반 등)일 때도 DUPLICATE_BOOKING으로 응답됨 — 이후 원인별 분기 필요 시 message 파싱 가능.

---

## 새 에러 코드

| 코드 | HTTP | 발생 상황 |
|------|------|-----------|
| `DUPLICATE_BOOKING` | 409 | 동일 전화번호 + 동일 슬롯 중복 예약 |
| `SLOT_NOT_AVAILABLE` | 409 | 비활성화된 슬롯 예약 시도 |
| `PHONE_VERIFICATION_FAILED` | 400 | 인증 코드 불일치 또는 만료 |
