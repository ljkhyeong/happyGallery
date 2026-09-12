import { expect, test, type Page, type Route } from "@playwright/test";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

const sdk = `
(() => {
  let sequence = 0;
  const widgets = new Map();
  window.turnstile = {
    render(container, options) {
      const id = String(++sequence);
      container.dataset.action = options.action;
      container.dataset.sitekey = options.sitekey;
      const solve = document.createElement('button');
      solve.type = 'button';
      solve.textContent = '자동 입력 방지 테스트 완료';
      solve.onclick = () => options.callback('token-' + options.sitekey + '-' + id);
      const expire = document.createElement('button');
      expire.type = 'button';
      expire.textContent = '자동 입력 방지 테스트 만료';
      expire.onclick = () => options['expired-callback']();
      container.replaceChildren(solve, expire);
      widgets.set(id, container);
      return id;
    },
    remove(id) { widgets.get(id)?.replaceChildren(); widgets.delete(id); },
    isExpired() { return false; }
  };
  const callback = new URL(document.currentScript.src).searchParams.get('onload');
  window[callback]?.();
})();`;

function json(route: Route, body: unknown, status = 200) {
  return route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function setup(page: Page, member = false) {
  const configuration = { siteKey: "test-site-key", unavailable: false };
  await page.route("https://challenges.cloudflare.com/turnstile/v0/api.js**", (route) =>
    route.fulfill({ contentType: "application/javascript", body: sdk }));
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/workshop") return route.fallback();
    if (path === "/api/v1/bot-protection") return configuration.unavailable
      ? json(route, { code: "SERVICE_UNAVAILABLE" }, 503)
      : json(route, { siteKey: configuration.siteKey });
    if (path === "/api/v1/me") return member
      ? json(route, { id: 101, name: "회원", phone: "01012345678", email: "member@example.com", phoneVerified: true })
      : json(route, { code: "UNAUTHORIZED", message: "로그인 필요" }, 401);
    if (path === "/api/v1/me/cart") return json(route, { cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json(route, { count: 0 });
    if (path === "/api/v1/policies/current") return json(route, {
      terms: { version: "2026-09-11-v1", documentPath: "/terms/2026-09-11-v1" },
      privacy: { version: "2026-09-12-v1", documentPath: "/privacy/2026-09-12-v1" },
    });
    return json(route, []);
  });
  return configuration;
}

async function reconnectAfter(page: Page, minutes: number) {
  await page.clock.setFixedTime(new Date(Date.UTC(2026, 8, 12, 0, minutes)));
  const response = page.waitForResponse("**/api/v1/bot-protection");
  await page.evaluate(() => {
    window.dispatchEvent(new Event("offline"));
    window.dispatchEvent(new Event("online"));
  });
  await response;
}

test.beforeEach(async ({ context, baseURL }) => {
  await context.addCookies([{ name: "XSRF-TOKEN", value: "bot-test-csrf", url: baseURL! }]);
  await replaceSsrUpstreamFixtures(ssrApiFixture("/workshop", { name: "해피갤러리" }));
});
test.afterEach(clearSsrUpstreamFixtures);

test("자동 입력 방지 키가 바뀌면 이전 토큰과 늦은 위젯 응답을 사용하지 않는다", async ({ page }) => {
  const configuration = await setup(page);
  const tokens: string[] = [];
  await page.route("**/api/v1/bookings/phone-verifications", (route) => {
    tokens.push(route.request().headers()["x-bot-token"]);
    return json(route, { phone: "01012345678", expiresAt: "2099-01-01T00:00:00" });
  });
  await page.clock.setFixedTime(new Date("2026-09-12T00:00:00Z"));
  await page.goto("/signup");
  const phone = page.getByLabel("휴대폰 번호", { exact: true });
  await phone.fill("01012345678");
  const send = page.getByRole("button", { name: "인증번호 발송", exact: true });
  const solve = page.getByRole("button", { name: "자동 입력 방지 테스트 완료" });
  const expire = page.getByRole("button", { name: "자동 입력 방지 테스트 만료" });
  await solve.click();
  await expect(send).toBeEnabled();
  const oldSolve = await solve.elementHandle();
  const oldExpire = await expire.elementHandle();
  await reconnectAfter(page, 2);
  await expect(send).toBeEnabled();

  configuration.siteKey = "new-site-key";
  await reconnectAfter(page, 4);
  await expect(page.locator('[data-action="phone_verification"]')).toHaveAttribute("data-sitekey", "new-site-key");
  await expect(send).toBeDisabled();
  await expect(phone).toHaveValue("01012345678");
  await oldSolve!.evaluate((button: HTMLButtonElement) => button.click());
  await expect(send).toBeDisabled();
  await solve.click();
  await expect(send).toBeEnabled();
  await oldSolve!.evaluate((button: HTMLButtonElement) => button.click());
  await oldExpire!.evaluate((button: HTMLButtonElement) => button.click());
  await expect(send).toBeEnabled();
  await send.click();
  await expect(page.getByLabel("인증번호", { exact: true })).toBeVisible();
  expect(tokens).toHaveLength(1);
  expect(tokens[0]).toMatch(/^token-new-site-key-/);
});

test("자동 입력 방지 설정 조회 장애에서 복구하면 같은 키라도 새 확인을 받는다", async ({ page }) => {
  const configuration = await setup(page);
  await page.clock.setFixedTime(new Date("2026-09-12T00:00:00Z"));
  await page.goto("/signup");
  const phone = page.getByLabel("휴대폰 번호", { exact: true });
  await phone.fill("01012345678");
  const send = page.getByRole("button", { name: "인증번호 발송", exact: true });
  const solve = page.getByRole("button", { name: "자동 입력 방지 테스트 완료" });
  await solve.click();
  await expect(send).toBeEnabled();
  configuration.unavailable = true;
  await reconnectAfter(page, 2);
  await expect(solve).toHaveCount(0);
  await expect(send).toBeDisabled();
  configuration.unavailable = false;
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(solve).toBeVisible();
  await expect(send).toBeDisabled();
  await expect(phone).toHaveValue("01012345678");
  await solve.click();
  await expect(send).toBeEnabled();
});

test("인증문자는 확인 후 발송하고 실패·재발송에는 새 토큰을 사용한다", async ({ page }) => {
  await setup(page);
  const tokens: string[] = [];
  await page.route("**/api/v1/bookings/phone-verifications", async (route) => {
    tokens.push(route.request().headers()["x-bot-token"]);
    expect(route.request().postDataJSON()).toEqual({ phone: "01012345678", purpose: "SIGNUP" });
    return tokens.length === 1 ? json(route, { code: "SERVICE_UNAVAILABLE" }, 503)
      : json(route, { phone: "01012345678", expiresAt: "2099-01-01T00:00:00" });
  });
  await page.goto("/signup");
  await page.getByLabel("휴대폰 번호", { exact: true }).fill("01012345678");
  const send = page.getByRole("button", { name: "인증번호 발송", exact: true });
  await expect(send).toBeDisabled();
  await expect(page.locator('[data-action="phone_verification"]')).toBeVisible();
  await page.getByRole("button", { name: "자동 입력 방지 테스트 완료" }).click();
  await expect(send).toBeEnabled();
  await page.getByRole("button", { name: "자동 입력 방지 테스트 만료" }).click();
  await expect(send).toBeDisabled();
  await page.getByRole("button", { name: "자동 입력 방지 테스트 완료" }).click();
  await send.click();
  await expect.poll(() => tokens.length).toBe(1);
  await expect(send).toBeDisabled();
  await expect(page.getByLabel("휴대폰 번호", { exact: true })).toHaveValue("01012345678");
  await page.getByRole("button", { name: "자동 입력 방지 테스트 완료" }).click();
  await send.click();
  await expect(page.getByLabel("인증번호", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "재발송", exact: true })).toBeDisabled();
  expect(tokens).toHaveLength(2);
  expect(tokens[0]).not.toBe(tokens[1]);
});

