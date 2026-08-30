import { expect, test, type Page, type Route } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

test.afterEach(clearSsrUpstreamFixtures);

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function openOptionProduct(page: Page, quantity: number, { member = false, readyStock = false } = {}) {
  const product: ProductDetailResponse = {
    id: 80, name: "각인 키링", type: "MADE_TO_ORDER", price: 10000, available: true,
    description: null, category: null, imageUrl: null, specification: "가죽 키링",
    careInstructions: null, productionLeadDays: 7, stockQuantity: quantity,
    optionGroups: [
      { key: "color", name: "색상", type: "SELECT", required: true, sortOrder: 0,
        inputMaxLength: null, inputPlaceholder: null, inputPriceAdjustment: null,
        values: [{ key: "brown", name: "브라운", sortOrder: 0 }, { key: "blue", name: "블루", sortOrder: 1 }] },
      { key: "engraving", name: "각인 문구", type: "TEXT", required: false, sortOrder: 1,
        inputMaxLength: 30, inputPlaceholder: null, inputPriceAdjustment: 1000, values: [] },
      { key: "note", name: "추가 문구", type: "TEXT", required: false, sortOrder: 2,
        inputMaxLength: 30, inputPlaceholder: null, inputPriceAdjustment: 0, values: [] },
    ],
    variants: [
      { id: 801, active: true, quantity, priceAdjustment: 2000, selections: [{ groupKey: "color", valueKey: "brown" }] },
      { id: 802, active: true, quantity: 3, priceAdjustment: 4000, selections: [{ groupKey: "color", valueKey: "blue" }] },
    ],
  };
  if (readyStock) {
    product.type = "READY_STOCK";
    product.optionGroups = [];
    product.variants = [];
  }
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/80", product));
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/me") return member
      ? json(route, { id: 7, name: "장바구니 고객", email: "cart@example.com", phone: "01012345678", phoneVerified: true })
      : json(route, { code: "UNAUTHORIZED" }, 401);
    if (pathname === "/api/v1/me/cart") return json(route, { items: [], totalAmount: 0, cartVersion: "empty" });
    if (pathname === "/api/v1/me/rewards") return json(route, { availableBalance: 0, reservedBalance: 0, debtBalance: 0, history: [] });
    if (pathname === "/api/v1/products/80") return json(route, product);
    if (pathname === "/api/v1/products") return json(route, [product]);
    if (pathname === "/api/v1/orders/policy") return json(route, {
      shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 조건에 동의합니다.",
    });
    if (pathname.endsWith("/qna/page")) return json(route, { content: [], hasMore: false, nextCursor: null });
    if (pathname.endsWith("/reviews")) return json(route, {
      content: [], filteredCount: 0, hasMore: false, nextCursor: null,
      summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } },
    });
    if (pathname === "/api/v1/workshop") return json(route, { name: "해피갤러리", version: 1 });
    return json(route, []);
  });
  await page.goto("/products/80");
  await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeVisible();
  return product;
}

for (const { quantity, limit, name } of [
  { quantity: 2, limit: 2, name: "재고" },
  { quantity: 100, limit: 99, name: "주문 한도" },
]) {
  test(`@payment 각인 문구가 달라도 같은 조합의 ${name}를 합산하고 다른 조합은 따로 계산한다`, async ({ page }) => {
    await openOptionProduct(page, quantity);
    const color = page.getByRole("combobox", { name: /색상/ });
    const engraving = page.getByRole("textbox", { name: /각인 문구/ });
    const add = page.getByRole("button", { name: "선택한 옵션 추가", exact: true });
    await color.selectOption("brown");
    await engraving.fill("문구 A");
    await add.click();
    const firstQuantity = page.getByRole("spinbutton", { name: "색상: 브라운 / 각인 문구: 문구 A 수량", exact: true });
    await firstQuantity.fill(String(limit - 1));
    await engraving.fill("문구 B");
    await add.click();
    await expect(page.getByText("같은 옵션 조합으로 추가 가능: 0개")).toBeVisible();
    await expect(firstQuantity).toHaveAttribute("max", String(limit - 1));
    await firstQuantity.fill(String(limit));
    await expect(firstQuantity).toHaveValue(String(limit - 1));
    await engraving.fill("문구 C");
    await add.click();
    await expect(page.getByText("이 옵션 조합은 더 담을 수 없습니다.")).toBeVisible();
    await expect(page.locator(".store-option-form tbody tr")).toHaveCount(2);

    await page.getByRole("row").filter({ hasText: "각인 문구: 문구 B" }).getByRole("button", { name: "삭제" }).click();
    await expect(page.getByText("같은 옵션 조합으로 추가 가능: 1개")).toBeVisible();
    await firstQuantity.fill(String(limit));
    await expect(firstQuantity).toHaveValue(String(limit));
    await color.selectOption("blue");
    await expect(page.getByText("같은 옵션 조합으로 추가 가능: 3개")).toBeVisible();
    await add.click();
    await expect(page.getByRole("spinbutton", { name: "색상: 블루 / 각인 문구: 문구 C 수량", exact: true })).toHaveValue("1");
    await expect(page.getByRole("button", { name: "로그인 후 구매하기", exact: true })).toBeEnabled();
    if (quantity === 2) {
      await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
      await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
      await page.goto("/cart");
      const cartRow = page.getByRole("row").filter({ hasText: "색상: 브라운" });
      await expect(cartRow).toContainText("각인 문구: 문구 A (+₩1,000)");
    }
  });
}

