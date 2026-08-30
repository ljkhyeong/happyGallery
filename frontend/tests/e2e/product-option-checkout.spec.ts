import { expect, test, type Page, type Route } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";
import { skipExternalFonts } from "./external-fonts";

test.afterEach(clearSsrUpstreamFixtures);

test.beforeEach(skipExternalFonts);

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function openOptionProduct(page: Page, quantity: number, { member = false, readyStock = false, defaultVariant = false } = {}) {
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
  } else if (defaultVariant) {
    product.optionGroups = [];
    product.variants = [{ id: 801, active: true, quantity, priceAdjustment: 2000, selections: [] }];
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

async function mockGuestCheckout(page: Page) {
  const prepared: unknown[] = [];
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "guest-order-xsrf", url: new URL("/", page.url()).href }]);
  await page.route("**/api/v1/bookings/phone-verifications", (route) => json(route, { verificationId: 1 }));
  await page.route("**/api/v1/policies/current", (route) => json(route, {
    terms: { version: "2026-07", documentPath: "/terms/2026-07" },
    privacy: { version: "2026-07", documentPath: "/privacy/2026-07" },
  }));
  await page.route("**/api/v1/payments/prepare", async (route) => {
    prepared.push(route.request().postDataJSON());
    return json(route, { code: "INVALID_INPUT", message: "요청 확인용 응답" }, 400);
  });
  return prepared;
}

async function verifyGuestOrderPhone(page: Page) {
  await page.getByLabel("휴대폰 번호", { exact: true }).fill("01012345678");
  await page.getByRole("button", { name: "인증코드 발송", exact: true }).click();
  await expect(page.getByLabel("인증코드", { exact: true })).toBeVisible();
  await page.getByLabel("인증코드", { exact: true }).fill("123456");
  await page.getByRole("button", { name: "확인", exact: true }).click();
}

async function completeGuestOrderForm(page: Page, madeToOrder = true) {
  await verifyGuestOrderPhone(page);
  await page.getByLabel("주문자 이름", { exact: true }).fill("주문 확인 고객");
  await page.getByRole("button", { name: "매장 수령", exact: true }).click();
  if (madeToOrder) await page.getByRole("checkbox", { name: "주문제작 조건에 동의합니다.", exact: true }).check();
  await page.getByRole("checkbox", { name: /이용약관/ }).check();
}

for (const readyStock of [false, true]) {
  test(`@payment 옵션 없는 ${readyStock ? "기성품" : "주문제작 상품"}은 상세와 주문서의 가격·재고 한도가 일치한다`, async ({ page }) => {
    await openOptionProduct(page, 2, { readyStock, defaultVariant: !readyStock });
    const prepared = await mockGuestCheckout(page);
    const quantity = page.getByRole("spinbutton", { name: "수량", exact: true });
    await expect(quantity).toHaveAttribute("max", "2");
    await quantity.fill("2");
    await expect(page.getByRole("button", { name: "수량 증가", exact: true })).toBeDisabled();
    await quantity.fill("3");
    await expect(quantity).toHaveValue("2");
    const total = readyStock ? "₩20,000" : "₩24,000";
    await expect(page.locator(".store-purchase-summary")).toContainText(total);
    await page.getByRole("link", { name: /비회원 주문하기/ }).click();
    await completeGuestOrderForm(page, !readyStock);
    const row = page.locator(".list-group-item");
    await expect(row).toContainText(total);
    await expect(row.getByRole("spinbutton")).toHaveAttribute("max", "2");
    await page.getByLabel("상품", { exact: true }).selectOption("80");
    await expect(page.getByText("추가 가능 수량: 0개")).toBeVisible();
    await expect(page.getByRole("button", { name: "추가", exact: true })).toBeDisabled();
    await row.getByRole("spinbutton").fill("1");
    await expect(page.getByText("추가 가능 수량: 1개")).toBeVisible();
    await page.getByRole("button", { name: "추가", exact: true }).click();
    await expect(row).toHaveCount(1);
    await row.getByRole("spinbutton").fill("1");
    await page.reload();
    await completeGuestOrderForm(page, !readyStock);
    await expect(row.getByRole("spinbutton")).toHaveValue("1");
    await page.getByRole("button", { name: "결제 진행하기", exact: true }).click();
    await expect.poll(() => prepared).toEqual([expect.objectContaining({ payload: expect.objectContaining({
      items: [{ productId: 80, productVariantId: readyStock ? null : 801, textInputs: [], qty: 1 }],
    }) })]);
  });
}

