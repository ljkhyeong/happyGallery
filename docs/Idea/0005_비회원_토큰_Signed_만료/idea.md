# Idea-0005: Guest Token — Signed Short-lived Token 전환

## 배경
ADR-0024에서 단기 조치(SHA-256 해시 저장)는 완료했지만, 전달 방식·만료·브라우저 저장 측면은 아직 개선 여지가 있다.

## 현재 vs 목표

| 항목 | 현재 (단기 완료) | 목표 |
|------|------------------|------|
| 전달 방식 | `X-Access-Token` 헤더 (query param 폐지 완료) | 유지 또는 `Authorization: Bearer` 전환 |
| 만료 | 없음 | 생성 후 7일 (또는 슬롯 종료 후 24시간) |
| 토큰 형식 | opaque 32자 hex (DB에 SHA-256 해시 저장) | signed JWT (HMAC-SHA256) with expiry claim |
| 갱신 | 없음 | refresh 없음 (단발성 guest 조작) |
| 브라우저 저장 | 성공 화면에서 1회 표시 + 복사 버튼, 조회 페이지 자동 연결 | httpOnly cookie 또는 sessionStorage |

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

## 영향
- 프론트엔드 전수 수정 필요 (API 계약 breaking change)
- 기존 발급 토큰 호환 정책 필요 (점진 전환 또는 일괄 무효화)

## 이미 완료된 항목
- `X-Access-Token` 헤더 전환 (T1-T3)
- SHA-256 해시 저장 + V18 backfill (T1-T2, T1-T5)
- 프론트엔드 성공 화면 1회 표시 + 복사 + 자동 연결 (T1-T4)
