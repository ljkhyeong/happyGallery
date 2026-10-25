import { expect, test } from "@playwright/test";
import { fulfillJson, fulfillMemberCommon, memberFixture } from "./mock-support";

test("알림 전체 목록은 이전 페이지와 읽지 않은 필터를 제공하고 읽음 처리 후 목록을 갱신한다 @identity", async ({ page }) => {
  const orderTitle = `주문 #42 · 가죽 지갑 ${"LongProductName".repeat(8)} 외 1건`;
  const rows = Array.from({ length: 22 }, (_, index) => ({ id: 22 - index,
    eventType: index === 1 ? "BOOKING_CONFIRMED" : "ORDER_PAID", aggregateType: index === 1 ? "BOOKING" : "ORDER", aggregateId: index === 1 ? 701 : 42,
    contextTitle: index === 0 ? orderTitle : index === 1 ? "예약 #701 · 가죽공예" : null,
    scheduledAt: index === 1 ? "2026-09-12T14:00:00" : null,
    target: { orderId: index === 1 ? null : 42, bookingId: index === 1 ? 701 : null,
      productId: null, productVariantId: null, classId: null, slotId: null },
    deliveredAt: "2026-09-05T10:00:00", readAt: index === 0 ? "2026-09-05T11:00:00" : null, read: index === 0 }));
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (path === "/api/v1/auth/csrf") return route.fulfill({ headers: { "Set-Cookie": "XSRF-TOKEN=inbox-test; Path=/" }, json: {} });
    if (path === "/api/v1/me/notifications/unread-count") return fulfillJson(route, { count: rows.filter((row) => !row.read).length });
    if (await fulfillMemberCommon(route, { member: memberFixture({ email: "inbox@example.com" }) })) return;
    if (path === "/api/v1/me/notifications") {
      const filtered = rows.filter((row) => url.searchParams.get("unreadOnly") !== "true" || !row.read);
      const offset = Number(url.searchParams.get("page") ?? 0) * 20;
      return fulfillJson(route, filtered.slice(offset, offset + 20));
    }
    if (path.endsWith("/read-all")) { rows.forEach((row) => { row.read = true; }); return route.fulfill({ status: 200, headers: { "Content-Length": "0" } }); }
    if (path.endsWith("/read")) { rows.find((row) => row.id === Number(path.split("/").at(-2)))!.read = true; return route.fulfill({ status: 200, headers: { "Content-Length": "0" } }); }
    return fulfillJson(route, []);
  });
  await page.goto("/my/notifications");
  await expect(page.getByRole("heading", { name: "알림함", level: 1 })).toBeVisible();
  await expect(page.getByText(orderTitle, { exact: true })).toBeVisible();
  await expect(page.getByText("예약 #701 · 가죽공예", { exact: true })).toBeVisible();
  await expect(page.getByText(/현재 예약일:/)).toContainText("2026. 09. 12.");
  await expect(page.getByRole("link", { name: "예약 확정", exact: true })).toHaveAttribute("href", "/my/bookings/701");
  await expect(page.getByRole("link", { name: "주문 결제 완료", exact: true }).first()).toHaveAttribute("href", "/my/orders/42");
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.getByRole("button", { name: "알림", exact: true }).click();
  await expect(page.getByRole("dialog", { name: "알림 목록" }).getByText(orderTitle, { exact: true })).toBeVisible();
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect(page).toHaveURL(/page=1/);
  await expect(page.getByRole("button", { name: /알림 1 읽음 처리/ })).toBeVisible();
  await page.getByLabel("읽지 않은 알림만").click();
  await expect(page.getByLabel("읽지 않은 알림만")).toBeChecked();
  await expect(page).toHaveURL(/unreadOnly=true/);
  await expect(page.getByText("1페이지", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: /알림 21 읽음 처리/ }).click();
  await expect(page.getByRole("button", { name: /알림 21 읽음 처리/ })).toHaveCount(0);
  await page.reload();
  await expect(page.getByLabel("읽지 않은 알림만")).toBeChecked();
  await page.getByRole("button", { name: "모두 읽음", exact: true }).click();
  await expect(page.getByText("읽지 않은 알림이 없습니다.", { exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
});
