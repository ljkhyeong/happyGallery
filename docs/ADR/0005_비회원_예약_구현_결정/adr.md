# ADR-0005: §5.2 게스트 예약 구현 결정

- **날짜**: 2026-02-22
- **상태**: 확정

---

## 배경

비회원(게스트)이 체험 슬롯을 예약하는 §5.2를 구현하면서 다음 설계 결정이 필요했다.

---

## 결정 1 — Guest upsert by phone_hmac

**선택**: `phone_hmac` UNIQUE 제약과 원자적 get-or-create를 사용해 동일 전화번호의 기존 Guest를 재사용한다.

**이유**:
- 재예약 시 guest row 중복 생성 방지
- 활성 `(slot_id, guest_id)` 쌍으로 중복 예약 차단이 가능해짐 (`BookingRepository.existsBookedBySlotIdAndGuestId`)
- 한 게스트의 예약 이력 조회가 단순해짐
- `guests.phone` 평문 컬럼 없이도 동일 전화번호 동등 검색을 유지할 수 있음
- 선조회와 INSERT 사이의 경쟁에서도 Guest 중복 생성을 DB가 최종 차단함

**트레이드오프**: 기존 Guest가 있으면 새 요청의 이름과 암호문으로 덮어쓰지 않고 기존 이력 소유자를 유지한다.

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

## 결정 5 — reserveCapacity를 GuestBookingService 트랜잭션 내에서 호출

**선택**: `@Transactional createGuestBooking()` 내에서 `SlotCapacitySupport.reserveCapacity(slotId)` 호출

**이유**: ADR-0003 준수. `booked_count` 증가와 Booking 생성이 별도 트랜잭션이면 정원 초과 롤백 시 booking row 고아 발생.

**구현**: `SlotCapacitySupport`는 `MANDATORY` 전파 속성으로 호출자 트랜잭션 참여를 강제한다.

---

## 결정 6 — GlobalExceptionHandler에 DataIntegrityViolationException 핸들러 추가

**선택**: 활성 회원/게스트 예약 UNIQUE 제약 위반만 409 `DUPLICATE_BOOKING`으로 반환한다.

**이유**: TOCTOU 경쟁 조건에서 애플리케이션 수준 중복 체크를 통과해도 DB UNIQUE 제약이 최후 방어선 역할을 해야 한다. 취소·완료·결석 예약은 UNIQUE 대상에서 제외해 취소 후 동일 슬롯 재예약을 허용한다.

**트레이드오프**: 예외 변환 계층이 Hibernate 제약 이름을 확인하므로 알려진 예약 제약 이름을 마이그레이션과 함께 유지해야 한다. 다른 FK/CHECK/UNIQUE 위반은 400 `INVALID_INPUT`으로 처리한다.

---

## 새 에러 코드

| 코드 | HTTP | 발생 상황 |
|------|------|-----------|
| `DUPLICATE_BOOKING` | 409 | 동일 전화번호 + 동일 슬롯 중복 예약 |
| `SLOT_NOT_AVAILABLE` | 409 | 비활성화된 슬롯 예약 시도 |
| `PHONE_VERIFICATION_FAILED` | 400 | 인증 코드 불일치 또는 만료 |
