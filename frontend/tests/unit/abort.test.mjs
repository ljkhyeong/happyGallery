import assert from "node:assert/strict";
import test from "node:test";

import { waitForPromiseWithSignal } from "../../src/shared/api/abort.ts";

test("한 대기 요청의 취소가 공유 작업과 다른 대기 요청을 취소하지 않는다", async () => {
  const firstController = new AbortController();
  const secondController = new AbortController();
  let resolveShared;
  const shared = new Promise((resolve) => {
    resolveShared = resolve;
  });

  const first = waitForPromiseWithSignal(shared, firstController.signal);
  const second = waitForPromiseWithSignal(shared, secondController.signal);
  firstController.abort();
  resolveShared("csrf-token");

  await assert.rejects(first, { name: "AbortError" });
  await assert.doesNotReject(async () => {
    assert.equal(await second, "csrf-token");
  });
});

test("이미 취소된 신호는 공유 작업을 기다리지 않고 즉시 실패한다", async () => {
  const controller = new AbortController();
  controller.abort();

  await assert.rejects(
    waitForPromiseWithSignal(Promise.resolve("unused"), controller.signal),
    { name: "AbortError" },
  );
});
