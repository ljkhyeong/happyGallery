import { expect, test, type Page } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import type { RestockAlertResponse } from "../../src/generated/api/customerStore";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";
import { skipExternalFonts } from "./external-fonts";

test.beforeEach(skipExternalFonts);
test.afterEach(clearSsrUpstreamFixtures);

async function mockRestockProduct(page: Page, { loggedIn = true, soldOut = false } = {}) {
  const product: ProductDetailResponse = {
    id: 42, name: "각인 가죽 파우치", type: "MADE_TO_ORDER", price: 20000,
    available: !soldOut, stockQuantity: soldOut ? 0 : 4, description: null, category: "가죽",
    imageUrl: null, specification: null, careInstructions: null, productionLeadDays: 7,
    optionGroups: [
      { key: "color", name: "색상", type: "SELECT", required: true, sortOrder: 0,
        inputMaxLength: null, inputPlaceholder: null, inputPriceAdjustment: null,
        values: [{ key: "brown", name: "브라운", sortOrder: 0 }, { key: "blue", name: "블루", sortOrder: 1 }] },
      { key: "size", name: "크기", type: "SELECT", required: true, sortOrder: 1,
        inputMaxLength: null, inputPlaceholder: null, inputPriceAdjustment: null,
        values: [{ key: "small", name: "소형", sortOrder: 0 }, { key: "large", name: "대형", sortOrder: 1 }] },
      { key: "engraving", name: "각인 문구", type: "TEXT", required: true, sortOrder: 2,
        inputMaxLength: 30, inputPlaceholder: null, inputPriceAdjustment: 0, values: [] },
    ],
    variants: [
      { id: 801, active: true, quantity: soldOut ? 0 : 2, priceAdjustment: 0,
        selections: [{ groupKey: "color", valueKey: "brown" }, { groupKey: "size", valueKey: "small" }] },
      { id: 802, active: true, quantity: soldOut ? 0 : 2, priceAdjustment: 3000,
        selections: [{ groupKey: "color", valueKey: "blue" }, { groupKey: "size", valueKey: "large" }] },
      { id: 803, active: false, quantity: 2, priceAdjustment: 3000,
        selections: [{ groupKey: "color", valueKey: "brown" }, { groupKey: "size", valueKey: "large" }] },
    ],
  };
  const alerts: RestockAlertResponse[] = [{ id: 901, productId: product.id, productVariantId: 802,
    productName: product.name, optionLabel: "색상: 블루 / 크기: 대형", status: "NOTIFIED",
    createdAt: "2026-09-11T10:00:00", notifiedAt: "2026-09-12T10:00:00" }];
  const state = { loggedIn, product, writes: [] as unknown[], productReads: 0 };
  const member = { id: 501, name: "회원", email: "restock@example.com", phone: "01012345678",
    phoneVerified: true, localPasswordEnabled: true };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({ contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=restock-option; Path=/" }, body: "{}" });
      case "/api/v1/auth/login": state.loggedIn = true; return json(member);
      case "/api/v1/me": return state.loggedIn ? json(member) : json({ code: "UNAUTHORIZED" }, 401);
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/coupons": return json([]);
      case "/api/v1/me/rewards": return json({ availableBalance: 0, reservedBalance: 0, debtBalance: 0, history: [] });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/favorites/PRODUCT/42": return json({ saved: false });
      case "/api/v1/me/restock-alerts": {
        if (request.method() === "POST") {
          const input = request.postDataJSON();
          state.writes.push(input);
          alerts.push({ ...alerts[0]!, id: 902, productVariantId: input.productVariantId, status: "WAITING", notifiedAt: null });
          return route.fulfill({ status: 204 });
        }
        return json(alerts);
      }
      case "/api/v1/products/42": state.productReads += 1; return json(product);
      case "/api/v1/products/42/qna/page":
      case "/api/v1/me/products/42/qna/page": return json({ content: [], hasMore: false, nextCursor: null });
      case "/api/v1/products/42/reviews": return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
        summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
      case "/api/v1/orders/policy": return json({ shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
      default: throw new Error(`정의하지 않은 재입고 옵션 테스트 요청: ${request.method()} ${pathname}`);
    }
  });
  return state;
}

test("재입고 내역은 해당 옵션을 선택하고 직접입력과 구매 수량은 고객이 확인한다", async ({ page, context }, testInfo) => {
  const state = await mockRestockProduct(page);
  await page.clock.install();
  await page.goto("/my/restock-alerts");
  const productLink = page.locator("#my-restock-alerts").getByRole("link", { name: state.product.name });
  await expect(productLink).toHaveAttribute("href", "/products/42?variantId=802");
  await productLink.click();
  const color = page.getByRole("combobox", { name: /색상/ });
  const size = page.getByRole("combobox", { name: /크기/ });
  const engraving = page.getByRole("textbox", { name: /각인 문구/ });
  const lines = page.locator(".store-option-form tbody tr");
  await expect(color).toHaveValue("blue");
  await expect(size).toHaveValue("large");
  await expect(engraving).toHaveValue("");
  await expect(lines).toHaveCount(0);
  await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  await expect(page.getByText("필수 옵션을 모두 선택하거나 입력해 주세요.")).toBeVisible();
  await color.selectOption("brown");
  await size.selectOption("small");
  await engraving.fill("내 문구");
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  await expect(lines).toContainText("색상: 브라운 / 크기: 소형 / 각인 문구: 내 문구");

  state.product.variants[0]!.quantity = 6;
  await page.clock.fastForward(61_000);
  await context.setOffline(true);
  await context.setOffline(false);
  await expect(page.getByText("같은 옵션 조합으로 추가 가능: 5개")).toBeVisible();
  await expect(color).toHaveValue("brown");
  await expect(size).toHaveValue("small");
  await expect(engraving).toHaveValue("내 문구");
  await expect(lines).toHaveCount(1);
  expect(state.writes).toEqual([]);

  await page.reload();
  await expect(color).toHaveValue("blue");
  await expect(size).toHaveValue("large");
  await expect(engraving).toHaveValue("");
  await expect(lines).toHaveCount(0);
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 844 });
    await page.locator("section[aria-labelledby='product-option-title']").screenshot({
      path: testInfo.outputPath(`restock-option-${width}.png`), animations: "disabled",
    });
  }
});