test("@payment 비회원은 주문서에서 선택 옵션과 각인 문구를 확인하고 같은 내용으로 결제를 준비한다", async ({ page }) => {
  await openOptionProduct(page, 2);
  const prepared: unknown[] = [];
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "option-order-xsrf", url: page.url() }]);
  await page.route("**/api/v1/bookings/phone-verifications", (route) => json(route, { verificationId: 1 }));
  await page.route("**/api/v1/policies/current", (route) => json(route, {
    terms: { version: "2026-07", documentPath: "/terms/2026-07" },
    privacy: { version: "2026-07", documentPath: "/privacy/2026-07" },
  }));
  await page.route("**/api/v1/payments/prepare", async (route) => {
    prepared.push(route.request().postDataJSON());
    return json(route, { orderId: "option-order", amount: 27000, context: "ORDER", statusToken: "option-status" });
  });
  await page.evaluate(() => {
    (window as unknown as { TossPayments: unknown }).TossPayments = () => ({
      payment: () => ({ requestPayment: async () => undefined }),
    });
  });
  const color = page.getByRole("combobox", { name: /색상/ });
  const engraving = page.getByRole("textbox", { name: /각인 문구/ });
  const add = page.getByRole("button", { name: "선택한 옵션 추가", exact: true });
  await color.selectOption("brown");
  await engraving.fill("<A> & B");
  await add.click();
  await color.selectOption("blue");
  await engraving.fill("");
  await add.click();
  await page.getByRole("button", { name: /비회원 주문하기/ }).click();

  await page.getByLabel("휴대폰 번호", { exact: true }).fill("01012345678");
  await page.getByRole("button", { name: "인증코드 발송", exact: true }).click();
  await page.getByLabel("인증코드", { exact: true }).fill("123456");
  await page.getByRole("button", { name: "확인", exact: true }).click();
  const brownItem = page.locator(".list-group-item").filter({ hasText: "색상: 브라운" });
  await expect(brownItem.getByText("각인 문구: <A> & B (+₩1,000)", { exact: true })).toBeVisible();
  await expect(brownItem).toContainText("₩13,000");
  const blueItem = page.locator(".list-group-item").filter({ hasText: "색상: 블루" });
  await expect(blueItem).toContainText("₩14,000");
  await expect(blueItem).not.toContainText("각인 문구");
  await page.getByLabel("주문자 이름", { exact: true }).fill("옵션 확인 고객");
  await page.getByRole("button", { name: "매장 수령", exact: true }).click();
  await page.getByRole("checkbox", { name: "주문제작 조건에 동의합니다.", exact: true }).check();
  await page.getByRole("checkbox", { name: /이용약관/ }).check();
  await page.getByRole("button", { name: "결제 진행하기", exact: true }).click();
  await expect.poll(() => prepared).toEqual([expect.objectContaining({ payload: expect.objectContaining({
    items: [
      { productId: 80, productVariantId: 801, textInputs: [{ groupKey: "engraving", value: "<A> & B" }], qty: 1 },
      { productId: 80, productVariantId: 802, textInputs: [], qty: 1 },
    ],
  }) })]);
});

