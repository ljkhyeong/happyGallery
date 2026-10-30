import assert from "node:assert/strict";
import test from "node:test";

import { parseGuestRecordNumber } from "../../src/features/guest-lookup/guestRecordNumber.ts";

test("비회원 조회 번호는 화면·알림 표기와 ORD-·BK- 번호, 앞자리 0을 숫자로 읽는다", () => {
  for (const value of ["123", " 0123 ", "#123", "주문 #123", "ORD-00000123", "ord-00000123"]) {
    assert.equal(parseGuestRecordNumber(value, "orders"), 123, value);
  }
  for (const value of ["45", "예약 #45", "BK-00000045", "bk-45"]) {
    assert.equal(parseGuestRecordNumber(value, "bookings"), 45, value);
  }
});

test("비회원 조회 번호는 숫자가 없거나 다른 종류 번호면 받지 않는다", () => {
  for (const value of ["", "  ", "ORD-", "0", "00000000", "9007199254740992"]) {
    assert.equal(parseGuestRecordNumber(value, "orders"), null, value);
  }
  assert.equal(parseGuestRecordNumber("BK-00000045", "orders"), null);
  assert.equal(parseGuestRecordNumber("예약 #45", "orders"), null);
  assert.equal(parseGuestRecordNumber("ORD-00000123", "bookings"), null);
  assert.equal(parseGuestRecordNumber("주문 #123", "bookings"), null);
});