for (const changed of ["재고 감소", "옵션 삭제", "판매 중지"] as const) {
  test(`@payment 비회원 주문서는 ${changed} 시 결제를 막고 조정 방법을 안내한다`, async ({ page }) => {
    const product = await openOptionProduct(page, 2);
    const prepared = await mockGuestCheckout(page);
    await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
    for (const text of ["첫 각인", "둘째 각인"]) {
      await page.getByRole("textbox", { name: /각인 문구/ }).fill(text);
      await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
    }
    if (changed === "재고 감소") product.variants[0]!.quantity = 1;
    if (changed === "옵션 삭제") product.variants = product.variants.filter((variant) => variant.id !== 801);
    if (changed === "판매 중지") product.available = false;
    await page.getByRole("button", { name: /비회원 주문하기/ }).click();
    await completeGuestOrderForm(page);
    const checkout = page.getByRole("button", { name: "결제 진행하기", exact: true });
    await expect(checkout).toBeDisabled();
    expect(prepared).toHaveLength(0);
    if (changed === "재고 감소") {
      await expect(page.getByText(/같은 상품·옵션 조합은 합계 1개까지/)).toHaveCount(2);
      await page.locator(".list-group-item").filter({ hasText: "둘째 각인" }).getByRole("button", { name: "삭제", exact: true }).click();
      await expect(checkout).toBeEnabled();
      const quantity = page.locator(".list-group-item").getByRole("spinbutton");
      await quantity.fill("2");
      await expect(quantity).toHaveValue("1");
    } else if (changed === "옵션 삭제") {
      await expect(page.getByRole("link", { name: "옵션 다시 선택", exact: true })).toHaveCount(2);
    } else {
      await expect(page.getByText("현재 구매할 수 없는 상품입니다. 이 항목을 삭제해 주세요.", { exact: true })).toHaveCount(2);
    }
  });
}

test("@payment 옵션 주문 초안은 주문서별로 복원하고 누락되거나 다른 세션이면 재선택을 안내한다", async ({ page }) => {
  await openOptionProduct(page, 2);
  await mockGuestCheckout(page);
  const urls: string[] = [];
  for (const text of ["첫 주문 각인", "둘째 주문 각인"]) {
    await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeVisible();
    await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
    await page.getByRole("textbox", { name: /각인 문구/ }).fill(text);
    await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
    await expect(page.locator(".store-option-form tbody tr")).toContainText(text);
    await page.getByRole("button", { name: /비회원 주문하기/ }).click();
    await expect(page).toHaveURL(/draftId=/);
    urls.push(page.url());
    await page.goBack();
  }
  expect(urls[0]).not.toBe(urls[1]);
  await page.goto(urls[0]!);
  await completeGuestOrderForm(page);
  await expect(page.locator(".list-group-item")).toContainText("첫 주문 각인");
  await expect(page.locator(".list-group-item")).not.toContainText("둘째 주문 각인");
  await page.evaluate(() => {
    const key = `hg_guest_order_draft:${new URL(location.href).searchParams.get("draftId")}`;
    const draft = JSON.parse(sessionStorage.getItem(key)!);
    draft.owner.boundaryEpoch = "different-customer-session";
    sessionStorage.setItem(key, JSON.stringify(draft));
  });
  await page.reload();
  await expect(page.getByText(/선택한 옵션 정보를 불러올 수 없습니다/)).toBeVisible();
  await expect(page.getByRole("link", { name: "상품 다시 선택", exact: true })).toHaveAttribute("href", "/products/80");
  await expect(page.getByRole("button", { name: "결제 진행하기", exact: true })).toHaveCount(0);
  await page.goto("/orders/new?productId=80&draft=options");
  await expect(page.getByText(/선택한 옵션 정보를 불러올 수 없습니다/)).toBeVisible();
});

