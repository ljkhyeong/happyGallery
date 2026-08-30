import assert from "node:assert/strict";
import test from "node:test";

import { getUserMessage } from "../../src/shared/lib/errorMessages.ts";
import {
  CLASS_CATEGORY_OPTIONS,
  FULFILLMENT_TYPE_LABEL,
  getClassCategoryLabel,
  getStatusLabel,
  isPerfumeClassCategory,
  NOTIFICATION_EVENT_LABEL,
  PRODUCT_FULFILLMENT_LABEL,
  PRODUCT_TYPE_LABEL,
} from "../../src/shared/lib/labels.ts";

test("주문 상태는 고객과 관리자에게 각자의 행동 주체를 분명히 안내한다", () => {
  assert.equal(getStatusLabel("PAID_APPROVAL_PENDING"), "공방 승인 대기");
  assert.equal(getStatusLabel("PAID_APPROVAL_PENDING", "admin"), "승인 필요");
  assert.equal(getStatusLabel("DELAY_CONSENT_PENDING"), "지연 안내 확인 필요");
  assert.equal(getStatusLabel("DELAY_CONSENT_PENDING", "admin"), "고객 응답 대기");
  assert.equal(getStatusLabel("PICKUP_READY"), "매장에서 수령 가능");
  assert.equal(getStatusLabel("PICKUP_READY", "admin"), "고객 수령 대기");
});

test("결제 상태와 알 수 없는 상태는 내부 enum을 노출하지 않는다", () => {
  assert.equal(getStatusLabel("CONFIRMING"), "결제 결과 확인 중");
  assert.equal(getStatusLabel("FAILED"), "결제 실패");
  assert.equal(getStatusLabel("UNRECOGNIZED_SERVER_STATUS"), "상태 확인 필요");
  assert.equal(getStatusLabel("UNRECOGNIZED_SERVER_STATUS", "admin"), "상태 확인 필요");
});

test("예약 오류는 슬롯이나 충돌 대신 사용자가 할 일을 안내한다", () => {
  assert.equal(getUserMessage("DUPLICATE_BOOKING"), "같은 시간에 이미 예약이 있습니다.");
  assert.equal(
    getUserMessage("SLOT_NOT_AVAILABLE"),
    "선택한 시간은 더 이상 예약할 수 없습니다. 다른 시간을 선택해 주세요.",
  );
  assert.equal(
    getUserMessage("BOOKING_CONFLICT"),
    "예약을 완료할 수 없습니다. 예약 가능한 시간을 다시 확인한 뒤 시도해 주세요.",
  );
  assert.equal(
    getUserMessage("CONFLICT"),
    "요청을 완료할 수 없습니다. 안내된 조건을 확인하거나 잠시 후 다시 시도해 주세요.",
  );
});

test("회원과 결제 오류는 서비스에서 사용하는 쉬운 용어로 안내한다", () => {
  assert.equal(getUserMessage("PASS_EXPIRED"), "8회권이 만료되었습니다.");
  assert.equal(
    getUserMessage("SOCIAL_PROVIDER_ALREADY_LINKED"),
    "같은 소셜 로그인 서비스의 다른 계정이 이미 연결되어 있습니다.",
  );
  assert.equal(
    getUserMessage("PAYMENT_METHOD_NOT_ALLOWED"),
    "이 결제에 사용할 수 없는 결제 수단입니다. 다른 결제 수단을 선택해 주세요.",
  );
  assert.match(getUserMessage("ACCOUNT_WITHDRAWAL_BLOCKED"), /회수할 적립금/);
  assert.equal(
    getUserMessage("REFUND_NOT_ALLOWED"),
    "현재 이 항목은 환불할 수 없습니다. 환불 조건과 진행 상태를 확인해 주세요.",
  );
  assert.equal(getUserMessage("REVIEW_REPORT_ALREADY_EXISTS"), "이미 신고한 후기입니다.");
  assert.match(getUserMessage("PAYMENT_RESULT_RETENTION_EXPIRED"), /8회권 내역/);
});

test("클래스 카테고리는 알려진 종류를 고객이 읽을 수 있는 이름으로 표시한다", () => {
  assert.equal(getClassCategoryLabel("PERFUME"), "향수");
  assert.equal(getClassCategoryLabel(" resin "), "레진아트");
  assert.equal(getClassCategoryLabel("CERAMIC"), "CERAMIC");
  assert.equal(isPerfumeClassCategory(" perfume "), true);
  assert.deepEqual(
    CLASS_CATEGORY_OPTIONS.map(({ code }) => code),
    ["PERFUME", "RESIN", "WOOD", "KNIT", "LEATHER", "UPCYCLING"],
  );
});

test("상품과 알림 라벨은 내부 업무 용어 대신 고객 표현을 사용한다", () => {
  assert.equal(PRODUCT_TYPE_LABEL.READY_STOCK, "재고 상품");
  assert.equal(PRODUCT_TYPE_LABEL.MADE_TO_ORDER, "주문 제작 상품");
  assert.match(PRODUCT_FULFILLMENT_LABEL.MADE_TO_ORDER, /공방 승인 후/);
  assert.equal(FULFILLMENT_TYPE_LABEL.PICKUP, "매장 수령");
  assert.equal(NOTIFICATION_EVENT_LABEL.ORDER_CLAIM_RESOLVED, "환불·교환 요청 처리 결과");
  assert.equal(NOTIFICATION_EVENT_LABEL.REVIEW_HIDDEN, "후기 비공개 안내");
  assert.equal(NOTIFICATION_EVENT_LABEL.PRODUCT_QNA_ANSWERED, "상품 문의 답변");
});
