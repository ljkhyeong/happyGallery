import { expect, test, type Page } from "@playwright/test";
import type { ClassResponse } from "../../src/generated/api/booking";
import type { FavoriteResponse } from "../../src/generated/api/customerStore";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

test.afterEach(clearSsrUpstreamFixtures);

const bookingClass: ClassResponse = {
  id: 42, name: "가죽 카드지갑 수업", category: "LEATHER", durationMin: 90, price: 50000,
  bufferMin: 10, capacity: 8, passEligible: false, description: null, imageUrl: null,
  preparationInfo: null, targetAudience: null, status: "ACTIVE",
};
const favorites: FavoriteResponse[] = [
  { id: 2, targetType: "CLASS", targetId: 42, name: bookingClass.name, active: true, createdAt: "2026-09-12T10:00:00" },
  { id: 1, targetType: "PRODUCT", targetId: 11, name: "도자기 화병", active: false, createdAt: "2026-09-11T10:00:00" },
];

async function mockFavorites(page: Page) {
  await replaceSsrUpstreamFixtures(ssrApiFixture("/classes/42", bookingClass));
  const state = {
    accountId: 501, failWrite: false, failList: false, failStatus: false, failNextPage: false,
    paginated: false, hold: false, release: () => {},
    listReads: [] as Array<{ type: string | null; cursor: string | null }>,
    writes: [] as Array<{ method: string; target: string; accountId: number }>,
    accounts: new Map([
      [501, new Set(["CLASS:42", "PRODUCT:11"])],
      [502, new Set(["CLASS:42"])],
    ]),
  };
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname, searchParams } = new URL(request.url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    const saved = state.accounts.get(state.accountId)!;
    if (pathname.startsWith("/api/v1/me/favorites/")) {
      const target = pathname.split("/").slice(-2).join(":");
      const method = request.method();
      if (method === "GET") {
        return state.failStatus ? json({ code: "SERVICE_UNAVAILABLE" }, 503) : json({ saved: saved.has(target) });
      }
      state.writes.push({ method, target, accountId: state.accountId });
      if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
      if (state.failWrite) return json({ code: "SERVICE_UNAVAILABLE" }, 503);
      if (method === "PUT") saved.add(target);
      else if (method === "DELETE") saved.delete(target);
      else throw new Error(`지원하지 않는 찜 변경 요청: ${method}`);
      return route.fulfill({ status: 204 });
    }
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({ contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=favorites-recovery; Path=/" }, body: "{}" });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`,
        email: "favorite@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/favorites": {
        const type = searchParams.get("type");
        const cursor = searchParams.get("cursor");
        state.listReads.push({ type, cursor });
        if (state.failList || (cursor && state.failNextPage)) return json({ code: "SERVICE_UNAVAILABLE" }, 503);
        const rows = favorites.filter((item) => saved.has(`${item.targetType}:${item.targetId}`)
          && (!type || item.targetType === type));
        const hasMore = state.paginated && !cursor && rows.length > 1;
        return json({ content: state.paginated ? (cursor ? rows.slice(1) : rows.slice(0, 1)) : rows,
          hasMore, nextCursor: hasMore ? "next-page" : null });
      }
      case "/api/v1/classes/42": return route.fallback();
      case "/api/v1/classes/42/reviews": return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
        summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
      default: throw new Error(`정의하지 않은 찜 테스트 요청: ${pathname}`);
    }
  });
  return state;
}

test("다음 찜 목록 조회 실패는 현재 목록을 유지하고 실패한 페이지부터 재시도한다", async ({ page }, testInfo) => {
  const state = await mockFavorites(page);
  state.paginated = true;
  state.failNextPage = true;
  await page.goto("/my/favorites");
  await page.getByRole("button", { name: "찜 더 보기", exact: true }).click();
  const retry = page.getByRole("button", { name: "이어서 불러오기", exact: true });
  await expect(retry).toBeVisible();
  await expect(page.getByRole("link", { name: bookingClass.name, exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "찜 더 보기", exact: true })).toHaveCount(0);
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`favorites-retry-${width}.png`), fullPage: true, animations: "disabled" });
  }
  const readsBeforeRetry = state.listReads.length;
  state.failNextPage = false;
  await retry.click();
  await expect(page.getByText("도자기 화병 · 현재 이용할 수 없음", { exact: true })).toBeVisible();
  await expect(retry).toHaveCount(0);
  expect(state.listReads.slice(readsBeforeRetry)).toEqual([{ type: null, cursor: "next-page" }]);
  expect(state.writes).toEqual([]);
});

