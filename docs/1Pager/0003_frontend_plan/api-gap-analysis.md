# B1. 프론트 선행 API 갭 분석 및 해소 현황

## 1) 현재 공개 API 목록

| # | Method | Path | 용도 |
|---|--------|------|------|
| 1 | GET | `/api/v1/products` | 공개 상품 목록 조회 |
| 2 | GET | `/api/v1/products/{id}` | 공개 상품 상세 조회 |
| 3 | GET | `/api/v1/classes` | 공개 클래스 목록 조회 |
| 4 | GET | `/api/v1/slots?classId=&date=` | 공개 예약 가능 슬롯 조회 |
| 5 | POST | `/api/v1/bookings/phone-verifications` | 휴대폰 인증코드 발송 |
| 6 | POST | `/api/v1/bookings/guest` | 게스트 예약 생성 |
| 7 | GET | `/api/v1/bookings/{bookingId}?token=` | 비회원 예약 조회 |
| 8 | PATCH | `/api/v1/bookings/{bookingId}/reschedule` | 예약 변경 |
| 9 | DELETE | `/api/v1/bookings/{bookingId}?token=` | 예약 취소 |
| 10 | POST | `/api/v1/passes/guest` | 게스트 8회권 구매 (legacy) |
| 11 | POST | `/api/v1/passes/purchase` | 휴대폰 인증 기반 8회권 구매 |
| 12 | POST | `/api/v1/orders` | 사용자 주문 생성 |
| 13 | GET | `/api/v1/orders/{id}?token=` | 사용자 주문 상세 조회 |

## 2) 현재 관리자 API 목록

| # | Method | Path | 용도 |
|---|--------|------|------|
| 1 | GET | `/api/v1/admin/products` | 상품 목록 조회 |
| 2 | POST | `/api/v1/admin/products` | 상품 등록 |
| 3 | POST | `/api/v1/admin/slots` | 슬롯 생성 |
| 4 | PATCH | `/api/v1/admin/slots/{id}/deactivate` | 슬롯 비활성화 |
| 5 | POST | `/api/v1/admin/bookings/{bookingId}/no-show` | 결석 처리 |
| 6 | POST | `/api/v1/admin/orders/{id}/approve` | 주문 승인 |
| 7 | POST | `/api/v1/admin/orders/{id}/reject` | 주문 거절 |
| 8 | POST | `/api/v1/admin/orders/{id}/complete-production` | 제작 완료 |
| 9 | PATCH | `/api/v1/admin/orders/{id}/expected-ship-date` | 예상 출고일 설정 |
| 10 | POST | `/api/v1/admin/orders/{id}/delay` | 배송 지연 요청 |
| 11 | POST | `/api/v1/admin/orders/{id}/prepare-pickup` | 픽업 준비 |
| 12 | POST | `/api/v1/admin/orders/{id}/complete-pickup` | 픽업 완료 |
| 13 | POST | `/api/v1/admin/orders/expire-pickups` | 픽업 만료 배치 |
| 14 | POST | `/api/v1/admin/passes/expire` | 8회권 만료 배치 |
| 15 | POST | `/api/v1/admin/passes/{passId}/refund` | 8회권 전체 환불 |
| 16 | GET | `/api/v1/admin/refunds/failed` | 환불 실패 목록 |
| 17 | POST | `/api/v1/admin/refunds/{refundId}/retry` | 환불 재시도 |

## 3) 프론트 단위별 API 준비 상태

### API 기준으로 완료 또는 착수 가능

| 단위 | 상태 | 근거 |
|------|------|------|
| **F0** | 완료 | 워크스페이스와 Vite 설정 존재 |
| **F1** | 완료 | 공통 API 클라이언트/타입 계층 존재 |
| **F2** | 완료 | 앱 셸/테마/로딩·에러·empty 상태/404/토스트 안정화 |
| **F3** | 완료 | 관리자 상품/슬롯 화면 + 401/400 에러 구분 + 클래스 드롭다운 |
| **F4** | 완료 | 예약 조회/변경/취소 API 존재 |
| **F5** | 완료 | 공개 상품 목록/상세 API 존재 |
| **F6** | 완료 | 공개 클래스/슬롯 조회 + 예약 생성 API 존재 |
| **F7** | 완료 | 휴대폰 인증 기반 8회권 구매 API 존재 |
| **F8** | 완료 | 관리자 주문/패스/환불 화면 + 401 처리 보강 |
| **F9** | 완료 | 사용자 주문 생성/조회 + 총액 미리보기 |

## 4) 초기 API 갭 해소 현황

| GAP | 초기 문제 | 현재 상태 | 해소 계약 |
|-----|-----------|-----------|-----------|
| GAP-1 | 공개 상품 목록 API 부재 | 해소됨 | `GET /api/v1/products` |
| GAP-2 | 공개 클래스 목록 API 부재 | 해소됨 | `GET /api/v1/classes` |
| GAP-3 | 공개 슬롯 조회 API 부재 | 해소됨 | `GET /api/v1/slots?classId=&date=` |
| GAP-4 | `guestId` 획득 흐름 부재 | 해소됨 | `POST /api/v1/passes/purchase` |
| GAP-5 | 사용자 주문 생성/조회 API 부재 | 해소됨 | `POST /api/v1/orders`, `GET /api/v1/orders/{id}?token=` |

## 5) 현재 남은 프론트 관점 이슈

- 프론트 단위(F0–F9)는 모두 완료됐다.
- 후속 작업은 `docs/1Pager/0004_polish_plan/plan.md` 기준으로 진행한다.
- 우선순위:
  - `P3` 폼 검증 및 에러 UX 강화 마무리
  - `P6`–`P7` 관리자 운영 화면 보강 (예약 조회, 주문 목록)
  - `P4` 반응형 UI 및 접근성

## 6) 결론

- B1에서 정리했던 프론트 선행 API 갭은 현재 모두 해소됐다.
- 프론트 단위(F0–F9)는 모두 완료됐다.
- 후속은 polish plan(P1–P10) 기준으로 진행한다.
- 최신 상태 판단은 이 문서와 `docs/1Pager/0003_frontend_plan/plan.md`, `HANDOFF.md`를 함께 기준으로 본다.