test("@payment 구분문자가 포함된 각인과 두 개의 직접입력 옵션은 별도 항목으로 담긴다", async ({ page }) => {
  await openOptionProduct(page, 5);
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  const engraving = page.getByRole("textbox", { name: /각인 문구/ });
  const add = page.getByRole("button", { name: "선택한 옵션 추가", exact: true });
  await engraving.fill("A|note=B");
  await add.click();
  await engraving.fill("A");
  await page.getByRole("textbox", { name: /추가 문구/ }).fill("B");
  await add.click();
  await expect(page.locator(".store-option-form tbody tr")).toHaveCount(2);
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
  await page.goto("/cart");
  await expect(page.locator("tbody tr")).toHaveCount(2);
  await expect(page.getByText("각인 문구: A|note=B (+₩1,000)", { exact: true })).toBeVisible();
  await expect(page.getByText("추가 문구: B", { exact: true })).toBeVisible();
});

for (const readyStock of [true, false]) {
  test(`@payment 비회원 ${readyStock ? "기성품" : "옵션"} 장바구니는 재고 초과 증가를 막고 재고 감소 후 수량 조정을 허용한다`, async ({ page }) => {
    const product = await openOptionProduct(page, 3, { readyStock });
    if (!readyStock) {
      await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
      await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
    }
    await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
    await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
    await page.goto("/cart");
    const plus = page.getByRole("button", { name: "+", exact: true });
    await plus.click();
    await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart")!)[0].qty)).toBe(2);
    product.stockQuantity = 1;
    if (!readyStock) product.variants[0]!.quantity = 1;
    await expect(plus).toBeEnabled();
    await plus.click();
    await expect(page.getByText("각인 키링의 같은 옵션 조합은 합계 1개까지 담을 수 있습니다.")).toBeVisible();
    await expect(plus).toBeDisabled();
    await page.getByRole("button", { name: "-", exact: true }).click();
    await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart")!)[0].qty)).toBe(1);
    await expect(page.getByText(/수량을 줄여 주세요/)).toHaveCount(0);
  });
}

for (const { stock, quantity, name, message } of [
  { stock: 3, quantity: 3, name: "재고", message: "각인 키링의 같은 옵션 조합은 합계 3개까지 담을 수 있습니다." },
  { stock: 100, quantity: 99, name: "99개 한도", message: "같은 상품·옵션 조합의 장바구니 수량은 1개 이상 99개 이하여야 합니다." },
]) {
  test(`@payment 비회원 다건 담기는 기존 각인 수량까지 합쳐 ${name}를 초과하면 어느 항목도 추가하지 않는다`, async ({ page }) => {
    await page.addInitScript((qty) => localStorage.setItem("hg_guest_cart", JSON.stringify([{
      productId: 80, productVariantId: 801, textInputs: [{ groupKey: "engraving", value: "문구 A" }],
      lineKey: "legacy", lineageId: "old-brown", qty,
    }])), quantity);
    await openOptionProduct(page, stock);
    const color = page.getByRole("combobox", { name: /색상/ });
    const add = page.getByRole("button", { name: "선택한 옵션 추가", exact: true });
    await color.selectOption("blue");
    await add.click();
    await color.selectOption("brown");
    await page.getByRole("textbox", { name: /각인 문구/ }).fill("문구 B");
    await add.click();
    await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
    await expect(page.getByText(message, { exact: true })).toBeVisible();
    const stored = await page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart")!));
    expect(stored).toEqual([expect.objectContaining({ productVariantId: 801, qty: quantity })]);
  });
}

test("@payment 회원 다건 담기는 한 요청을 보내고 응답 실패 뒤 같은 멱등키로 재시도한다", async ({ page }) => {
  await openOptionProduct(page, 5, { member: true });
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "cart-batch-xsrf", url: page.url() }]);
  const requests: Array<{ idempotencyKey: string; items: unknown[] }> = [];
  await page.route("**/api/v1/me/cart/merge", async (route) => {
    requests.push(route.request().postDataJSON());
    return requests.length === 1
      ? json(route, { code: "SERVICE_UNAVAILABLE" }, 503)
      : route.fulfill({ status: 204 });
  });
  const color = page.getByRole("combobox", { name: /색상/ });
  const add = page.getByRole("button", { name: "선택한 옵션 추가", exact: true });
  await color.selectOption("brown");
  await add.click();
  await color.selectOption("blue");
  await add.click();
  const submit = page.getByRole("button", { name: "장바구니 담기", exact: true });
  await submit.click();
  await expect.poll(() => requests.length).toBe(1);
  await expect(submit).toBeEnabled();
  await submit.click();
  await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
  expect(requests).toHaveLength(2);
  expect(requests[0]!.items).toHaveLength(2);
  expect(requests[1]).toEqual(requests[0]);
});