for (const member of [false, true]) {
  test(`${member ? "회원" : "비회원"} 단체 문의는 검증 실패 후 입력을 보존하고 새 토큰으로 재접수한다`, async ({ page }) => {
    await setup(page, member);
    const tokens: string[] = [];
    await page.route(`**/api/v1/${member ? "me/" : ""}group-inquiries`, (route) => {
      tokens.push(route.request().headers()["x-bot-token"]);
      expect(route.request().postDataJSON().organization).toBe("테스트 기관");
      return tokens.length === 1 ? json(route, { code: "INVALID_INPUT" }, 400)
        : json(route, { id: 99, status: "RECEIVED" }, 201);
    });
    await page.goto("/group-classes");
    const form = page.locator("#group-inquiry-form");
    for (const [label, value] of [
      ["기관·모임명", "테스트 기관"], ["담당자 이름", "테스트 담당"], ["담당자 휴대폰", "01012345678"],
      ["희망 일정", "10월 오전"], ["수업 장소", "공방"], ["관심 수업", "레진아트"], ["참여 인원", "20"],
    ]) await form.getByLabel(label, { exact: true }).fill(value);
    const submit = form.getByRole("button", { name: "단체 수업 문의 접수", exact: true });
    await expect(submit).toBeDisabled();
    await expect(form.locator('[data-action="group_inquiry"]')).toBeVisible();
    await form.getByRole("button", { name: "자동 입력 방지 테스트 완료" }).click();
    await submit.click();
    await expect.poll(() => tokens.length).toBe(1);
    await expect(submit).toBeDisabled();
    await expect(form.getByLabel("기관·모임명", { exact: true })).toHaveValue("테스트 기관");
    await form.getByRole("button", { name: "자동 입력 방지 테스트 완료" }).click();
    await submit.click();
    await expect(form.getByText("문의가 접수되었습니다.", { exact: true })).toBeVisible();
    expect(tokens[0]).not.toBe(tokens[1]);
  });
}

test("자동 입력 방지 스크립트가 실패하면 입력을 유지한 채 다시 불러온다", async ({ page }) => {
  await setup(page);
  let scripts = 0;
  await page.route("https://challenges.cloudflare.com/turnstile/v0/api.js**", (route) => {
    scripts++;
    return scripts === 1 ? route.abort() : route.fulfill({ contentType: "application/javascript", body: sdk });
  });
  await page.goto("/signup");
  await page.getByLabel("휴대폰 번호", { exact: true }).fill("01012345678");
  const retry = page.getByRole("button", { name: "자동 입력 방지 다시 확인" });
  await expect(retry).toBeVisible({ timeout: 20_000 });
  await retry.click();
  await page.getByRole("button", { name: "자동 입력 방지 테스트 완료" }).click();
  await expect(page.getByLabel("휴대폰 번호", { exact: true })).toHaveValue("01012345678");
  await expect(page.getByRole("button", { name: "인증번호 발송", exact: true })).toBeEnabled();
  expect(scripts).toBe(2);
});
