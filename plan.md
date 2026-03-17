# Plan

이 파일은 현재 활성 작업만 유지한다.
완료된 실행 계획은 별도 보관하지 않고 제거하며, 장기적으로 남겨야 하는 내용은 `README.md`, `HANDOFF.md`, `docs/PRD`, `docs/ADR`에 반영한다.

---

## Active Goal

전수점검에서 확인된 보안 취약점, 운영 오동작, 테스트 공백, 구조 불일치를 우선순위대로 정리한다.

핵심 목표:
- 공개 인증과 관리자 인증의 unsafe default 제거
- member/claim 이후에도 운영 기능이 깨지지 않도록 조회/배치 경로 정합화
- guest access token 취급을 안전한 형태로 재설계
- `me/*`, 환불/알림, 운영 필터 계층의 회귀 테스트 보강
- 현재 ADR과 실제 코드 경계의 불일치를 줄여 중간 상태 비용 해소

우선순위:
1. 외부 노출 보안 취약점 차단
2. 실제 운영 기능 누락 복구
3. 테스트 공백 보강
4. 구조 정리와 문서 동기화

---

## Track 1. Public/Auth Security Hardening

목표:
- OTP 응답 노출 제거
- 관리자 기본 자격/기본 API key 제거
- profile 오적용에 강한 기본 구성으로 전환
- Actuator 노출 정책 명시

| Task | 상태 | 범위 | 완료 기준 |
|------|------|------|-----------|
| `S1-T1` | done | 공개 휴대폰 인증 API | `SendVerificationResponse`에서 `code` 제거, 서버 로그 출력, local-only dev API 추가, 테스트 헬퍼 repository 직접 조회로 전환 |
| `S1-T2` | done | frontend 공개 인증 UX | `PhoneVerificationStep`에서 MVP 코드 표시 제거, 타입에서 `code` 필드 제거, E2E를 dev API 조회로 전환 |
| `S1-T3` | done | `booking/order/pass/claim` 인증 재사용부 | `VerifiedGuestResolver` 추출, booking/order/pass 3곳의 인증+guest upsert 중복 제거 |
| `S1-T4` | done | admin seed/migration | V16으로 기본 admin 삭제, `LocalAdminSeedService`로 local-only seed 전환, 테스트 `@BeforeEach`에서 자체 seed |
| `S1-T5` | done | admin auth config/filter | 기본값을 `enableApiKeyAuth=false`, `apiKey=""` 로 변경, local 프로필에서만 활성화, prod 누락 시 안전 |
| `S1-T6` | done | local-only admin hook | 전수 확인: 모든 Local* 클래스에 `@Profile("local")` 적용, 신규 `LocalPhoneVerificationController` 포함 |
| `S1-T7` | done | actuator/prometheus | management port를 기본 8081로 분리, local에서는 8080 유지, 노출 목록 health/info/metrics/prometheus 유지 |
| `S1-T8` | pending | 문서 | README/HANDOFF/ADR-0023/API 계약 문서에 인증/운영 기본값 변경 사항 반영 |

검증:
- 공개 OTP API 응답에 code 미포함 확인
- guest booking/order/pass/claim happy path 회귀 확인
- admin login/API 접근 정책이 local/prod에서 의도대로 동작하는지 확인
- actuator 접근 제어 정책 확인

---

## Track 2. Booking Query/Reminder Recovery

목표:
- member booking 과 claim 완료 booking 이 운영 조회/리마인드에서 빠지지 않도록 복구

| Task | 상태 | 범위 | 완료 기준 |
|------|------|------|-----------|
| `B1-T1` | done | admin 예약 조회 쿼리 | `findAllInRange` JOIN→LEFT JOIN FETCH, member/claimed booking 결과 포함 |
| `B1-T2` | done | admin 예약 응답 DTO | `bookerType`(GUEST/MEMBER) 추가, `guestName/Phone`→`bookerName/Phone` 변경, User batch fetch, 프론트 타입/UI 동기화 |
| `B1-T3` | done | reminder batch 쿼리/발송 | `findBookingsInRange` LEFT JOIN 전환, guest/member 분기 발송 (`notifyByGuestId`/`notifyByUserId`) |
| `B1-T4` | done | notification 진입점 | `notifyBookingGuest`/`notifyBookingUser` → `notifyBooker` 통합, cancel/reschedule/create 호출처 일괄 전환 |
| `B1-T5` | pending | 운영 검증 테스트 | member booking, claimed booking, guest booking 각각이 admin list 와 D-1/당일 reminder 대상이 되는지 IT 추가 |
| `B1-T6` | pending | 문서 | PRD/ADR/HANDOFF에 member/claim 이후 운영 처리 규칙 반영 |

