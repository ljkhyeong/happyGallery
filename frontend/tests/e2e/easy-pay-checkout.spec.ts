import { expect, test, type Page, type Route } from "@playwright/test";

async function json(route: Route, body: unknown) {
  await route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
}

async function openCheckout(page: Page, amount = 12000) {
  const prepares: unknown[] = [];
  const confirms: unknown[] = [];
  const abandoned: string[] = [];
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
      case "/api/v1/payments/easy-pay-test/abandon":
        abandoned.push(pathname);
        return route.fulfill({ status: 204 });
      default:
        return json(route, []);
    }
  });
  await page.goto("/cart");
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "easy-pay-test-xsrf", url: page.url() }]);
  await page.getByRole("button", { name: "매장 수령" }).click();
  return { prepares, confirms, abandoned };
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

test("@payment 결제창 취소는 콜백 주문번호 없이도 기존 결제를 종료한 뒤 장바구니로 복귀한다", async ({ page }) => {
  const { prepares, confirms, abandoned } = await openCheckout(page);
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect.poll(() => requests(page)).toHaveLength(1);
  const [request] = await requests(page) as Array<{ failUrl: string }>;
  await page.goto(`${request!.failUrl}?code=PAY_PROCESS_CANCELED&message=external-message`);
  await expect(page.getByText("결제창에서 결제를 취소했습니다.")).toBeVisible();
  await expect(page).toHaveURL(/\/payments\/fail$/);
  const back = page.getByRole("button", { name: "구매 화면으로 돌아가기", exact: true });
  await back.click();
  await expect(page).toHaveURL(/\/cart$/);
  expect(prepares).toHaveLength(1);
  expect(confirms).toHaveLength(0);
  expect(abandoned).toHaveLength(1);
  expect(await page.evaluate(() => sessionStorage.getItem("hg_payment_return_hint"))).toBeNull();
});

for (const checkout of [
  { label: "비회원 주문서", path: "/orders/new?productId=42&qty=2&draft=options", customerId: null },
  { label: "비회원 예약", path: "/bookings/new?classId=7", customerId: null },
  { label: "회원 8회권", path: "/passes/purchase", customerId: 701 },
]) {
  test(`@payment ${checkout.label} 결제 실패는 같은 고객의 구매 경로를 유지한다`, async ({ page }) => {
    await page.addInitScript(({ path, customerId }) => {
      localStorage.setItem("hg_customer_session_boundary", JSON.stringify({ epoch: "checkout-owner", customerId }));
      sessionStorage.setItem("hg_payment_return_hint", JSON.stringify({
        owner: { boundaryEpoch: "checkout-owner", boundaryCustomerId: customerId },
        value: { returnPath: path },
      }));
    }, checkout);
    await page.route("**/api/v1/**", (route) => {
      const { pathname } = new URL(route.request().url());
      if (pathname === "/api/v1/me") {
        return checkout.customerId === null
          ? route.fulfill({ status: 401, contentType: "application/json", body: '{"code":"UNAUTHORIZED"}' })
          : json(route, { id: checkout.customerId, name: "결제 테스트", phone: "01012345678", phoneVerified: true });
      }
      if (pathname.endsWith("/unread-count")) return json(route, { count: 0 });
      if (pathname === "/api/v1/me/cart") return json(route, { items: [], totalAmount: 0, cartVersion: "a".repeat(64) });
      return json(route, {});
    });
    await page.goto("/payments/fail?code=PAY_PROCESS_ABORTED");
    await page.getByRole("button", { name: "구매 화면으로 돌아가기", exact: true }).click();
    await expect(page).toHaveURL(checkout.path);
  });
}

test("@payment 복귀 정보가 없거나 외부 주소이면 구매 복귀 링크를 표시하지 않는다", async ({ page }) => {
  await openCheckout(page);
  await page.goto("/payments/fail");
  const back = page.getByRole("button", { name: "구매 화면으로 돌아가기", exact: true });
  await expect(back).not.toBeVisible();
  await page.evaluate(() => {
    const boundary = JSON.parse(localStorage.getItem("hg_customer_session_boundary")!);
    sessionStorage.setItem("hg_payment_return_hint", JSON.stringify({
      owner: { boundaryEpoch: boundary.epoch, boundaryCustomerId: boundary.customerId },
      value: { returnPath: "//external.example/checkout" },
    }));
  });
  await page.reload();
  await expect(page.getByRole("link", { name: "결제 테스트", exact: true })).toBeVisible();
  await expect(back).not.toBeVisible();
  await expect(page.getByRole("link", { name: "상품 둘러보기", exact: true })).toHaveAttribute("href", "/products");
});

