import assert from "node:assert/strict";
import test from "node:test";
import { refundContent, refundCouponMessage } from "../../src/features/refund/refundPresentation.ts";

test("환불 처리 중에는 쿠폰 상태만으로 재사용을 안내하지 않는다", () => {
  for (const status of ["REQUESTED", "PROCESSING", "RETRYABLE", "RECONCILIATION_REQUIRED", "FAILED"]) {
    assert.doesNotMatch(refundCouponMessage(status, "AVAILABLE"), /다시 사용할 수 있습니다/);
  }
});

test("환불이 완료돼도 만료되거나 이미 다시 사용한 쿠폰은 재사용 가능으로 안내하지 않는다", () => {
  assert.match(refundCouponMessage("SUCCEEDED", "AVAILABLE"), /다시 사용할 수 있습니다/);
  for (const state of ["EXPIRED", "RESERVED", "REDEEMED", "CANCELED", null, undefined]) {
    assert.doesNotMatch(refundCouponMessage("SUCCEEDED", state), /다시 사용할 수 있습니다/);
  }
});

test("쿠폰만 복원하는 환불은 0원 환불 완료로 안내하지 않는다", () => {
  assert.doesNotMatch(refundContent({ status: "SUCCEEDED", amount: 0 }).message, /0원/);
  assert.match(refundContent({ status: "SUCCEEDED", amount: 12000 }).message, /12,000/);
  assert.notEqual(refundContent({ status: "REQUESTED", amount: 12000 }).title,
    refundContent({ status: "SUCCEEDED", amount: 12000 }).title);
});
