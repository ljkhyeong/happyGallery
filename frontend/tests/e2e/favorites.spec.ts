import { expect, test } from "@playwright/test";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

test.afterEach(clearSsrUpstreamFixtures);

test("상품과 클래스를 찜하고 내 목록에서 종류를 골라 해제한다", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "XSRF-TOKEN", value: "favorite-csrf", url: baseURL! }]);
  const product = { id: 42, name: "찜할 작품", description: null, category: "공예", type: "READY_STOCK", price: 12000,
    imageUrl: null, available: true, stockQuantity: 10, specification: null, careInstructions: null,
    productionLeadDays: null, optionGroups: [], variants: [] };
  const bookingClass = { id: 42, name: "찜할 클래스", category: "WOOD", durationMin: 60, price: 30000,
    bufferMin: 30, capacity: 8, passEligible: true, description: null, imageUrl: null, preparationInfo: null, targetAudience: null };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product), ssrApiFixture("/classes/42", bookingClass));
  const saved = new Set<string>();
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const json = (body: unknown) => route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/products/42" || path === "/api/v1/classes/42") return route.fallback();
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "favorite@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
    if (path === "/api/v1/me/default-shipping-address") return json({ version: 0, shippingAddress: null });
    if (path.startsWith("/api/v1/me/favorites/")) {
      const type = path.split("/").at(-2)!;
      if (route.request().method() === "PUT") { saved.add(type); return route.fulfill({ status: 204 }); }
      if (route.request().method() === "DELETE") { saved.delete(type); return route.fulfill({ status: 204 }); }
      return json({ saved: saved.has(type) });
    }
    if (path === "/api/v1/me/favorites") {
      const type = url.searchParams.get("type");
      const rows = Array.from(saved).filter((value) => !type || value === type).map((value) => ({
        id: value === "PRODUCT" ? 1 : 2, targetType: value, targetId: 42, name: value === "PRODUCT" ? product.name : bookingClass.name,
        active: true, createdAt: "2026-09-05T10:00:00",
      }));
      return json({ content: rows, nextCursor: null, hasMore: false });
    }
    if (path === "/api/v1/me/group-inquiries" || path.endsWith("/page")) return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (path.endsWith("/reviews")) return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
      summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
    if (path === "/api/v1/orders/policy") return json({ shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
    if (path === "/api/v1/me/rewards") return json({ balance: 0 });
    return json([]);
  });
  await page.goto("/products/42");
  await page.getByRole("button", { name: "상품 찜하기", exact: true }).click();
  await expect(page.getByRole("button", { name: "상품 찜 해제", exact: true })).toHaveAttribute("aria-pressed", "true");
  await page.goto("/classes/42");
  await page.getByRole("button", { name: "클래스 찜하기", exact: true }).click();
  await page.getByRole("link", { name: "내 찜 보기", exact: true }).click();
  const section = page.locator("#my-favorites");
  await expect(section.getByRole("link", { name: product.name, exact: true })).toBeVisible();
  await expect(section.getByRole("link", { name: bookingClass.name, exact: true })).toBeVisible();
  await section.getByLabel("찜 종류").selectOption("CLASS");
  await expect(section.getByRole("link", { name: product.name, exact: true })).toHaveCount(0);
  await section.getByRole("button", { name: `${bookingClass.name} 찜 해제`, exact: true }).click();
  await expect(section.getByText("찜한 항목이 없습니다.", { exact: true })).toBeVisible();
  await section.getByLabel("찜 종류").selectOption("");
  await section.getByRole("link", { name: product.name, exact: true }).click();
  await page.getByRole("button", { name: "상품 찜 해제", exact: true }).click();
  await expect(page.getByRole("button", { name: "상품 찜하기", exact: true })).toHaveAttribute("aria-pressed", "false");
});
