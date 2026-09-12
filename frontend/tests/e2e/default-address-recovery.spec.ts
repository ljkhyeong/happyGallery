import { expect, test, type Page } from "@playwright/test";
import type { DefaultShippingAddressResponse, ShippingAddress } from "../../src/generated/api/customerStore";

const originalAddress = {
  recipientName: "회원", phone: "01012345678", postalCode: "12345", addressLine1: "서울시 기존 주소 10", addressLine2: "101호",
} satisfies ShippingAddress;

async function mockAddress(page: Page) {
  const state = {
    accountId: 501, failRead: false, failWrite: false, hold: false, release: () => {}, readCount: 0,
    requests: [] as Array<{ method: string; version: number; shippingAddress: ShippingAddress | null }>,
    accounts: new Map<number, DefaultShippingAddressResponse>([
      [501, { version: 4, shippingAddress: originalAddress }],
      [502, { version: 0, shippingAddress: { ...originalAddress, recipientName: "새 회원", addressLine1: "새 회원 주소" } }],
    ]),
  };
  await page.route("**/api/v1/**", async (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({ contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=address-recovery; Path=/" }, body: "{}" });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`,
        email: "address@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/default-shipping-address": {
        const saved = state.accounts.get(state.accountId)!;
        const method = route.request().method();
        if (method === "GET") {
          state.readCount += 1;
          return state.failRead ? json({ code: "SERVICE_UNAVAILABLE" }, 503) : json(saved);
        }
        const change = method === "PUT" ? route.request().postDataJSON() : { version: Number(searchParams.get("version")), shippingAddress: null };
        state.requests.push({ method, ...change });
        if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
        if (state.failWrite) return json({ code: "SERVICE_UNAVAILABLE" }, 503);
        if (change.version !== saved.version) return json({ code: "CONFLICT" }, 409);
        saved.version += 1;
        saved.shippingAddress = change.shippingAddress;
        return route.fulfill({ status: 204 });
      }
      default: throw new Error(`정의하지 않은 주소 테스트 요청: ${pathname}`);
    }
  });
  await page.goto("/my/shipping-address");
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue(originalAddress.addressLine1);
  return state;
}

test("저장·삭제 실패는 해당 요청을 재시도하고 처리 중에는 주소 입력을 막는다", async ({ page }) => {
  const state = await mockAddress(page);
  await page.getByLabel("기본 주소", { exact: true }).fill("수정한 주소");
  state.hold = true;
  state.failWrite = true;
  await page.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  await expect(page.getByLabel("기본 주소", { exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "주소 검색" })).toBeDisabled();
  state.hold = false;
  state.release();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("수정한 주소");
  state.failWrite = false;
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect.poll(() => state.accounts.get(501)?.version).toBe(5);
  state.failWrite = true;
  await page.getByRole("button", { name: "기본 배송지 삭제", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  state.failWrite = false;
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(page.getByRole("button", { name: "기본 배송지 삭제", exact: true })).toHaveCount(0);
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("");
  expect(state.requests.map(({ method, version }) => [method, version])).toEqual([["PUT", 4], ["PUT", 4], ["DELETE", 5], ["DELETE", 5]]);
  expect(state.requests[0]).toEqual(state.requests[1]);
});

test("재연결로 최신 주소를 조회해도 입력을 유지하고 사용자가 불러올 때만 교체한다", async ({ page }, testInfo) => {
  const state = await mockAddress(page);
  await page.clock.install();
  await page.getByLabel("기본 주소", { exact: true }).fill("아직 저장하지 않은 주소");
  state.accounts.set(501, { version: 5, shippingAddress: { ...originalAddress, addressLine1: "다른 화면의 주소" } });
  await page.clock.fastForward(31_000);
  await page.evaluate(() => window.dispatchEvent(new Event("offline")));
  await page.evaluate(() => window.dispatchEvent(new Event("online")));
  await expect.poll(() => state.readCount).toBe(2);
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("아직 저장하지 않은 주소");
  await expect(page.getByRole("button", { name: "기본 배송지 저장", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "기본 배송지 삭제", exact: true })).toBeDisabled();
  for (const [name, width] of [["desktop", 1280], ["mobile", 390]] as const) {
    await page.setViewportSize({ width, height: 844 });
    await page.screenshot({ path: testInfo.outputPath(`address-changed-${name}.png`), fullPage: true, animations: "disabled" });
  }
  await page.getByRole("button", { name: "최신 주소 불러오기", exact: true }).click();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("다른 화면의 주소");
  await expect(page.getByRole("alert")).toHaveCount(0);
  expect(state.requests).toEqual([]);
});

test("버전 충돌과 최신 주소 조회 실패 뒤에도 입력을 유지하고 최신 버전으로 저장한다", async ({ page }) => {
  const state = await mockAddress(page);
  await page.getByLabel("기본 주소", { exact: true }).fill("저장하려던 주소");
  state.accounts.set(501, { version: 5, shippingAddress: { ...originalAddress, addressLine1: "다른 화면의 주소" } });
  await page.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  const load = page.getByRole("button", { name: "최신 주소 불러오기", exact: true });
  await expect(load).toBeVisible();
  state.failRead = true;
  await load.click();
  await expect(page.getByRole("button", { name: "다시 시도", exact: true })).toBeVisible();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("저장하려던 주소");
  state.failRead = false;
  await load.click();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("다른 화면의 주소");
  await page.getByLabel("기본 주소", { exact: true }).fill("최종 저장 주소");
  await page.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  await expect.poll(() => state.accounts.get(501)?.version).toBe(6);
  expect(state.requests.map((request) => request.version)).toEqual([4, 5]);
  expect(state.accounts.get(501)?.shippingAddress?.addressLine1).toBe("최종 저장 주소");
});

test("저장 성공 후 조회만 실패하면 입력을 유지하고 재조회 뒤 새 버전으로 수정한다", async ({ page }) => {
  const state = await mockAddress(page);
  await page.getByLabel("기본 주소", { exact: true }).fill("저장에 성공한 주소");
  state.failRead = true;
  await page.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  await expect(page.getByRole("button", { name: "다시 시도", exact: true })).toBeVisible();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("저장에 성공한 주소");
  expect(state.requests).toHaveLength(1);
  state.failRead = false;
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "최신 주소 불러오기", exact: true })).toHaveCount(0);
  await page.getByLabel("기본 주소", { exact: true }).fill("다시 수정한 주소");
  await page.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  await expect.poll(() => state.accounts.get(501)?.version).toBe(6);
  expect(state.requests.map((request) => request.version)).toEqual([4, 5]);
});

test("계정 전환 후 이전 주소 저장 결과는 새 계정의 입력과 안내를 바꾸지 않는다", async ({ page }) => {
  const state = await mockAddress(page);
  await page.getByLabel("기본 주소", { exact: true }).fill("이전 회원의 수정 주소");
  state.hold = true;
  await page.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  state.accountId = 502;
  await page.evaluate(() => {
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("새 회원 주소");
  const response = page.waitForResponse((res) => res.request().method() === "PUT" && res.url().endsWith("/default-shipping-address"));
  state.release();
  await response;
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("새 회원 주소");
  await expect(page.getByText("기본 배송지를 저장했습니다.", { exact: true })).toHaveCount(0);
  await expect(page.getByRole("alert")).toHaveCount(0);
});
