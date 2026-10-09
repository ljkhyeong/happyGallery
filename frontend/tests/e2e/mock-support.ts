import type { Route } from "@playwright/test";

/**
 * API 모의 응답을 JSON으로 보낸다. 실제 백엔드를 쓰는 준비 helper는 support.ts에 둔다.
 * Playwright는 json 값이 falsy이면 content-type을 붙이지 않으므로 직접 지정한다.
 */
export async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", json: body });
}
