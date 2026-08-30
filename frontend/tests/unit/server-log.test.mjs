import assert from "node:assert/strict";
import { once } from "node:events";
import test from "node:test";

import { createFrontendApp } from "../../server.mjs";

test("운영 프런트 서버는 결제 callback query를 access log에 남기지 않는다", async (t) => {
  const app = createFrontendApp({
    requestHandler: (_request, response) => response.status(200).send("ok"),
  });
  const server = app.listen(0, "127.0.0.1");
  await once(server, "listening");
  t.after(() => new Promise((resolve, reject) => {
    server.close((error) => error ? reject(error) : resolve());
  }));

  const output = [];
  const originalStdoutWrite = process.stdout.write;
  const originalStderrWrite = process.stderr.write;
  process.stdout.write = function captureStdout(chunk, ...args) {
    output.push(String(chunk));
    return originalStdoutWrite.call(this, chunk, ...args);
  };
  process.stderr.write = function captureStderr(chunk, ...args) {
    output.push(String(chunk));
    return originalStderrWrite.call(this, chunk, ...args);
  };

  try {
    const address = server.address();
    assert.equal(typeof address, "object");
    const response = await fetch(
      `http://127.0.0.1:${address.port}/payments/success?paymentKey=secret-payment-key&orderId=secret-order-id&amount=10000`,
    );
    assert.equal(response.status, 200);
    assert.equal(await response.text(), "ok");
  } finally {
    process.stdout.write = originalStdoutWrite;
    process.stderr.write = originalStderrWrite;
  }

  const logged = output.join("");
  assert.doesNotMatch(logged, /secret-payment-key/);
  assert.doesNotMatch(logged, /secret-order-id/);
});
