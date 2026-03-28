# ADR-0024: Guest Access Token Hardening

## Status
Accepted

## Context
비회원(guest)이 예약/주문을 생성하면 access token을 발급받아 조회·변경·취소에 사용한다.
기존 구현은 UUID v4 원본을 DB에 평문 저장하고, GET/DELETE 요청에서 query parameter로 전달했다.

**식별된 리스크:**
1. DB 유출 시 모든 guest 예약/주문에 대한 접근 가능
2. Query parameter 전달로 access log, Referer 헤더, 브라우저 히스토리에 토큰 노출
3. 토큰 만료 없음 — 한번 노출 시 영구 접근
4. Booking(32자 hex) / Order(36자 UUID) 포맷 불일치
5. Order에 UNIQUE 제약 없음

## Decision

### 단기 (구현 완료)
**SHA-256 해시 저장 + 포맷 통일 + 전달 방식 변경.**

- `AccessTokenHasher` 유틸: `generate()` → 32자 hex, `hash()` → SHA-256 64자 hex
- DB에는 해시만 저장, 원본 토큰은 생성 응답에서 1회만 반환
- 조회/변경/취소 시 `X-Access-Token` 헤더로 토큰을 전달 (query param 폐지)
- 조회/변경/취소 시 입력 토큰을 해시하여 저장값과 비교
- Booking/Order 모두 동일한 32자 hex 포맷으로 통일
- Order에 UNIQUE INDEX 추가 (V17 마이그레이션)
- 기존 평문 토큰을 SHA-256 해시로 일괄 전환 (V18 backfill 마이그레이션)
- 프론트엔드 성공 화면에서 토큰 1회 표시 + 복사 버튼 제공, 조회 페이지 자동 연결

### 후속 (구현 완료)
**HMAC-SHA256 서명 + 만료 타임스탬프.**

- `AccessTokenSigner` 유틸: `sign(expiry, secretKey)` → 서명 토큰, `verify(token, secretKey, now)` → 클레임 검증
- 토큰 형식: `base64url(nonce:expiryEpochSeconds).base64url(hmac)` — nonce는 16바이트 랜덤 hex
- DB에는 nonce의 SHA-256 해시를 저장 (기존 VARCHAR(64) 컬럼 재사용, 스키마 변경 없음)
- 기본 만료: 7일 (`app.guest-token.expiry-hours: 168`)
- HMAC 비밀키: `app.guest-token.hmac-secret` 환경변수로 주입
- `GuestTokenService`가 발급(`issue()`)과 검증(`resolveTokenHash()`)을 담당
- **듀얼 모드 폴백**: 토큰에 `.` 구분자가 있으면 서명 검증 경로, 없으면 레거시 SHA-256 해시 경로

배경 및 설계 검토는 [Idea-0005](../../Idea/0005_비회원_토큰_Signed_만료/idea.md) 참조.

## Consequences
- DB 유출 시 원본 토큰 복원 불가 (SHA-256 단방향)
- V18 backfill로 기존 토큰도 해시 전환, 기존 사용자의 raw token은 그대로 유효
- query param에서 헤더로 전환하여 access log/Referer 노출 제거
- 포맷 통일로 향후 토큰 검증 로직 단순화
- HMAC 서명으로 토큰 위·변조 방지
- 만료 시간으로 노출된 토큰의 재사용 기간 제한 (7일)
- 듀얼 모드로 기존 레거시 토큰 하위 호환 유지
