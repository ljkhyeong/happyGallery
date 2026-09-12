import assert from "node:assert/strict";
import { once } from "node:events";
import test from "node:test";
import { createFrontendApp } from "../../server.mjs";

async function serve(t, key) {
  const app = createFrontendApp({
    indexNowKey: key,
    requestHandler: (_request, response) => response.status(404).send("not found"),
  });
  const server = app.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => new Promise((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve());
  }));
  return `http://127.0.0.1:${server.address().port}`;
}

test("IndexNow는 설정된 키 파일에만 평문 키를 반환하고 캐시하지 않는다", async (t) => {
  const origin = await serve(t, "local-test-indexnow-key");
  const response = await fetch(`${origin}/local-test-indexnow-key.txt`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type"), /^text\/plain; charset=utf-8$/);
  assert.equal(response.headers.get("cache-control"), "no-store");
  assert.equal(await response.text(), "local-test-indexnow-key");
  for (const path of ["/wrong-key.txt", "/products/local-test-indexnow-key.txt", "/robots.txt"]) {
    const other = await fetch(`${origin}${path}`);
    assert.equal(other.status, 404);
    assert.equal(await other.text(), "not found");
  }
});

test("키를 비우거나 교체하면 이전 IndexNow 키 파일을 제공하지 않는다", async (t) => {
  for (const key of ["", "replacement-indexnow-key"]) {
    const origin = await serve(t, key);
    const response = await fetch(`${origin}/local-test-indexnow-key.txt`);
    assert.equal(response.status, 404);
  }
});

test("잘못된 IndexNow 키는 경로로 등록하지 않고 값 없이 설정 오류를 알린다", () => {
  for (const key of ["short", "a".repeat(129), "secret/key*value", "a".repeat(8) + "\n"]) {
    assert.throws(() => createFrontendApp({ indexNowKey: key }), {
      message: "INDEXNOW_KEY는 영문·숫자·하이픈 8~128자여야 합니다.",
    });
  }
});