test("찜 해제 실패는 같은 항목을 다시 해제하고 재조회 실패로 되살리지 않는다", async ({ page }) => {
  const state = await mockFavorites(page);
  await page.goto("/my/favorites?type=CLASS");
  state.failWrite = true;
  state.hold = true;
  const remove = page.getByRole("button", { name: `${bookingClass.name} 찜 해제`, exact: true });
  await remove.click();
  await expect.poll(() => state.writes.length).toBe(1);
  await expect(remove).toBeDisabled();
  state.hold = false;
  state.release();
  await expect(page.getByRole("button", { name: "찜 해제 다시 시도", exact: true })).toBeVisible();
  state.failWrite = false;
  state.failList = true;
  await page.getByRole("button", { name: "찜 해제 다시 시도", exact: true }).click();
  await expect(page.getByRole("link", { name: bookingClass.name, exact: true })).toHaveCount(0);
  const reload = page.getByRole("button", { name: "목록 다시 불러오기", exact: true });
  await expect(reload).toBeEnabled();
  await expect(page.getByRole("button", { name: "찜 해제 다시 시도", exact: true })).toHaveCount(0);
  state.failList = false;
  await reload.click();
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect(page.getByText("찜한 항목이 없습니다.", { exact: true })).toBeVisible();
  expect(state.writes).toEqual([
    { method: "DELETE", target: "CLASS:42", accountId: 501 },
    { method: "DELETE", target: "CLASS:42", accountId: 501 },
  ]);
});

test("상세 화면은 실패한 찜 저장·해제를 재시도하고 상태 조회만 실패하면 조회만 재시도한다", async ({ page }) => {
  const state = await mockFavorites(page);
  state.accounts.get(501)!.delete("CLASS:42");
  await page.goto("/classes/42");
  state.failWrite = true;
  await page.getByRole("button", { name: "클래스 찜하기", exact: true }).click();
  await expect(page.getByRole("button", { name: "찜 저장 다시 시도", exact: true })).toBeVisible();
  state.failWrite = false;
  state.failStatus = true;
  await page.getByRole("button", { name: "찜 저장 다시 시도", exact: true }).click();
  await expect(page.getByRole("button", { name: "클래스 찜 해제", exact: true })).toHaveAttribute("aria-pressed", "true");
  const retryStatus = page.getByRole("button", { name: "찜 상태 다시 확인", exact: true });
  await expect(retryStatus).toBeEnabled();
  await expect(page.getByRole("button", { name: "찜 저장 다시 시도", exact: true })).toHaveCount(0);
  state.failStatus = false;
  await retryStatus.click();
  await expect(retryStatus).toHaveCount(0);
  state.failWrite = true;
  await page.getByRole("button", { name: "클래스 찜 해제", exact: true }).click();
  await expect(page.getByRole("button", { name: "찜 해제 다시 시도", exact: true })).toBeVisible();
  state.failWrite = false;
  await page.getByRole("button", { name: "찜 해제 다시 시도", exact: true }).click();
  await expect(page.getByRole("button", { name: "클래스 찜하기", exact: true })).toHaveAttribute("aria-pressed", "false");
  expect(state.writes.map(({ method }) => method)).toEqual(["PUT", "PUT", "DELETE", "DELETE"]);
});

for (const surface of ["목록", "상세"]) {
  test(`${surface}에서 찜 해제 직후 계정이 바뀌면 이전 요청을 전송하지 않는다`, async ({ page }) => {
    const state = await mockFavorites(page);
    await page.goto(surface === "목록" ? "/my/favorites" : "/classes/42");
    const label = surface === "목록" ? `${bookingClass.name} 찜 해제` : "클래스 찜 해제";
    await expect(page.getByRole("button", { name: label, exact: true })).toBeEnabled();
    state.accountId = 502;
    await page.evaluate((buttonLabel) => {
      const button = Array.from(document.querySelectorAll<HTMLButtonElement>("button"))
        .find((element) => element.getAttribute("aria-label") === buttonLabel || element.textContent?.trim() === buttonLabel);
      if (!button) throw new Error("찜 해제 버튼이 없습니다.");
      button.click();
      const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
      localStorage.setItem("hg_customer_session_boundary", value);
      window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
    }, label);
    await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: label, exact: true })).toBeEnabled();
    expect(state.writes).toEqual([]);
    await expect(page.getByRole("alert")).toHaveCount(0);
  });
}

test("계정 전환 뒤 도착한 이전 찜 해제 결과는 새 계정 목록을 바꾸지 않는다", async ({ page }) => {
  const state = await mockFavorites(page);
  await page.goto("/my/favorites?type=CLASS");
  state.hold = true;
  await page.getByRole("button", { name: `${bookingClass.name} 찜 해제`, exact: true }).click();
  await expect.poll(() => state.writes.length).toBe(1);
  state.accountId = 502;
  await page.evaluate(() => {
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
  const completed = page.waitForResponse((response) => response.request().method() === "DELETE");
  state.release();
  await completed;
  await expect(page.getByRole("link", { name: bookingClass.name, exact: true })).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0);
  expect(state.accounts.get(502)!.has("CLASS:42")).toBe(true);
});