검증:
- admin booking list 에 guest/member/claimed booking 모두 노출
- D-1/당일 reminder 가 guest/member 각각 적절한 채널로 기록
- claim 이후 예약이 운영 화면과 배치에서 누락되지 않음

---

## Track 3. Guest Token Hardening

목표:
- guest access token 을 URL/평문 저장 기반에서 더 안전한 계약으로 전환

| Task | 상태 | 범위 | 완료 기준 |
|------|------|------|-----------|
| `T1-T1` | done | 현행 계약 정리 | 예약/주문 token 사용 API와 프론트 호출 경로 전수 정리 |
| `T1-T2` | done | backend token 저장 방식 | AccessTokenHasher(SHA-256 해시 저장), V17 migration(orders 컬럼 확장+UNIQUE), 서비스/컨트롤러 전환 완료 |
| `T1-T3` | done | backend API 계약 | query param → `X-Access-Token` 헤더로 통일, RescheduleRequest에서 token 필드 제거, 프론트엔드 API 호출 동기화 |
| `T1-T4` | done | frontend guest 조회/변경 흐름 | 성공 화면에 일회성 안내+복사 버튼 추가, 성공→조회 네비게이션 시 state로 token+ID 전달, 조회 페이지 자동 fill+자동 조회 |
| `T1-T5` | done | migration/compat | V18 마이그레이션으로 기존 평문 토큰 일괄 SHA-256 해싱, CHAR_LENGTH<64 조건으로 멱등 처리 |
| `T1-T6` | done | 보안 문서 | API contract(query param→X-Access-Token 헤더, 토큰 형식/1회성 명시), PRD §8(토큰 보안 정책), ADR-0024(V18 backfill+헤더 전환), Idea-0005(완료 항목 반영), HANDOFF(ADR-0024 추가) |

결정 포인트:
- 단기: hash 저장 + 기존 UX 유지
- 중기: guest session 또는 signed short-lived token 도입

검증:
- guest 조회/취소/변경이 새 token 계약으로 동작
- DB에 raw token 이 남지 않음
- access log/support screenshot 에서 민감 토큰 노출이 줄어드는 구조 확인

---

## Track 4. Test Coverage Recovery

목표:
- 지금 구조에서 가장 위험한 회귀 지점을 직접 때리는 테스트를 추가

| Task | 상태 | 범위 | 완료 기준 |
|------|------|------|-----------|
| `Q1-T1` | done | `me/bookings` | MeBookingUseCaseIT: 생성/목록/상세/변경/취소/401 — 6 tests |
| `Q1-T2` | done | `me/orders` | MeOrderUseCaseIT: 생성/목록/상세/401 — 4 tests |
| `Q1-T3` | done | `me/passes` | MePassUseCaseIT: 구매/목록/상세/401 — 4 tests |
| `Q1-T4` | done | pass refund | PassCreditUsageUseCaseIT: slot bookedCount=0, BookingHistory 4건 assert 추가 |
| `Q1-T5` | done | 알림 사이드이펙트 | BookingReminderBatchUseCaseIT: member D-1 리마인드, guest+member 혼합 당일 리마인드 2 tests 추가 |
| `Q1-T6` | done | admin/member filter | AdminAuthFilterTest: /api/v1/me/ 통과, /api/v1/auth/ 통과 2 tests 추가 |
| `Q1-T7` | done | OTP/관리자 설정 | AdminAuthFilterTest: 기본값 enableApiKeyAuth=false 검증, 기본 설정 admin 접근 401 — 2 tests 추가 |

