import test from "node:test";
import assert from "node:assert/strict";
import { requireCheckoutTerms, tossCheckoutOptions } from "../../src/features/payment/checkoutSelection.ts";

for (const method of ["NAVERPAY", "KAKAOPAY"]) {
  test(`${method}는 현재 창에서 자체 결제창을 연다`, () => {
    assert.deepEqual(tossCheckoutOptions(method), {
      card: { flowMode: "DIRECT", easyPay: method }, windowTarget: "self",
    });
  });

  test(`${method} 자체창에는 결제 약관 동의가 필요하다`, () => {
    assert.throws(() => requireCheckoutTerms({ method, termsAgreed: false }), /약관/);
    assert.doesNotThrow(() => requireCheckoutTerms({ method, termsAgreed: true }));
  });
}

test("기본 결제는 별도 약관 입력 없이 통합창을 유지한다", () => {
  assert.deepEqual(tossCheckoutOptions(), {});
  assert.deepEqual(tossCheckoutOptions("DEFAULT"), {});
  assert.doesNotThrow(() => requireCheckoutTerms({ method: "DEFAULT", termsAgreed: false }));
  assert.doesNotThrow(() => requireCheckoutTerms());
});
