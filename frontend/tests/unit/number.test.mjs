import assert from "node:assert/strict";
import test from "node:test";

import { parsePositiveId } from "../../src/shared/lib/number.ts";

test("주소의 ID는 1 이상의 안전한 정수만 받는다", () => {
  assert.equal(parsePositiveId("42"), 42);
  assert.equal(parsePositiveId(" 7 "), 7);
  for (const value of [null, undefined, "", "0", "-3", "1.5", "abc", "9007199254740992"]) {
    assert.equal(parsePositiveId(value), null, String(value));
  }
});
