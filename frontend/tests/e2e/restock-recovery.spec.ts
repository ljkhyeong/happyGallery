import { expect, test, type Page } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import type { RestockAlertResponse } from "../../src/generated/api/customerStore";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";
import { skipExternalFonts } from "./external-fonts";

test.beforeEach(skipExternalFonts);
test.afterEach(clearSsrUpstreamFixtures);

async function mockRestock(page: Page, { registered = false, withOptions = false } = {}) {
  const product: ProductDetailResponse = {
    id: 42, name: "재입고 확인 작품", description: null, category: "공예", type: withOptions ? "MADE_TO_ORDER" : "READY_STOCK",
    price: 12000, imageUrl: null, available: false, stockQuantity: 0, specification: null, careInstructions: null,
    productionLeadDays: null, optionGroups: [], variants: [],
  };
  if (withOptions) {
    product.optionGroups = [{ key: "color", name: "색상", type: "SELECT", required: true, sortOrder: 0,
      inputMaxLength: null, inputPlaceholder: null, inputPriceAdjustment: null,
      values: [{ key: "brown", name: "브라운", sortOrder: 0 }, { key: "blue", name: "블루", sortOrder: 1 }] }];
    product.variants = ["brown", "blue"].map((color, index) => ({ id: 801 + index, active: true,
      quantity: 0, priceAdjustment: 0, selections: [{ groupKey: "color", valueKey: color }] }));
  }
  const alert = (id: number): RestockAlertResponse => ({ id, productId: 42, productVariantId: null,
    productName: product.name, optionLabel: "기본 상품", status: "WAITING", createdAt: "2026-09-12T10:00:00", notifiedAt: null });
  const state = {
    accountId: 501, failRead: false, failWrite: false, holdWrite: false, finishAsNotified: false, release: () => {},
    reads: 0, writes: [] as Array<{ method: string; accountId: number; body: unknown; id: number | null }>,
    accounts: new Map<number, RestockAlertResponse[]>([[501, registered ? [alert(901)] : []], [502, [alert(1001)]]]),
  };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    const rows = state.accounts.get(state.accountId)!;
    if (pathname.startsWith("/api/v1/me/restock-alerts") && request.method() !== "GET") {
      const id = request.method() === "DELETE" ? Number(pathname.split("/").at(-1)) : null;
      const body = request.postDataJSON();
      state.writes.push({ method: request.method(), accountId: state.accountId, body, id });
      if (state.holdWrite) await new Promise<void>((resolve) => { state.release = resolve; });
      if (state.failWrite) return json({ code: "SERVICE_UNAVAILABLE" }, 503);
      if (request.method() === "POST") rows.push({ ...alert(901 + rows.length), productVariantId: body.productVariantId });
      else {
        const row = rows.find((item) => item.id === id)!;
        row.status = state.finishAsNotified ? "NOTIFIED" : "CANCELED";
      }
      return route.fulfill({ status: 204 });
    }
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({ contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=restock-recovery; Path=/" }, body: "{}" });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`, email: "restock@example.com",
        phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/me/restock-alerts": state.reads += 1; return state.failRead ? json({ code: "SERVICE_UNAVAILABLE" }, 503) : json(rows);
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/coupons": return json([]);
      case "/api/v1/me/rewards": return json({ availableBalance: 0, reservedBalance: 0, debtBalance: 0, history: [] });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/favorites/PRODUCT/42": return json({ saved: false });
      case "/api/v1/products/42": return route.fallback();
      case "/api/v1/products/42/qna/page":
      case "/api/v1/me/products/42/qna/page": return json({ content: [], hasMore: false, nextCursor: null });
      case "/api/v1/products/42/reviews": return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
        summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
      case "/api/v1/orders/policy": return json({ shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      default: throw new Error(`정의하지 않은 재입고 복구 테스트 요청: ${request.method()} ${pathname}`);
    }
  });
  return state;
}

test("신청·해지는 실패한 요청을 재시도하고 처리 후 조회 실패는 상태만 다시 확인한다", async ({ page }, testInfo) => {
  const state = await mockRestock(page);
  state.failRead = true;
  await page.goto("/products/42");
  const checkStatus = page.getByRole("button", { name: "신청 상태 다시 확인", exact: true });
  await expect(checkStatus).toBeEnabled();
  await expect(page.getByRole("button", { name: "재입고 알림 받기", exact: true })).toBeDisabled();
  state.failRead = false;
  await checkStatus.click();
  state.failWrite = true;
  await page.getByRole("button", { name: "재입고 알림 받기", exact: true }).click();
  const retryRegister = page.getByRole("button", { name: "알림 신청 다시 시도", exact: true });
  await expect(retryRegister).toBeEnabled();
  state.failWrite = false;
  state.failRead = true;
  await retryRegister.click();
  await expect(page.getByText("요청을 처리했습니다. 최신 신청 상태를 확인해 주세요.")).toBeVisible();
  await expect(retryRegister).toHaveCount(0);
  await expect(page.getByRole("button", { name: "신청 상태 확인 필요", exact: true })).toBeDisabled();
  expect(state.writes.map(({ method }) => method)).toEqual(["POST", "POST"]);
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 844 });
    await page.getByRole("button", { name: "신청 상태 확인 필요", exact: true }).locator("..").screenshot({
      path: testInfo.outputPath(`restock-status-${width}.png`), animations: "disabled",
    });
  }
  state.failRead = false;
  await checkStatus.click();
  const cancel = page.getByRole("button", { name: "재입고 알림 해지", exact: true });
  await expect(cancel).toBeEnabled();
  expect(state.writes).toHaveLength(2);
  state.failWrite = true;
  await cancel.click();
  const retryCancel = page.getByRole("button", { name: "알림 해지 다시 시도", exact: true });
  await expect(retryCancel).toBeEnabled();
  state.failWrite = false;
  await retryCancel.click();
  await expect(page.getByRole("button", { name: "재입고 알림 받기", exact: true })).toBeEnabled();
  expect(state.writes.map(({ method, id }) => [method, id])).toEqual([["POST", null], ["POST", null], ["DELETE", 901], ["DELETE", 901]]);
});

