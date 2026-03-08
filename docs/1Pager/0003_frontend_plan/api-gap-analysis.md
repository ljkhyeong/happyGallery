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
| **F2** | 진행 중 | API 의존 없음, 공통 셸/상태 UI 보강 단계 |
| **F3** | 진행 중 | 관리자 상품/슬롯 API 이미 존재 |
| **F4** | 완료 | 예약 조회/변경/취소 API 존재 |
| **F5** | 완료 | 공개 상품 목록/상세 API 존재 |
| **F6** | 완료 | 공개 클래스/슬롯 조회 + 예약 생성 API 존재 |
| **F7** | 완료 | 휴대폰 인증 기반 8회권 구매 API 존재 |
| **F8** | 진행 중 | 관리자 주문/패스/환불 API 이미 존재 |
| **F9** | 진행 중 | 사용자 주문 생성/조회 API 존재 |

## 4) 초기 API 갭 해소 현황

| GAP | 초기 문제 | 현재 상태 | 해소 계약 |
|-----|-----------|-----------|-----------|
| GAP-1 | 공개 상품 목록 API 부재 | 해소됨 | `GET /api/v1/products` |
| GAP-2 | 공개 클래스 목록 API 부재 | 해소됨 | `GET /api/v1/classes` |
| GAP-3 | 공개 슬롯 조회 API 부재 | 해소됨 | `GET /api/v1/slots?classId=&date=` |
| GAP-4 | `guestId` 획득 흐름 부재 | 해소됨 | `POST /api/v1/passes/purchase` |
| GAP-5 | 사용자 주문 생성/조회 API 부재 | 해소됨 | `POST /api/v1/orders`, `GET /api/v1/orders/{id}?token=` |

## 5) 현재 남은 프론트 관점 이슈

- API 공백보다는 화면 완성도 이슈가 남아 있다.
- 우선순위:
  - `F2` 공통 셸, 로딩/에러/empty 상태 보강
  - `F3` 관리자 상품/슬롯 UX 보강
  - `F8` 관리자 운영 화면 검증과 UX 보강
  - `F9` 사용자 주문 생성/조회 UX 보강

## 6) 결론

- B1에서 정리했던 프론트 선행 API 갭은 현재 모두 해소됐다.
- 현재 프론트 작업의 병목은 백엔드 계약 부족이 아니라 UI 완성도와 흐름 연결 마무리다.
- 최신 상태 판단은 이 문서와 `docs/1Pager/0003_frontend_plan/plan.md`, `HANDOFF.md`를 함께 기준으로 본다.
