import type { BrowserContext, Route } from "@playwright/test";

/**
 * API 모의 응답을 JSON으로 보낸다. 실제 백엔드를 쓰는 준비 helper는 support.ts에 둔다.
 * Playwright는 json 값이 falsy이면 content-type을 붙이지 않으므로 직접 지정한다.
 */
export async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", json: body });
}

/** 고객 변경 요청이 /auth/csrf를 거치지 않고 보낼 CSRF 쿠키를 넣는다. 헤더 값을 단정할 때만 value를 넘긴다. */
export async function installCsrfCookie(
  context: BrowserContext,
  url: string | undefined,
  value = "e2e-csrf",
) {
  if (!url) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{ name: "XSRF-TOKEN", value, url }]);
}
