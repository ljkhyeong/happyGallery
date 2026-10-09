import type { BrowserContext, Page, Route } from "@playwright/test";

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

/** Toss SDK 대역이 requestPayment 요청을 쌓는 전역 이름. 직접 내려주는 SDK 스크립트도 이 이름을 쓴다. */
export const TOSS_REQUESTS_GLOBAL = "tossRequests";

/** 문서를 열 때마다 결제창 대신 requestPayment 요청만 기록하는 Toss SDK 대역을 설치한다. */
export async function installTossRequestRecorder(page: Page) {
  await page.addInitScript((globalName) => {
    const recorded = globalThis as unknown as Record<string, unknown[]>;
    const sdk = globalThis as unknown as { TossPayments: unknown };
    recorded[globalName] = [];
    sdk.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          recorded[globalName].push(request);
        },
      }),
    });
  }, TOSS_REQUESTS_GLOBAL);
}

/** installTossRequestRecorder가 기록한 requestPayment 요청을 읽는다. */
export function readTossRequests(page: Page): Promise<unknown[]> {
  return page.evaluate(
    (globalName) => (globalThis as unknown as Record<string, unknown[]>)[globalName],
    TOSS_REQUESTS_GLOBAL,
  );
}