test("@payment 계정이 바뀌면 이전 고객의 구매 복귀 링크를 숨긴다", async ({ page }) => {
  await openCheckout(page);
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect.poll(() => requests(page)).toHaveLength(1);
  await page.goto("/payments/fail?code=PAY_PROCESS_CANCELED");
  const back = page.getByRole("button", { name: "구매 화면으로 돌아가기", exact: true });
  await expect(back).toBeVisible();
  await page.route("**/api/v1/me", (route) => json(route, {
    id: 702, name: "변경된 고객", phone: "01098765432", phoneVerified: true,
  }));
  await page.evaluate(() => {
    const key = "hg_customer_session_boundary";
    const oldValue = localStorage.getItem(key);
    const newValue = JSON.stringify({ epoch: "changed-customer", customerId: 702 });
    localStorage.setItem(key, newValue);
    window.dispatchEvent(new StorageEvent("storage", { key, oldValue, newValue, storageArea: localStorage }));
  });
  await expect(page.getByRole("link", { name: "변경된 고객", exact: true })).toBeVisible();
  await expect(back).not.toBeVisible();
});

test("@payment 승인 중인 결제는 종료하지 못하며 구매 화면 대신 현재 상태를 보여준다", async ({ page }) => {
  const { prepares, confirms } = await openCheckout(page);
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect.poll(() => requests(page)).toHaveLength(1);
  await page.route("**/api/v1/payments/easy-pay-test/abandon", (route) => route.fulfill({
    status: 409, contentType: "application/json", body: '{"code":"CONFLICT"}',
  }));
  await page.route("**/api/v1/payments/easy-pay-test", (route) => json(route, {
    context: "ORDER", amount: 12000, status: "CONFIRMING", domainId: null,
    accessToken: null, accessRecoveryRequired: false, receiptUrl: null,
  }));
  await page.goto("/payments/fail?code=PAY_PROCESS_CANCELED");
  await page.getByRole("button", { name: "구매 화면으로 돌아가기", exact: true }).click();
  await expect(page.getByText("결제를 확인하고 있습니다", { exact: true })).toBeVisible();
  await expect(page).toHaveURL(/\/payments\/fail$/);
  expect(prepares).toHaveLength(1);
  expect(confirms).toHaveLength(0);
  expect(await page.evaluate(() => sessionStorage.getItem("hg_payment_return_hint"))).not.toBeNull();
});

test("@payment SDK가 현재 화면에서 취소 오류를 반환하면 승인 전 결제를 종료한다", async ({ page }) => {
  const { prepares, confirms, abandoned } = await openCheckout(page);
  await page.evaluate(() => {
    (window as unknown as { TossPayments: unknown }).TossPayments = () => ({ payment: () => ({
      requestPayment: async () => { throw new Error("결제창 취소"); },
    }) });
  });
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect.poll(() => abandoned).toHaveLength(1);
  await expect(page).toHaveURL(/\/cart$/);
  expect(prepares).toHaveLength(1);
  expect(confirms).toHaveLength(0);
});

test("@payment 회원 주문·예약·8회권에서 영수증을 다시 열고 영수증이 없는 결제는 링크를 숨긴다", async ({ page }) => {
  await openCheckout(page);
  const receiptUrl = "https://dashboard.tosspayments.com/receipt/member-history";
  await page.route("**/api/v1/me/orders/701", (route) => json(route, {
    orderId: 701, orderNumber: "ORD-00000701", status: "COMPLETED", totalAmount: 12000,
    productAmount: 12000, shippingFee: 0, couponDiscountAmount: 0, rewardUsedAmount: 0,
    pgPaidAmount: 12000, rewardEarnBase: 12000, issuedCouponId: null,
    paidAt: "2026-08-30T10:00:00", approvalDeadlineAt: null, items: [], fulfillment: null, refund: null, receiptUrl,
  }));
  await page.route("**/api/v1/me/bookings/702", (route) => json(route, {
    bookingId: 702, classId: 7, slotId: 70, status: "CANCELED", className: "영수증 클래스",
    startAt: "2026-08-30T10:00:00", endAt: "2026-08-30T12:00:00", participantCount: 1,
    depositAmount: 5000, balanceAmount: 45000, balanceStatus: "UNPAID", passBooking: false,
    cancelPolicy: { cancellable: false, refundable: false, deadlineAt: null, passCreditRestorable: false },
    refund: null, receiptUrl,
  }));
  await page.route("**/api/v1/me/passes/page**", (route) => json(route, {
    content: [true, false].map((hasReceipt, index) => ({
      passId: 703 + index, planCode: "REGULAR_CRAFT_8", planName: "정규 공예 8회권",
      purchasedAt: "2026-08-30T10:00:00", expiresAt: "2099-12-31T23:59:59",
      totalCredits: 8, remainingCredits: 8, totalPrice: 240000, refund: null,
      receiptUrl: hasReceipt ? receiptUrl : null,
    })), hasMore: false, nextCursor: null,
  }));
  for (const path of ["/my/orders/701", "/my/bookings/702", "/my/passes"]) {
    await page.goto(path);
    const receipt = page.getByRole("link", { name: "결제 영수증 보기", exact: true });
    await expect(receipt).toHaveCount(1);
    await expect(receipt).toHaveAttribute("href", receiptUrl);
    await expect(receipt).toHaveAttribute("target", "_blank");
  }
});
