# Idea-0005: Guest Token — Signed Short-lived Token 전환

> **구현 완료** — HMAC-SHA256 서명 + 만료(30일) 토큰 발급·검증 전환 완료. 서비스 공개 전 서명 없는 토큰 호환 경로를 제거했으며, 만료·분실은 휴대폰 번호 인증 기반 복구로 처리한다. 현재 기준은 ADR-0024를 따른다.

## 배경
이 문서는 DB에 SHA-256 해시만 저장하던 초기 비회원 토큰을 만료 가능한 서명 토큰으로
전환하기 전에 작성한 검토 기록이다.

## 전환 전 vs 현재

| 항목 | 전환 전 | 현재 |
|------|------------------|------|
| 전달 방식 | `X-Access-Token` 헤더 | `X-Access-Token` 헤더 유지 |
| 만료 | 없음 | 생성 후 30일 |
| 토큰 형식 | opaque 32자 hex, DB에는 SHA-256 해시 저장 | HMAC-SHA256 서명과 만료 시각을 포함한 opaque 토큰, DB에는 해시 저장 |
| 갱신 | 없음 | refresh 없이 휴대폰 번호 인증으로 재발급 |
| 브라우저 저장 | 성공 화면에서 1회 표시·복사 | 소유자 경계가 있는 `sessionStorage`와 휴대폰 번호 인증 복구 |

## 고려사항
1. JWT를 쓰면 DB 조회 없이 서명 검증만으로 유효성 확인 가능
2. 다만 토큰 무효화(예: 예약 취소 후)가 필요하면 결국 DB 상태 확인 필요
3. ~~Query param → header 전환은 프론트엔드 API 호출 전수 수정 필요~~ → `X-Access-Token` 헤더 전환 완료
4. Cookie 기반 시 CORS, SameSite 정책 설정 필요
5. 기존 수동 복사 UX와의 호환성 (링크 공유 시나리오)

## 권장 접근
- Signed opaque token (HMAC-SHA256 + expiry timestamp) — JWT보다 가벼움
- Header 기반 전달로 access log 노출 제거
- 만료 시각을 토큰에 내장하되, 취소 상태는 DB에서 확인
- 프론트엔드에서 sessionStorage에 저장, 성공 화면에서 자동 세팅

## 당시 영향
- 프론트엔드 API 계약과 저장 방식을 함께 전환해야 했다.
- 서비스 공개 전이므로 서명 없는 기존 토큰 호환 경로는 두지 않았다.

## 이미 완료된 항목
- `X-Access-Token` 헤더 전환 (T1-T3)
- SHA-256 해시 저장 + V18 backfill (T1-T2, T1-T5)
- 프론트엔드 성공 화면 1회 표시 + 복사 + 자동 연결 (T1-T4)
- HMAC-SHA256 서명 + 만료 타임스탬프 토큰 발급 (`AccessTokenSigner`, `GuestTokenService`)
- 서명 없는 토큰 거절과 휴대폰 번호 인증 기반 복구
- `GuestTokenProperties` 설정 (`app.guest-token.hmac-secret`, 현재 키 `access-expiry: 720h`; 기존 `expiry-hours` 표기는 Duration 전환으로 대체됨)
