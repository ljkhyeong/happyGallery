import assert from "node:assert/strict";
import test from "node:test";

import {
  readSessionValue,
  removeSessionValues,
  writeSessionValue,
  writeSessionValues,
} from "../../src/shared/storage/browserSessionStorage.ts";

function withWindow(windowValue, operation) {
  const originalWindow = globalThis.window;
  globalThis.window = windowValue;
  try {
    operation();
  } finally {
    if (originalWindow === undefined) {
      delete globalThis.window;
    } else {
      globalThis.window = originalWindow;
    }
  }
}

test("sessionStorage 접근 자체가 거절되면 기본값과 실패 결과로 안전하게 종료한다", () => {
  withWindow({
    get sessionStorage() {
      throw new DOMException("blocked", "SecurityError");
    },
  }, () => {
    assert.equal(readSessionValue("oauth-return"), null);
    assert.equal(writeSessionValue("oauth-return", "/my"), false);
    assert.doesNotThrow(() => removeSessionValues("oauth-return", "oauth-intent"));
  });
});

test("한 callback key의 삭제 실패가 나머지 key 정리를 막지 않는다", () => {
  const values = new Map([
    ["blocked-key", "blocked"],
    ["next-key", "next"],
  ]);
  withWindow({
    sessionStorage: {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
      removeItem: (key) => {
        if (key === "blocked-key") {
          throw new DOMException("blocked", "SecurityError");
        }
        values.delete(key);
      },
    },
  }, () => {
    removeSessionValues("blocked-key", "next-key");
    assert.equal(values.get("blocked-key"), "blocked");
    assert.equal(values.has("next-key"), false);
  });
});

test("사용 가능한 sessionStorage는 기존 값을 그대로 읽고 쓴다", () => {
  const values = new Map();
  withWindow({
    sessionStorage: {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => values.set(key, value),
      removeItem: (key) => values.delete(key),
    },
  }, () => {
    assert.equal(writeSessionValue("oauth-return", "/orders"), true);
    assert.equal(readSessionValue("oauth-return"), "/orders");
    removeSessionValues("oauth-return");
    assert.equal(readSessionValue("oauth-return"), null);
  });
});

test("여러 continuation 중 하나의 저장이 실패하면 앞서 쓴 값도 되돌린다", () => {
  const values = new Map();
  withWindow({
    sessionStorage: {
      getItem: (key) => values.get(key) ?? null,
      setItem: (key, value) => {
        if (key === "intent") {
          throw new DOMException("blocked", "QuotaExceededError");
        }
        values.set(key, value);
      },
      removeItem: (key) => values.delete(key),
    },
  }, () => {
    assert.equal(writeSessionValues([
      ["owner", "101"],
      ["intent", "naver"],
    ]), false);
    assert.equal(values.has("owner"), false);
    assert.equal(values.has("intent"), false);
  });
});
