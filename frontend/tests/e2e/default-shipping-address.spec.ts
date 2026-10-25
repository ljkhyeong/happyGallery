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

test("기본 배송지를 저장한 뒤 주문서에 불러오고 삭제한다", async ({ page, context, baseURL }) => {
  await installCsrfCookie(context, baseURL);
  const product = productDetailFixture({ name: "주소 확인 작품", category: "공예" });
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  let address: Record<string, string | null> | null = null;
  let version = 0;
  const member = memberFixture({ email: "address@example.com" });
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    if (path === "/api/v1/products/42") return route.fallback();
    if (await fulfillMemberCommon(route, { member })) return;
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
      return fulfillJson(route, { version, shippingAddress: address });
    }
    if (path === "/api/v1/me/favorites") return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path === "/api/v1/me/group-inquiries" || path.endsWith("/page")) return fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
    if (path.endsWith("/reviews")) return fulfillJson(route, EMPTY_REVIEW_PAGE);
    if (path === "/api/v1/orders/policy") return fulfillJson(route, { shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
    return fulfillJson(route, []);
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
  await expect(page.getByRole("button", { name: "기본 배송지 불러오기", exact: true })).toBeEnabled();
  address = { recipientName: "회원", phone: "01012345678", postalCode: "12345", addressLine1: "다른 화면에서 변경한 최신 주소", addressLine2: null };
  version += 1;
  await page.getByRole("button", { name: "기본 배송지 불러오기", exact: true }).click();
  await expect(page.getByLabel("기본 주소", { exact: true })).toHaveValue("다른 화면에서 변경한 최신 주소");
  await page.goto("/my/shipping-address");
  await section.getByRole("button", { name: "기본 배송지 삭제", exact: true }).click();
  await expect(section.getByRole("button", { name: "기본 배송지 삭제" })).toHaveCount(0);
  await expect(section.getByLabel("기본 주소", { exact: true })).toHaveValue("");
});
