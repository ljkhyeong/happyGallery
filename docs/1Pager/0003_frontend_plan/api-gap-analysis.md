# B1. 프론트 선행 API 갭 분석

## 1) 현재 공개 API 목록

| # | Method | Path | 용도 |
|---|--------|------|------|
| 1 | GET | `/api/v1/products/{id}` | 상품 상세 조회 |
| 2 | POST | `/api/v1/bookings/phone-verifications` | 휴대폰 인증코드 발송 |
| 3 | POST | `/api/v1/bookings/guest` | 게스트 예약 생성 |
| 4 | GET | `/api/v1/bookings/{bookingId}?token=` | 비회원 예약 조회 |
| 5 | PATCH | `/api/v1/bookings/{bookingId}/reschedule` | 예약 변경 |
| 6 | DELETE | `/api/v1/bookings/{bookingId}?token=` | 예약 취소 |
| 7 | POST | `/api/v1/passes/guest` | 게스트 8회권 구매 |

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

---

## 3) 프론트 단위별 API 매핑

### 바로 구현 가능 (기존 API로 충분)

| 단위 | 화면 | 사용하는 API | 비고 |
|------|------|-------------|------|
| **F0** | 스캐폴딩 | 없음 | 프로젝트 초기 설정 |
| **F1** | API 클라이언트 | 없음 | 공통 인프라 |
| **F2** | 앱 셸/테마 | 없음 | 공통 UI |
| **F3** | 관리자 상품/슬롯 | Admin #1~4 | 전부 존재 |
| **F4** | 예약 조회/변경/취소 | 공개 #4~6 | 전부 존재 |
| **F8** | 관리자 운영 확장 | Admin #5~17 | 전부 존재 |

### 백엔드 선행 작업 필요

| 단위 | 화면 | 부족한 API | 선행 단위 |
|------|------|-----------|----------|
| **F5** | 공개 상품 카탈로그 | 공개 상품 **목록** 조회 | B2 |
| **F6** | 예약 생성 | 공개 **클래스 목록** 조회, 공개 **슬롯 목록** 조회 (날짜/클래스 기준, 잔여 정원 포함) | B2 |
| **F7** | 8회권 구매 | `guestId` 획득 흐름 없음 (현재 API는 guestId를 직접 요구) | B3 |
| **F9** | 주문 생성/조회 | 사용자용 주문 생성/조회 API 전체 부재 | B4 |

---

## 4) API 갭 상세

### GAP-1: 공개 상품 목록 API 부재
- **현재**: `GET /api/v1/products/{id}` (상세만)
- **필요**: ACTIVE 상품 전체를 공개 목록으로 조회
- **관리자 API에는 존재**: `GET /api/v1/admin/products` — 그러나 `X-Admin-Key` 필요
- **해소 방향**: 공개 경로로 `GET /api/v1/products` 추가 (ACTIVE 필터, 페이징 선택)
- **영향 단위**: F5

### GAP-2: 공개 클래스 목록 API 부재
- **현재**: 클래스 관련 공개 API 없음
- **필요**: 예약 생성 화면에서 "어떤 클래스를 선택할 수 있는지" 조회
- **해소 방향**: `GET /api/v1/classes` 추가 (id, name, category, durationMin, price)
- **영향 단위**: F6

### GAP-3: 공개 슬롯 조회 API 부재
- **현재**: 슬롯 관련 공개 API 없음 (생성/비활성화는 관리자 전용)
- **필요**: 날짜/클래스 기준으로 예약 가능한 슬롯 목록 + 잔여 정원 조회
- **해소 방향**: `GET /api/v1/slots?classId={}&date={}` 추가 (active & 잔여 정원 > 0인 슬롯)
- **영향 단위**: F6

### GAP-4: guestId 획득 흐름 부재
- **현재**: `POST /api/v1/passes/guest`는 `guestId`를 직접 요구
- **문제**: 브라우저는 게스트의 DB ID를 알 수 없음
- **해소 방향** (후보):
  - A) 전화번호/이름 기반 구매로 변경 (phone + verificationCode로 게스트 특정)
  - B) 게스트 조회/생성 API 추가 (`POST /api/v1/guests` → guestId 반환)
  - C) 예약 생성 시 반환된 정보에서 guestId를 간접 획득
- **영향 단위**: F7, B3

### GAP-5: 사용자 주문 생성/조회 API 전체 부재
- **현재**: 주문 관련 공개 API 없음 (전부 관리자 전용)
- **필요**: 상품 주문 생성, 주문 상세 조회, 배송/픽업 상태 조회
- **해소 방향**: `POST /api/v1/orders`, `GET /api/v1/orders/{id}` 등 추가
- **영향 단위**: F9, B4

---

## 5) 선행 작업 → 프론트 단위 의존 그래프

```
B1 (이 문서) ─── 완료
 │
 ├─ F0 → F1 → F2 ─── API 불필요, 즉시 착수 가능
 │                │
 │                ├─ F3 ─── 관리자 API 전부 존재, 즉시 착수 가능
 │                └─ F4 ─── 공개 예약 API 전부 존재, 즉시 착수 가능
 │
 ├─ B2 (GAP-1,2,3 해소) → F5, F6
 ├─ B3 (GAP-4 해소) → F7
 └─ B4 (GAP-5 해소) → F8(관리자 운영 API 이미 존재), F9
```

---

## 6) 결론

- **지금 바로 프론트 착수 가능**: F0, F1, F2, F3, F4
- **B2 완료 후 착수**: F5, F6
- **B3 완료 후 착수**: F7
- **B4 완료 후 착수**: F9
- **F8은 관리자 API가 전부 존재**하므로 F2 이후 언제든 착수 가능