검증:
- 변경 범위별 최소 IT/UseCaseIT 확보
- 새 보안 정책과 운영 경로가 테스트로 고정

---

## Track 5. Architecture Convergence

목표:
- “헥사고날 전환 중간 상태”를 줄이고, controller/service/repository 책임을 다시 맞춘다

| Task | 상태 | 범위 | 완료 기준 |
|------|------|------|-----------|
| `A1-T1` | done | admin query 계층 | `AdminBookingQueryService`(날짜조회+User batch fetch), `AdminOrderQueryService`(목록+이력) 도입, 컨트롤러에서 infra 직접 의존 제거, `UserReaderPort.findAllById` 추가 |
| `A1-T2` | done | product 계층 | `ProductReaderPort` 확장 + `ProductStorePort`/`InventoryReaderPort`/`InventoryStorePort` 신설, `ProductPersistencePortAdapter`/`InventoryPersistencePortAdapter` 도입, 3개 서비스에서 infra repository 직접 의존 제거 |
| `A1-T3` | done | notification 계층 | `NotificationLogStorePort` 신설, `NotificationLogPersistencePortAdapter` (Reader+Store 통합) 도입, `NotificationService`에서 `NotificationLogRepository` 직접 의존 제거 |
| `A1-T4` | done | booking creation | `BookingCreationSupport` 추출 — 슬롯검증/락/8회권차감/예약금검증/저장+이력+알림 공통화, GuestBookingService·MemberBookingService에서 8개 의존성 제거 |
| `A1-T5` | done | verified guest resolver | S1-T3에서 `VerifiedGuestResolver` 추출 완료 (booking/order/pass 3곳 공통). claim은 의미론이 다름(user phone re-verification) — 별도 유지 적합 |
| `A1-T6` | done | order creation facade | `OrderCreationService.createMemberOrder()` 추가 + `resolveItemPrices()` 추출, `MeOrderController`에서 `ProductQueryService` 의존 제거 — 가격 조회를 서비스 레이어로 이동 |
| `A1-T7` | pending | refund boundary | `RefundExecutionService` 의 소속을 `booking` 밖으로 옮길지 결정하고 공용 환불 경계 정리 |
| `A1-T8` | pending | query performance | `ProductQueryService.listActiveProducts()` N+1 제거 |
| `A1-T9` | pending | guest claim service | `DefaultGuestClaimService` 책임 분리와 PII-safe logging 적용 |
| `A1-T10` | pending | ADR 동기화 | 실제 선택한 구조가 ADR-0021/0022/0023 과 맞지 않으면 ADR 갱신 |

원칙:
- 새 포트는 경계가 분명한 곳에만 만든다.
- controller 에서 가격 조회, 소유권 판정, aggregate 조합을 하지 않는다.
- `Default*` 구현체와 `*Adapter` 규칙을 계속 유지하되, 혼합 상태를 오래 두지 않는다.

---

## Execution Order

1. `Track 1`의 `S1-T1`~`S1-T7`
2. `Track 2`의 `B1-T1`~`B1-T5`
3. `Track 4`의 `Q1-T1`~`Q1-T7`
4. `Track 3`의 `T1-T1`~`T1-T6`
5. `Track 5`의 `A1-T1`~`A1-T10`
6. 각 track 종료 시 문서 동기화

이 순서를 택하는 이유:
- 외부 노출 취약점과 운영 누락을 먼저 막아야 한다.
- token 계약과 구조 리팩토링은 영향 범위가 넓으므로, 안전망 테스트를 먼저 늘려야 한다.
- ADR/문서 정리는 구현 방향이 확정된 뒤 한 번에 맞추는 편이 비용이 낮다.

---

## Rules

- 새 실행 계획은 이 파일에만 추가한다.
- 완료된 task는 체크 후 제거하거나 간단한 완료 메모만 남기고 정리한다.
- 장기 보관 가치가 있는 설계/요구사항/운영 정보는 `docs/ADR`, `docs/PRD`, `docs/Idea`, `docs/POC`, `HANDOFF.md`로 옮긴다.
