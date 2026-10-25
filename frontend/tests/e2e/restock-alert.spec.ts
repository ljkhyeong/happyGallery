import { expect, test } from "@playwright/test";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";
import {
  EMPTY_REVIEW_PAGE,
  fulfillJson,
  fulfillMemberCommon,
  installCsrfCookie,
  memberFixture,
  productDetailFixture,
} from "./mock-support";

test.afterEach(clearSsrUpstreamFixtures);

test("품절 상품의 재입고 알림을 신청하고 내 정보에서 해지한다", async ({ page, context, baseURL }) => {
  await installCsrfCookie(context, baseURL);
  const product = productDetailFixture({ name: "재입고 확인 작품", category: "공예", available: false, stockQuantity: 0 });
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  let status: string | null = null;
  const member = memberFixture({ email: "restock@example.com" });
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/products/42") return route.fallback();
    if (path === "/api/v1/me/notifications/unread-count") return fulfillJson(route, { count: 1 });
    if (await fulfillMemberCommon(route, { member })) return;
    if (path === "/api/v1/me/favorites") return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/me/notifications") return fulfillJson(route, [{ id: 70, eventType: "PRODUCT_RESTOCK_AVAILABLE", aggregateType: "RESTOCK_ALERT", aggregateId: 901, read: true, readAt: "2026-09-05T10:00:00", deliveredAt: "2026-09-05T10:00:00" }]);
    if (path === "/api/v1/me/group-inquiries") return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/me/restock-alerts") {
      if (route.request().method() === "POST") {
        expect(route.request().postDataJSON()).toEqual({ productId: 42, productVariantId: null });
        status = "WAITING";
        return route.fulfill({ status: 204 });
      }
      return fulfillJson(route, status ? [{ id: 901, productId: 42, productVariantId: null, productName: product.name,
        optionLabel: "기본 상품", status, createdAt: "2026-09-05T10:00:00", notifiedAt: null }] : []);
    }
    if (path === "/api/v1/me/restock-alerts/901") {
      expect(route.request().method()).toBe("DELETE");
      status = "CANCELED";
      return route.fulfill({ status: 204 });
    }
    if (path.endsWith("/reviews")) return fulfillJson(route, EMPTY_REVIEW_PAGE);
    if (path.endsWith("/page")) return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/orders/policy") return fulfillJson(route, { shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
    return fulfillJson(route, []);
  });
  await page.goto("/products/42");
  await page.getByRole("button", { name: "재입고 알림 받기", exact: true }).click();
  await expect(page.getByRole("button", { name: "재입고 알림 해지", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "알림", exact: true }).click();
  await page.getByRole("button", { name: /작품 재입고 안내/ }).click();
  await expect(page).toHaveURL(/\/my\/restock-alerts$/);
  const section = page.locator("#my-restock-alerts");
  await expect(section.getByRole("link", { name: product.name })).toBeVisible();
  await section.getByRole("button", { name: "알림 해지", exact: true }).click();
  await expect(section.getByText("해지", { exact: true })).toBeVisible();
  await expect(section.getByRole("button", { name: "알림 해지" })).toHaveCount(0);
});

test("휴대폰 인증이 없는 회원은 내 정보에서 번호를 등록한 뒤 같은 작품으로 돌아온다", async ({ page, context, baseURL }) => {
  await installCsrfCookie(context, baseURL);
  const product = productDetailFixture({ name: "번호 인증 후 알림 작품", category: "공예", available: false, stockQuantity: 0 });
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  const member = memberFixture({ id: 502, email: "restock-phone@example.com", phone: null, phoneVerified: false });
  let phoneUpdate: unknown = null;
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/products/42") return route.fallback();
    if (await fulfillMemberCommon(route, { member })) return;
    if (path === "/api/v1/me/phone") {
      phoneUpdate = route.request().postDataJSON();
      Object.assign(member, { phone: "01055556666", phoneVerified: true });
      return fulfillJson(route, member);
    }
    if (path === "/api/v1/bookings/phone-verifications") return fulfillJson(route, { phone: "01055556666", verificationId: 801 });
    if (path === "/api/v1/me/favorites") return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path.endsWith("/reviews")) return fulfillJson(route, EMPTY_REVIEW_PAGE);
    if (path.endsWith("/page")) return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/orders/policy") return fulfillJson(route, { shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
    return fulfillJson(route, []);
  });

  await page.goto("/products/42");
  await page.getByRole("link", { name: "휴대폰 인증 후 재입고 알림 받기", exact: true }).click();
  await expect(page).toHaveURL(/\/my$/);
  const dialog = page.getByRole("dialog", { name: "휴대폰 번호 등록" });
  await dialog.getByLabel("휴대폰 번호").fill("01055556666");
  await dialog.getByRole("button", { name: "인증번호 발송", exact: true }).click();
  await dialog.getByLabel("인증번호").fill("123456");
  await dialog.getByRole("button", { name: "인증하고 등록", exact: true }).click();

  await expect(page).toHaveURL(/\/products\/42$/);
  await expect(page.getByRole("button", { name: "재입고 알림 받기", exact: true })).toBeVisible();
  expect(phoneUpdate).toEqual({ phone: "01055556666", verificationCode: "123456" });
});
