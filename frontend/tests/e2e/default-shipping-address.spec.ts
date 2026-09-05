import { expect, test } from "@playwright/test";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

test.afterEach(clearSsrUpstreamFixtures);

test("기본 배송지를 저장한 뒤 주문서에 불러오고 삭제한다", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "XSRF-TOKEN", value: "saved-address-csrf", url: baseURL! }]);
  const product = { id: 42, name: "주소 확인 작품", description: null, category: "공예", type: "READY_STOCK", price: 12000,
    imageUrl: null, available: true, stockQuantity: 10, specification: null, careInstructions: null,
    productionLeadDays: null, optionGroups: [], variants: [] };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  let address: Record<string, string | null> | null = null;
  let version = 0;
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    const json = (body: unknown) => route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/products/42") return route.fallback();
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "address@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/default-shipping-address") {
      if (route.request().method() === "PUT") {
        const body = route.request().postDataJSON();
        expect(body.version).toBe(version);
        address = body.shippingAddress;
        version += 1;
        return route.fulfill({ status: 204 });
      }
      if (route.request().method() === "DELETE") {
        expect(url.searchParams.get("version")).toBe(String(version));
        address = null; version += 1;
        return route.fulfill({ status: 204 });
      }
      return json({ version, shippingAddress: address });
    }
    if (path === "/api/v1/me/favorites") return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
    if (path === "/api/v1/me/group-inquiries" || path.endsWith("/page")) return json({ content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (path.endsWith("/reviews")) return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
      summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
    if (path === "/api/v1/orders/policy") return json({ shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
    if (path === "/api/v1/me/rewards") return json({ balance: 0 });
    return json([]);
  });
  await page.goto("/my/shipping-address");
  const section = page.locator("#my-default-shipping-address");
  await section.getByLabel("우편번호", { exact: true }).fill("12345");
  await section.getByLabel("기본 주소", { exact: true }).fill("서울시 저장 주소 10");
  await section.getByRole("button", { name: "기본 배송지 저장", exact: true }).click();
  await expect(section.getByRole("button", { name: "기본 배송지 삭제" })).toBeVisible();
  await page.goto("/products/42");
  await page.getByRole("button", { name: /택배 배송/ }).click();
  await page.getByLabel("기본 주소", { exact: true }).fill("입력 중인 주소");
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("입력 중인 주소");
  await page.getByRole("button", { name: "기본 배송지 불러오기", exact: true }).click();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("서울시 저장 주소 10");
  await page.goto("/my/shipping-address");
  await section.getByRole("button", { name: "기본 배송지 삭제", exact: true }).click();
  await expect(section.getByRole("button", { name: "기본 배송지 삭제" })).toHaveCount(0);
  await expect(section.getByLabel("기본 주소", { exact: true })).toHaveValue("");
});
