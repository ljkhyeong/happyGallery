import { expect, test } from "@playwright/test";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

test.afterEach(clearSsrUpstreamFixtures);

test("품절 상품의 재입고 알림을 신청하고 내 정보에서 해지한다", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "XSRF-TOKEN", value: "restock-csrf", url: baseURL! }]);
  const product = { id: 42, name: "재입고 확인 작품", description: null, category: "공예", type: "READY_STOCK", price: 12000,
    imageUrl: null, available: false, stockQuantity: 0, specification: null, careInstructions: null,
    productionLeadDays: null, optionGroups: [], variants: [] };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  let status: string | null = null;
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown, responseStatus = 200) => route.fulfill({ status: responseStatus, contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/products/42") return route.fallback();
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "restock@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/favorites") return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: 1 });
    if (path === "/api/v1/me/notifications") return json([{ id: 70, eventType: "PRODUCT_RESTOCK_AVAILABLE", aggregateType: "RESTOCK_ALERT", aggregateId: 901, read: true, readAt: "2026-09-05T10:00:00", deliveredAt: "2026-09-05T10:00:00" }]);
    if (path === "/api/v1/me/group-inquiries") return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (path === "/api/v1/me/restock-alerts") {
      if (route.request().method() === "POST") {
        expect(route.request().postDataJSON()).toEqual({ productId: 42, productVariantId: null });
        status = "WAITING";
        return route.fulfill({ status: 204 });
      }
      return json(status ? [{ id: 901, productId: 42, productVariantId: null, productName: product.name,
        optionLabel: "기본 상품", status, createdAt: "2026-09-05T10:00:00", notifiedAt: null }] : []);
    }
    if (path === "/api/v1/me/restock-alerts/901") {
      expect(route.request().method()).toBe("DELETE");
      status = "CANCELED";
      return route.fulfill({ status: 204 });
    }
    if (path.endsWith("/reviews")) return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
      summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
    if (path.endsWith("/page")) return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/orders/policy") return json({ shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
    return json([]);
  });
  await page.goto("/products/42");
  await page.getByRole("button", { name: "재입고 알림 받기", exact: true }).click();
  await expect(page.getByRole("button", { name: "재입고 알림 해지", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "알림", exact: true }).click();
  await page.getByRole("button", { name: /상품 재입고 안내/ }).click();
  await expect(page).toHaveURL(/\/my#my-restock-alerts$/);
  const section = page.locator("#my-restock-alerts");
  await expect(section.getByRole("link", { name: product.name })).toBeVisible();
  await section.getByRole("button", { name: "알림 해지", exact: true }).click();
  await expect(section.getByText("해지", { exact: true })).toBeVisible();
  await expect(section.getByRole("button", { name: "알림 해지" })).toHaveCount(0);
});
