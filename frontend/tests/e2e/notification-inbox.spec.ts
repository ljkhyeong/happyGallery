import { expect, test } from "@playwright/test";

test("알림 전체 목록은 이전 페이지와 읽지 않은 필터를 제공하고 읽음 처리 후 목록을 갱신한다 @identity", async ({ page }) => {
  const rows = Array.from({ length: 22 }, (_, index) => ({ id: 22 - index, eventType: "ORDER_PAID", aggregateType: "ORDER", aggregateId: 42,
    deliveredAt: "2026-09-05T10:00:00", readAt: index === 0 ? "2026-09-05T11:00:00" : null, read: index === 0 }));
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const json = (body: unknown) => route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/auth/csrf") return route.fulfill({ contentType: "application/json", headers: { "Set-Cookie": "XSRF-TOKEN=inbox-test; Path=/" }, body: "{}" });
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "inbox@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: rows.filter((row) => !row.read).length });
    if (path === "/api/v1/me/notifications") {
      const filtered = rows.filter((row) => url.searchParams.get("unreadOnly") !== "true" || !row.read);
      const offset = Number(url.searchParams.get("page") ?? 0) * 20;
      return json(filtered.slice(offset, offset + 20));
    }
    if (path.endsWith("/read-all")) { rows.forEach((row) => { row.read = true; }); return route.fulfill({ status: 200, headers: { "Content-Length": "0" } }); }
    if (path.endsWith("/read")) { rows.find((row) => row.id === Number(path.split("/").at(-2)))!.read = true; return route.fulfill({ status: 200, headers: { "Content-Length": "0" } }); }
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    return json([]);
  });
  await page.goto("/my/notifications");
  await expect(page.getByRole("heading", { name: "전체 알림" })).toBeVisible();
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
