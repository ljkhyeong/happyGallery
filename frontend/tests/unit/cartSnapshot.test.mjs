import assert from "node:assert/strict";
import test from "node:test";

import { isCartSnapshotConflict } from "../../src/features/cart/cartSnapshot.ts";

test("서버의 장바구니 버전 충돌 계약만 스냅샷 충돌로 분류한다", () => {
  assert.equal(
    isCartSnapshotConflict({
      status: 409,
      code: "CART_SNAPSHOT_CHANGED",
    }),
    true,
  );
});

test("다른 409 충돌은 장바구니 스냅샷 충돌로 오인하지 않는다", () => {
  assert.equal(
    isCartSnapshotConflict({
      status: 409,
      code: "CONFLICT",
      message: "재고가 부족합니다.",
    }),
    false,
  );
  assert.equal(
    isCartSnapshotConflict({
      status: 409,
      code: "BOOKING_CONFLICT",
      message: "장바구니가 변경되었습니다. 최신 장바구니를 확인한 뒤 다시 결제해 주세요.",
    }),
    false,
  );
});

test("상태나 전용 코드가 계약과 다르면 일반 오류로 유지한다", () => {
  assert.equal(
    isCartSnapshotConflict({
      status: 500,
      code: "CART_SNAPSHOT_CHANGED",
    }),
    false,
  );
  assert.equal(
    isCartSnapshotConflict({ status: 409, code: "CONFLICT" }),
    false,
  );
});

test("일반 오류는 장바구니 스냅샷 충돌로 분류하지 않는다", () => {
  assert.equal(isCartSnapshotConflict(new Error("network")), false);
  assert.equal(isCartSnapshotConflict(null), false);
});
