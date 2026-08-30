import assert from "node:assert/strict";
import test from "node:test";

import {
  sanitizeTelemetryHeaders,
  sanitizeTelemetryPath,
  sanitizeTelemetryUrl,
} from "../../src/shared/lib/sentryUrl.ts";

test("관리자 검색 URL의 개인정보 query와 fragment를 제거한다", () => {
  assert.equal(
    sanitizeTelemetryUrl(
      "/api/v1/admin/bookings/search?keyword=01096355608#result",
      "https://happygallery.example",
    ),
    "https://happygallery.example/api/v1/admin/bookings/search",
  );
});

test("절대 URL도 경로만 보존한다", () => {
  assert.equal(
    sanitizeTelemetryUrl(
      "https://happygallery.example/payments/success?paymentKey=secret&orderId=42#done",
      "https://happygallery.example",
    ),
    "https://happygallery.example/payments/success",
  );
});

test("Sentry API 태그에는 query가 없는 상대 경로만 남긴다", () => {
  assert.equal(
    sanitizeTelemetryPath(
      "/admin/bookings/search?keyword=01096355608#result",
      "https://happygallery.example",
    ),
    "/admin/bookings/search",
  );
});

test("Referer는 경로만 남기고 인증 헤더는 제거한다", () => {
  assert.deepEqual(
    sanitizeTelemetryHeaders(
      {
        Referer:
          "https://happygallery.example/admin/orders?keyword=01096355608#result",
        Authorization: "Bearer secret",
        Cookie: "HG_SESSION=secret",
        "X-Access-Token": "guest-secret",
        "X-Payment-Status-Token": "payment-secret",
        "X-XSRF-TOKEN": "csrf-secret",
        Accept: "application/json",
      },
      "https://happygallery.example",
    ),
    {
      Referer: "https://happygallery.example/admin/orders",
      Accept: "application/json",
    },
  );
});