test("@payment 주문서의 수량 수정·상품 추가·삭제는 결제 실패 복귀와 새로고침 후에도 유지한다", async ({ page }) => {
  const product = await openOptionProduct(page, 5);
  const prepared = await mockGuestCheckout(page);
  const extra: ProductDetailResponse = { ...product, id: 81, name: "추가 작품", type: "READY_STOCK", optionGroups: [], variants: [] };
  await page.route("**/api/v1/products", (route) => json(route, [product, extra]));
  await page.route("**/api/v1/payments/prepare", (route) => {
    prepared.push(route.request().postDataJSON());
    return json(route, { orderId: "draft-return", amount: 56000, context: "ORDER", statusToken: "draft-return-token" });
  });
  await page.route("**/api/v1/payments/draft-return/abandon", (route) => route.fulfill({ status: 204 }));
  await page.evaluate(() => {
    (window as unknown as { TossPayments: unknown }).TossPayments = () => ({ payment: () => ({
      requestPayment: async () => { window.location.assign("/payments/fail?code=PAY_PROCESS_CANCELED"); },
    }) });
  });
  const color = page.getByRole("combobox", { name: /색상/ });
  for (const value of ["brown", "blue"]) {
    await color.selectOption(value);
    await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  }
  await page.getByRole("button", { name: /비회원 주문하기/ }).click();
  await completeGuestOrderForm(page);
  const orderUrl = page.url();
  const brown = page.locator(".list-group-item").filter({ hasText: "색상: 브라운" });
  await brown.getByRole("spinbutton").fill("3");
  await page.locator(".list-group-item").filter({ hasText: "색상: 블루" }).getByRole("button", { name: "삭제", exact: true }).click();
  await page.getByLabel("상품", { exact: true }).selectOption("81");
  await page.getByLabel("수량", { exact: true }).fill("2");
  await page.getByRole("button", { name: "추가", exact: true }).click();
  await page.getByRole("button", { name: "결제 진행하기", exact: true }).click();
  await expect(page.getByRole("heading", { name: "결제 실패", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "구매 화면으로 돌아가기", exact: true }).click();
  await expect(page).toHaveURL(orderUrl);
  await expect(page.getByLabel("휴대폰 번호", { exact: true })).toHaveValue("");
  await verifyGuestOrderPhone(page);
  await expect(brown.getByRole("spinbutton")).toHaveValue("3");
  await expect(page.locator(".list-group-item").filter({ hasText: "추가 작품" }).getByRole("spinbutton")).toHaveValue("2");
  await expect(page.locator(".list-group-item")).toHaveCount(2);
  await page.locator(".list-group-item").getByRole("button", { name: "삭제", exact: true }).first().click();
  await page.locator(".list-group-item").getByRole("button", { name: "삭제", exact: true }).click();
  await page.reload();
  await verifyGuestOrderPhone(page);
  await expect(page.locator(".list-group-item")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "결제 진행하기", exact: true })).toBeDisabled();
});

test("@payment 수동 주문서도 초안을 만들고 저장 실패 시 현재 수량을 유지하며 재저장한다", async ({ page }) => {
  await openOptionProduct(page, 5, { readyStock: true });
  await mockGuestCheckout(page);
  await page.goto("/orders/new");
  await expect(page).toHaveURL(/draftId=/);
  await page.getByRole("button", { name: "비회원 다중 상품 주문 계속", exact: true }).click();
  await completeGuestOrderForm(page, false);
  await page.getByLabel("상품", { exact: true }).selectOption("80");
  await page.getByRole("button", { name: "추가", exact: true }).click();
  await page.evaluate(() => {
    const original = Storage.prototype.setItem;
    Storage.prototype.setItem = function (key, value) {
      if (key.startsWith("hg_guest_order_draft:")) {
        Storage.prototype.setItem = original;
        throw new DOMException("test storage full", "QuotaExceededError");
      }
      original.call(this, key, value);
    };
  });
  const quantity = page.locator(".list-group-item").getByRole("spinbutton");
  await quantity.fill("2");
  await expect(quantity).toHaveValue("2");
  await expect(page.getByText(/변경한 주문 내용을 저장하지 못했습니다/)).toBeVisible();
  await expect(page.getByRole("button", { name: "결제 진행하기", exact: true })).toBeDisabled();
  await page.getByRole("button", { name: "다시 저장", exact: true }).click();
  await expect(page.getByRole("button", { name: "결제 진행하기", exact: true })).toBeEnabled();
  await page.reload();
  await verifyGuestOrderPhone(page);
  await expect(quantity).toHaveValue("2");
});

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
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "option-order-xsrf", url: new URL("/", page.url()).href }]);
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
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "cart-batch-xsrf", url: new URL("/", page.url()).href }]);
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

test("@payment 선택한 옵션은 상품 재조회 뒤 현재 이름과 단가로 합계를 갱신한다", async ({ page }) => {
  const product = await openOptionProduct(page, 10);
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  await page.getByRole("textbox", { name: /각인 문구/ }).fill("문구 A");
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  await page.locator(".store-option-form").getByRole("spinbutton").fill("2");
  await expect(page.locator(".store-purchase-summary")).toContainText("₩26,000");

  product.price = 12000;
  product.variants[0]!.priceAdjustment = 4000;
  product.optionGroups[0]!.values[0]!.name = "밤색";
  product.optionGroups[1]!.inputPriceAdjustment = 1500;
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
  const row = page.locator(".store-option-form tbody tr");
  await expect(row).toContainText("색상: 밤색 / 각인 문구: 문구 A");
  await expect(row).toContainText("₩17,500");
  await expect(row.getByRole("spinbutton")).toHaveValue("2");
  await expect(page.locator(".store-purchase-summary")).toContainText("₩35,000");
  await expect(page.getByText("상품 가격 또는 옵션 정보가 변경되었습니다. 현재 표시된 옵션과 금액을 확인해 주세요.")).toBeVisible();
  await page.goto("/cart");
  await expect(page.locator("tbody tr")).toContainText("₩35,000");
});

