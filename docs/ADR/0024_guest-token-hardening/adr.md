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

향후 개선 방향은 [Idea-0005](../../Idea/0005_guest-token-signed-expiry/idea.md) 참조.

## Consequences
- DB 유출 시 원본 토큰 복원 불가 (SHA-256 단방향)
- V18 backfill로 기존 토큰도 해시 전환, 기존 사용자의 raw token은 그대로 유효
- query param에서 헤더로 전환하여 access log/Referer 노출 제거
- 포맷 통일로 향후 토큰 검증 로직 단순화
