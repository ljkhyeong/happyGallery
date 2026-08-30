import test from "node:test";
import assert from "node:assert/strict";
import { requireCheckoutTerms, tossCheckoutOptions } from "../../src/features/payment/checkoutSelection.ts";

test("네이버페이는 자체창을 현재 창에서 열고 기존 결제는 통합창을 유지한다", () => {
  assert.deepEqual(tossCheckoutOptions("NAVERPAY"), {
    card: { flowMode: "DIRECT", easyPay: "NAVERPAY" }, windowTarget: "self",
  });
  assert.deepEqual(tossCheckoutOptions(), {});
  assert.deepEqual(tossCheckoutOptions("DEFAULT"), {});
});

test("네이버페이 자체창에만 결제 약관 동의가 필요하다", () => {
  assert.throws(() => requireCheckoutTerms({ method: "NAVERPAY", termsAgreed: false }), /약관/);
  assert.doesNotThrow(() => requireCheckoutTerms({ method: "NAVERPAY", termsAgreed: true }));
  assert.doesNotThrow(() => requireCheckoutTerms({ method: "DEFAULT", termsAgreed: false }));
});
