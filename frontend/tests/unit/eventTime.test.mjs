import assert from "node:assert/strict";
import test from "node:test";

import {
  eventTimingLabel,
  isEventAvailable,
  isEventOngoing,
} from "../../src/features/event/time.ts";

const event = {
  startAt: "2026-08-08T10:00:00",
  endAt: "2026-08-08T11:00:00",
};
const start = Date.parse("2026-08-08T10:00:00+09:00");
const end = Date.parse("2026-08-08T11:00:00+09:00");

test("이벤트 시작 시각은 진행 구간에 포함한다", () => {
  assert.equal(isEventOngoing(event, start), true);
  assert.equal(eventTimingLabel(event, start), "진행 중");
});

test("이벤트 종료 직전은 진행 중이지만 종료 시각은 즉시 종료로 판정한다", () => {
  assert.equal(isEventOngoing(event, end - 1), true);
  assert.equal(isEventAvailable(event, end - 1), true);
  assert.equal(isEventOngoing(event, end), false);
  assert.equal(isEventAvailable(event, end), false);
  assert.equal(eventTimingLabel(event, end), "종료");
});
