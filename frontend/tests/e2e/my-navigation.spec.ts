import { expect, test } from "@playwright/test";

test("내 정보는 관리 화면을 열 때 해당 목록을 조회하고 이전 링크도 연결한다 @identity", async ({ page }) => {
  const requested: string[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    requested.push(path);
    const json = (body: unknown) => route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "menu@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
    if (path === "/api/v1/me/favorites" || path === "/api/v1/me/group-inquiries") return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/me/default-shipping-address") return json({ version: 0, shippingAddress: null });
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    return json([]);
  });
  await page.goto("/my");
  const menu = page.getByRole("navigation", { name: "내 정보 관리 메뉴" });
  await expect(menu).toBeVisible();
  expect(requested).not.toContain("/api/v1/me/favorites");
  expect(requested).not.toContain("/api/v1/me/default-shipping-address");
  expect(requested).not.toContain("/api/v1/me/group-inquiries");
  for (const [title, route] of [["내 찜", "favorites"], ["기본 배송지", "shipping-address"], ["재입고 알림 신청", "restock-alerts"], ["예약 빈자리 알림 신청", "vacancy-alerts"], ["단체 수업 문의", "group-inquiries"]]) {
    await menu.getByRole("link").filter({ hasText: title! }).click();
    await expect(page).toHaveURL(new RegExp(`/my/${route}$`));
    await expect(page.getByRole("heading", { name: title, exact: true }).first()).toBeVisible();
    await page.goBack();
    await expect(menu).toBeVisible();
  }
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
  await page.goto("/my#my-favorites");
  await expect(page).toHaveURL(/\/my\/favorites$/);
  await page.reload();
  await expect(page.getByRole("heading", { name: "내 찜", level: 1, exact: true })).toBeVisible();
});