test("품절 옵션을 고른 비회원은 로그인 후 같은 옵션의 재입고 알림을 신청한다", async ({ page }) => {
  const state = await mockRestockProduct(page, { loggedIn: false, soldOut: true });
  await page.goto("/products/42");
  await expect(page.getByRole("link", { name: "회원가입 후 구매하기", exact: true })).toBeVisible();
  await page.getByRole("combobox", { name: /색상/ }).selectOption("blue");
  await page.getByRole("combobox", { name: /크기/ }).selectOption("large");
  const login = page.getByRole("link", { name: "로그인하고 재입고 알림 받기", exact: true });
  await expect(login).toHaveAttribute("href", "/login?redirect=%2Fproducts%2F42%3FvariantId%3D802");
  await login.click();
  await page.getByLabel("이메일", { exact: true }).fill("restock@example.com");
  await page.getByLabel("비밀번호", { exact: true }).fill("test-password");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(page).toHaveURL(/\/products\/42\?variantId=802$/);
  await expect(page.getByRole("combobox", { name: /색상/ })).toHaveValue("blue");
  await expect(page.getByRole("combobox", { name: /크기/ })).toHaveValue("large");
  const register = page.getByRole("button", { name: "재입고 알림 받기", exact: true });
  await expect(register).toBeEnabled();
  expect(state.writes).toEqual([]);
  await register.click();
  await expect(page.getByRole("button", { name: "재입고 알림 해지", exact: true })).toBeVisible();
  expect(state.writes).toEqual([{ productId: 42, productVariantId: 802 }]);
});

test("삭제·판매 중지된 옵션 링크는 안내를 표시하고 다른 옵션을 직접 선택할 수 있다", async ({ page }, testInfo) => {
  await mockRestockProduct(page);
  for (const variantId of [999, 803]) {
    await page.goto(`/products/42?variantId=${variantId}`);
    await expect(page.getByText("해당 옵션은 현재 판매하지 않습니다. 현재 상품의 옵션과 가격을 확인해 주세요.")).toBeVisible();
    await expect(page.getByRole("combobox", { name: /색상/ })).toHaveValue("");
    await expect(page.getByRole("combobox", { name: /크기/ })).toHaveValue("");
    await expect(page.locator(".store-option-form tbody tr")).toHaveCount(0);
    await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeDisabled();
  }
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator("section[aria-labelledby='product-option-title']").screenshot({
    path: testInfo.outputPath("restock-unavailable-mobile.png"), animations: "disabled",
  });
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  await page.getByRole("combobox", { name: /크기/ }).selectOption("small");
  await page.getByRole("textbox", { name: /각인 문구/ }).fill("새 문구");
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  await expect(page.locator(".store-option-form tbody tr")).toContainText("색상: 브라운 / 크기: 소형 / 각인 문구: 새 문구");
  await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeEnabled();
});