test("@payment 모든 옵션이 삭제되어도 이전 선택을 기본 상품 주문으로 바꾸지 않는다", async ({ page }) => {
  const product = await openOptionProduct(page, 5);
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  await page.getByRole("textbox", { name: /각인 문구/ }).fill("남길 문구");
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  product.optionGroups = [];
  product.variants = [{ id: 803, active: true, quantity: 5, priceAdjustment: 0, selections: [] }];
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.locator(".store-option-form tbody tr")).toHaveCount(1);
  await expect(page.getByText("선택한 옵션이 변경되었습니다. 이 항목을 삭제한 뒤 다시 선택해 주세요.")).toBeVisible();
  await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeDisabled();
  await page.locator(".store-option-form").getByRole("button", { name: "삭제", exact: true }).click();
  await expect(page.getByLabel("수량", { exact: true })).toHaveValue("1");
  await expect(page.getByRole("button", { name: "장바구니 담기", exact: true })).toBeEnabled();
});

test("@payment 선택형 그룹 삭제 뒤에는 남아 있는 옵션을 새로 선택할 수 있다", async ({ page }) => {
  const product = await openOptionProduct(page, 5);
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  product.optionGroups = product.optionGroups.filter((group) => group.key !== "color");
  product.variants = [{ id: 803, active: true, quantity: 5, priceAdjustment: 0, selections: [] }];
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.getByText("선택한 옵션이 변경되었습니다. 이 항목을 삭제한 뒤 다시 선택해 주세요.")).toBeVisible();
  await page.locator(".store-option-form").getByRole("button", { name: "삭제", exact: true }).click();
  await expect(page.getByRole("combobox", { name: /색상/ })).toHaveCount(0);
  await page.getByRole("textbox", { name: /각인 문구/ }).fill("새 문구");
  await page.getByRole("button", { name: "선택한 옵션 추가", exact: true }).click();
  await expect(page.locator(".store-option-form tbody tr")).toContainText("각인 문구: 새 문구");
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
  const stored = await page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart")!));
  expect(stored).toEqual([expect.objectContaining({ productVariantId: 803, textInputs: [{ groupKey: "engraving", value: "새 문구" }] })]);
});

test("@payment 비회원은 같은 조합이라도 삭제된 각인이 있으면 다건 담기와 증가를 막고 감소·삭제는 허용한다", async ({ page }) => {
  const product = await openOptionProduct(page, 5);
  await page.getByRole("combobox", { name: /색상/ }).selectOption("brown");
  const engraving = page.getByRole("textbox", { name: /각인 문구/ });
  const add = page.getByRole("button", { name: "선택한 옵션 추가", exact: true });
  await engraving.fill("이전 각인");
  await add.click();
  await engraving.fill("");
  await add.click();
  product.optionGroups = product.optionGroups.filter((group) => group.key !== "engraving");
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.getByText("각인 키링의 선택한 옵션이 변경되었습니다. 상품 상세에서 다시 선택해 주세요.", { exact: true })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("hg_guest_cart"))).toBeNull();
  await page.locator(".store-option-form tbody tr").filter({ hasText: "선택한 옵션이 변경되었습니다" }).getByRole("button", { name: "삭제", exact: true }).click();
  await add.click();
  await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
  await expect(page.getByText("장바구니에 추가되었습니다.", { exact: true })).toBeVisible();
  await page.goto("/cart");
  const plus = page.getByRole("button", { name: "+", exact: true });
  await expect(plus).toBeEnabled();
  product.optionGroups.find((group) => group.key === "note")!.required = true;
  await plus.click();
  await expect(page.getByText("각인 키링의 선택한 옵션이 변경되었습니다. 상품 상세에서 다시 선택해 주세요.", { exact: true })).toBeVisible();
  await expect(plus).toBeDisabled();
  expect(await page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart")!)[0].qty)).toBe(2);
  await page.getByRole("button", { name: "-", exact: true }).click();
  await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart")!)[0].qty)).toBe(1);
  await page.getByRole("button", { name: "삭제", exact: true }).click();
  await expect.poll(() => page.evaluate(() => localStorage.getItem("hg_guest_cart"))).toBeNull();
});
