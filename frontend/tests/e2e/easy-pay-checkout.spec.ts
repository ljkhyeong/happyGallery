import { expect, test, type Page, type Route } from "@playwright/test";

async function json(route: Route, body: unknown) {
  await route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
}

async function openCheckout(page: Page, amount = 12000) {
  const prepares: unknown[] = [];
  const confirms: unknown[] = [];
  await page.addInitScript(() => {
    const app = window as unknown as {
      tossRequests: unknown[];
      TossPayments: () => { payment: () => { requestPayment: (request: unknown) => Promise<void> } };
    };
    app.tossRequests = [];
    app.TossPayments = () => ({ payment: () => ({
      requestPayment: async (request) => { app.tossRequests.push(request); },
    }) });
  });
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    switch (pathname) {
      case "/api/v1/me":
        return json(route, { id: 701, name: "결제 테스트", phone: "01012345678", email: "pay@example.com", phoneVerified: true });
      case "/api/v1/me/cart":
        return json(route, {
          cartVersion: "a".repeat(64), totalAmount: 12000,
          items: [{ cartItemId: 42, productId: 42, productVariantId: null, options: [], basePrice: 12000,
            variantPriceAdjustment: 0, textOptionPriceAdjustment: 0, careInstructions: null,
            productionLeadDays: null, specification: null,
            productName: "결제 확인 작품", productType: "READY_STOCK",
            price: 12000, qty: 1, subtotal: 12000, available: true }],
        });
      case "/api/v1/orders/policy":
        return json(route, { shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "제작 동의" });
      case "/api/v1/me/notifications/unread-count":
        return json(route, { count: 0 });
      case "/api/v1/workshop":
        return json(route, { name: "해피갤러리", updatedAt: "2026-08-30T10:00:00", version: 1 });
      case "/api/v1/payments/prepare":
        prepares.push(route.request().postDataJSON());
        return json(route, { orderId: "easy-pay-test", amount, context: "ORDER", statusToken: "payment-status" });
      case "/api/v1/payments/confirm":
        confirms.push(route.request().postDataJSON());
        return json(route, { domainId: 701, context: "ORDER", accessToken: null });
      default:
        return json(route, []);
    }
  });
  await page.goto("/cart");
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "easy-pay-test-xsrf", url: page.url() }]);
  await page.getByRole("button", { name: "매장 수령" }).click();
  return { prepares, confirms };
}

function requests(page: Page) {
  return page.evaluate(() => (window as unknown as { tossRequests: unknown[] }).tossRequests);
}

for (const { method, label } of [
  { method: "NAVERPAY", label: "네이버페이" },
  { method: "KAKAOPAY", label: "카카오페이" },
]) {
  test(`@payment ${label}는 약관 동의 전 준비 요청을 보내지 않고 동의 후 전용창을 연다`, async ({ page }) => {
    const { prepares } = await openCheckout(page);
    await page.getByRole("radio", { name: label, exact: true }).check();
    await page.getByRole("button", { name: "결제하기", exact: true }).click();
    await expect(page.getByRole("alert")).toContainText("간편결제에 필요한 약관에 동의해 주세요.");
    expect(prepares).toHaveLength(0);
    expect(await requests(page)).toHaveLength(0);
    await page.getByRole("checkbox", { name: "[필수] 토스페이먼츠 결제 약관에 동의합니다.", exact: true }).check();
    await page.getByRole("button", { name: "결제하기", exact: true }).click();
    await expect.poll(() => requests(page)).toEqual([expect.objectContaining({
      method: "CARD", card: { flowMode: "DIRECT", easyPay: method }, windowTarget: "self",
      amount: { currency: "KRW", value: 12000 }, orderId: "easy-pay-test",
    })]);
    expect(prepares).toHaveLength(1);
  });

  test(`@payment ${label}를 선택해도 서버 결제금액이 0원이면 PG 없이 주문을 확정한다`, async ({ page }) => {
    const { confirms } = await openCheckout(page, 0);
    await page.getByRole("radio", { name: label, exact: true }).check();
    await page.getByRole("checkbox", { name: "[필수] 토스페이먼츠 결제 약관에 동의합니다.", exact: true }).check();
    await page.getByRole("button", { name: "결제하기", exact: true }).click();
    await expect.poll(() => confirms).toEqual([{ orderId: "easy-pay-test", amount: 0 }]);
    await expect(page).toHaveURL(/\/my\/orders\/701$/);
    expect(await requests(page)).toHaveLength(0);
  });
}

test("@payment 기본 카드 결제는 별도 약관 입력 없이 기존 통합창을 연다", async ({ page }) => {
  await openCheckout(page);
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect.poll(() => requests(page)).toHaveLength(1);
  const [request] = await requests(page);
  expect(request).toMatchObject({ method: "CARD", amount: { currency: "KRW", value: 12000 } });
  expect(request).not.toHaveProperty("card");
});

test("@payment 간편결제 수단을 바꾸면 약관 동의를 다시 받는다", async ({ page }) => {
  const { prepares } = await openCheckout(page);
  await page.getByRole("radio", { name: "네이버페이", exact: true }).check();
  const terms = page.getByRole("checkbox", { name: "[필수] 토스페이먼츠 결제 약관에 동의합니다.", exact: true });
  await terms.check();
  await page.getByRole("radio", { name: "카카오페이", exact: true }).check();
  await expect(terms).not.toBeChecked();
  await expect(page.getByText("카카오페이 결제창으로 이동합니다.", { exact: false })).toBeVisible();
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("약관에 동의해 주세요.");
  expect(prepares).toHaveLength(0);
  expect(await requests(page)).toHaveLength(0);
});
