import assert from "node:assert/strict";
import test from "node:test";

import {
  eventRefetchInterval,
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

test("가까운 시작 경계 직후 다시 조회하고 먼 경계는 최대 1분마다 확인한다", () => {
  assert.equal(eventRefetchInterval([event], start - 500), 1000);
  assert.equal(eventRefetchInterval([event], start - 30_000), 30_250);
  assert.equal(eventRefetchInterval([event], start - 120_000), 60_000);
});

test("알려진 다음 경계가 없어도 새 게시를 반영하도록 1분마다 다시 조회한다", () => {
  assert.equal(eventRefetchInterval([], end), 60_000);
  assert.equal(eventRefetchInterval([{ startAt: "invalid", endAt: "invalid" }], end), 60_000);
});
