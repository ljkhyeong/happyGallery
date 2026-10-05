import test from "node:test";
import assert from "node:assert/strict";
import { normalizePhone, toPhoneInputValue } from "../../src/shared/validation/phone.ts";

test("자동완성·복사로 들어온 국가번호 형식을 국내 휴대폰 번호로 바꾼다", () => {
  assert.equal(normalizePhone("+82 10-1234-5678"), "01012345678");
  assert.equal(normalizePhone("+82-10-123-4567"), "0101234567");
  assert.equal(normalizePhone("010-1234-5678"), "01012345678");
});

test("입력 칸은 하이픈이 섞인 자동완성 값을 자르지 않고 숫자 11자리까지만 남긴다", () => {
  assert.equal(toPhoneInputValue("010-1234-5678"), "01012345678");
  assert.equal(toPhoneInputValue("0101234567899"), "01012345678");
});
