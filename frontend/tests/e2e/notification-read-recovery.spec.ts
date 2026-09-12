import { expect, test, type Page } from "@playwright/test";
import type { NotificationResponse } from "../../src/generated/api/notification";

const FAILURE_NOTICE = "알림을 읽음 처리하지 못했습니다. 알림 목록에서 다시 시도해 주세요.";

async function mockNotifications(page: Page) {
  const createRow = (id: number, title: string): NotificationResponse => ({
    id, contextTitle: title, eventType: "PRODUCT_RESTOCK_AVAILABLE", aggregateType: "RESTOCK_ALERT",
    aggregateId: id, read: false, readAt: null, scheduledAt: null, deliveredAt: "2026-09-12T10:00:00",
  });
  const accounts = new Map([
    [501, [createRow(1, "가죽 지갑"), createRow(2, "위빙 작품")]],
    [502, [createRow(101, "새 계정 알림")]],
  ]);
  const state = {
    accountId: 501, fail: false, hold: false, release: () => {},
    requests: [] as Array<number | undefined>,
  };
  await page.route("**/api/v1/**", async (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    const rows = accounts.get(state.accountId)!;
    if (pathname.endsWith("/read-all") || pathname.endsWith("/read")) {
      const id = pathname.endsWith("/read-all") ? undefined : Number(pathname.split("/").at(-2));
      state.requests.push(id);
      if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
      if (state.fail) return json({ code: "INTERNAL_ERROR" }, 503);
      rows.filter((row) => id === undefined || row.id === id).forEach((row) => { row.read = true; });
      return route.fulfill({ status: 204 });
    }
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({
        contentType: "application/json", headers: { "Set-Cookie": "XSRF-TOKEN=notification-read-test; Path=/" }, body: "{}",
      });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`,
        email: "notification@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: rows.filter((row) => !row.read).length });
      case "/api/v1/me/notifications": return json(rows.filter((row) => searchParams.get("unreadOnly") !== "true" || !row.read));
      case "/api/v1/me/restock-alerts": return json([]);
      default: throw new Error(`정의하지 않은 알림 테스트 요청: ${pathname}`);
    }
  });
  await page.goto("/my/notifications?unreadOnly=true");
  await expect(page.getByText("가죽 지갑", { exact: true })).toBeVisible();
  return state;
}

async function openPopover(page: Page) {
  await page.getByRole("button", { name: "알림", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "알림 목록" });
  await expect(dialog).toBeVisible();
  return dialog;
}

async function changeAccount(page: Page, clickBeforeChange = false) {
  await page.evaluate((clickFirst) => {
    if (clickFirst) {
      const button = Array.from(document.querySelectorAll<HTMLButtonElement>('[role="dialog"] button'))
        .find((element) => element.textContent?.trim() === "모두 읽음");
      if (!button) throw new Error("모두 읽음 버튼이 없습니다.");
      button.click();
    }
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  }, clickBeforeChange);
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
}

test("상단 모두 읽음은 처리 중 중복 클릭을 막고 실패한 요청을 재시도한다", async ({ page }, testInfo) => {
  const state = await mockNotifications(page);
  state.hold = true;
  state.fail = true;
  const dialog = await openPopover(page);
  await dialog.getByRole("button", { name: "모두 읽음", exact: true }).click();
  await expect(dialog.getByRole("button", { name: "읽음 처리 중..." })).toBeDisabled();
  await expect.poll(() => state.requests.length).toBe(1);
  state.hold = false;
  state.release();
  await expect(dialog.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("button", { name: "알림", exact: true })).toContainText("2");
  await page.screenshot({ path: testInfo.outputPath("popover-retry.png"), fullPage: true });
  state.fail = false;
  await dialog.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(dialog.getByRole("alert")).toHaveCount(0);
  await expect(dialog.getByRole("button", { name: "모두 읽음", exact: true })).toHaveCount(0);
  await expect(page.getByRole("button", { name: "알림", exact: true })).not.toContainText("2");
  expect(state.requests).toEqual([undefined, undefined]);
});

test("전체 알림에서 실패한 개별 읽음만 재시도하고 읽지 않은 목록을 갱신한다", async ({ page }, testInfo) => {
  const state = await mockNotifications(page);
  await page.setViewportSize({ width: 390, height: 844 });
  state.fail = true;
  await page.getByRole("button", { name: /알림 1 읽음 처리/ }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await page.screenshot({ path: testInfo.outputPath("inbox-mobile-retry.png"), fullPage: true });
  state.fail = false;
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(page.getByRole("button", { name: /알림 1 읽음 처리/ })).toHaveCount(0);
  await expect(page.getByRole("button", { name: /알림 2 읽음 처리/ })).toBeVisible();
  await expect(page).toHaveURL(/unreadOnly=true/);
  expect(state.requests).toEqual([1, 1]);
});

for (const surface of ["상단", "전체 목록"]) {
  test(`${surface} 알림에서 관련 화면으로 이동한 뒤 읽음 실패를 안내한다`, async ({ page }) => {
    const state = await mockNotifications(page);
    state.hold = true;
    state.fail = true;
    if (surface === "상단") {
      const dialog = await openPopover(page);
      await dialog.getByRole("button").filter({ hasText: "가죽 지갑" }).click();
    } else {
      await page.locator(".card").filter({ hasText: "가죽 지갑" }).getByRole("link").click();
    }
    await expect(page).toHaveURL(/\/my\/restock-alerts$/);
    await expect.poll(() => state.requests.length).toBe(1);
    state.release();
    await expect(page.getByText(FAILURE_NOTICE, { exact: true })).toBeVisible();
  });
}

test("계정 전환 뒤 도착한 이전 읽음 실패는 표시하지 않는다", async ({ page }) => {
  const state = await mockNotifications(page);
  state.hold = true;
  state.fail = true;
  const dialog = await openPopover(page);
  await dialog.getByRole("button").filter({ hasText: "가죽 지갑" }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  state.accountId = 502;
  await changeAccount(page);
  const failedResponse = page.waitForResponse((response) => response.url().endsWith("/notifications/1/read"));
  state.release();
  await failedResponse;
  const nextDialog = await openPopover(page);
  await expect(nextDialog.getByText("새 계정 알림", { exact: true })).toBeVisible();
  await expect(nextDialog.getByRole("button", { name: "모두 읽음", exact: true })).toBeEnabled();
  await expect(nextDialog.getByRole("alert")).toHaveCount(0);
  await expect(page.getByText(FAILURE_NOTICE, { exact: true })).toHaveCount(0);
});

test("읽음 클릭 직후 계정이 바뀌면 이전 요청을 전송하지 않는다", async ({ page }) => {
  const state = await mockNotifications(page);
  await openPopover(page);
  state.accountId = 502;
  await changeAccount(page, true);
  const nextDialog = await openPopover(page);
  await expect(nextDialog.getByText("새 계정 알림", { exact: true })).toBeVisible();
  await expect(nextDialog.getByRole("button", { name: "모두 읽음", exact: true })).toBeEnabled();
  expect(state.requests).toEqual([]);
});
