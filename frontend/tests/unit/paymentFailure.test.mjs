import test from "node:test";
import assert from "node:assert/strict";
import { paymentFailureMessage } from "../../src/features/payment/paymentFailure.ts";

test("허용된 결제 실패 코드를 한국어 안내로 변환한다", () => {
  assert.equal(
    paymentFailureMessage("PAY_PROCESS_CANCELED"),
    "결제창에서 결제를 취소했습니다.",
  );
  assert.equal(
    paymentFailureMessage("REJECT_CARD_COMPANY"),
    "카드사에서 결제를 승인하지 않았습니다. 카드사 또는 다른 결제 수단을 확인해 주세요.",
  );
});

test("알 수 없는 코드와 빈 코드는 일반 안내만 반환한다", () => {
  const expected = "결제가 완료되지 않았습니다. 잠시 후 다시 시도해 주세요.";
  assert.equal(paymentFailureMessage("UNTRUSTED_EXTERNAL_MESSAGE"), expected);
  assert.equal(paymentFailureMessage("__proto__"), expected);
  assert.equal(paymentFailureMessage(null), expected);
});