test("목록 해지 재시도는 같은 신청을 처리하고 조회 전에는 해지 상태를 단정하지 않는다", async ({ page }, testInfo) => {
  const state = await mockRestock(page, { registered: true });
  state.failWrite = true;
  await page.goto("/my/restock-alerts");
  const section = page.locator("#my-restock-alerts");
  await section.getByRole("button", { name: "알림 해지", exact: true }).click();
  const retry = page.getByRole("button", { name: "알림 해지 다시 시도", exact: true });
  await expect(retry).toBeEnabled();
  state.failWrite = false;
  state.failRead = true;
  state.finishAsNotified = true;
  await retry.click();
  await expect(section.getByText("해지 요청을 처리했습니다. 최신 신청 상태를 확인해 주세요.")).toBeVisible();
  await expect(section.getByText("상태 확인 필요", { exact: true })).toBeVisible();
  await expect(section.getByText("해지", { exact: true })).toHaveCount(0);
  await expect(section.getByRole("link", { name: "재입고 확인 작품", exact: true })).toBeVisible();
  await expect(section.getByRole("button", { name: "알림 해지", exact: true })).toBeDisabled();
  await page.setViewportSize({ width: 390, height: 844 });
  await section.screenshot({ path: testInfo.outputPath("restock-list-status-mobile.png"), animations: "disabled" });
  state.failRead = false;
  await page.getByRole("button", { name: "신청 상태 다시 확인", exact: true }).click();
  await expect(section.getByText("알림 완료", { exact: true })).toBeVisible();
  await expect(section.getByRole("button", { name: "알림 해지", exact: true })).toHaveCount(0);
  expect(state.writes.map(({ method, id }) => [method, id])).toEqual([["DELETE", 901], ["DELETE", 901]]);
});

test("품절 옵션을 바꾸면 이전 옵션의 신청 오류와 재시도 대상을 남기지 않는다", async ({ page }) => {
  const state = await mockRestock(page, { withOptions: true });
  await page.goto("/products/42?variantId=801");
  state.failWrite = true;
  await page.getByRole("button", { name: "재입고 알림 받기", exact: true }).click();
  await expect(page.getByRole("button", { name: "알림 신청 다시 시도", exact: true })).toBeEnabled();
  await page.getByRole("combobox", { name: /색상/ }).selectOption("blue");
  await expect(page.getByRole("button", { name: "알림 신청 다시 시도", exact: true })).toHaveCount(0);
  state.failWrite = false;
  await page.getByRole("button", { name: "재입고 알림 받기", exact: true }).click();
  await expect(page.getByRole("button", { name: "재입고 알림 해지", exact: true })).toBeEnabled();
  expect(state.writes.map(({ body }) => body)).toEqual([
    { productId: 42, productVariantId: 801 }, { productId: 42, productVariantId: 802 },
  ]);
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  await expect(page.getByRole("button", { name: "재입고 알림 받기", exact: true })).toBeEnabled();
});

for (const surface of ["목록", "상세"]) {
  test(`${surface}에서 클릭 직후 계정이 바뀌면 이전 알림 해지 요청을 전송하지 않는다`, async ({ page }) => {
    const state = await mockRestock(page, { registered: true });
    await page.goto(surface === "목록" ? "/my/restock-alerts" : "/products/42");
    const label = surface === "목록" ? "알림 해지" : "재입고 알림 해지";
    await expect(page.getByRole("button", { name: label, exact: true })).toBeEnabled();
    state.accountId = 502;
    await page.evaluate((buttonLabel) => {
      const button = Array.from(document.querySelectorAll<HTMLButtonElement>("button"))
        .find((element) => element.textContent?.trim() === buttonLabel);
      if (!button) throw new Error("알림 해지 버튼이 없습니다.");
      button.click();
      const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
      localStorage.setItem("hg_customer_session_boundary", value);
      window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
    }, label);
    await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: label, exact: true })).toBeEnabled();
    expect(state.writes).toEqual([]);
    await expect(page.getByRole("button", { name: "알림 해지 다시 시도", exact: true })).toHaveCount(0);
  });
}

test("계정 전환 후 도착한 이전 해지 응답은 새 계정의 알림 목록을 갱신하지 않는다", async ({ page }) => {
  const state = await mockRestock(page, { registered: true });
  await page.goto("/my/restock-alerts");
  state.holdWrite = true;
  await page.getByRole("button", { name: "알림 해지", exact: true }).click();
  await expect.poll(() => state.writes.length).toBe(1);
  await expect(page.getByRole("button", { name: "알림 해지", exact: true })).toBeDisabled();
  state.accountId = 502;
  await page.evaluate(() => {
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "알림 해지", exact: true })).toBeEnabled();
  const reads = state.reads;
  const completed = page.waitForResponse((response) => response.request().method() === "DELETE");
  state.release();
  await completed;
  await expect(page.getByText("입고 대기", { exact: true })).toBeVisible();
  expect(state.reads).toBe(reads);
  expect(state.accounts.get(502)![0]!.status).toBe("WAITING");
  await expect(page.getByRole("alert")).toHaveCount(0);
});
