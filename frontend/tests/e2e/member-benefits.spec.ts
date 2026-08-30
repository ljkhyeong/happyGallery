import { expect, test, type Route } from "@playwright/test";

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

test("@payment 회원은 공개 쿠폰을 받고 주문에 쿠폰 한 장과 적립금을 사용할 수 있다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const cartVersion = "d".repeat(64);
  let claimed = false;
  let preparedPayload: Record<string, unknown> | null = null;

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "member-benefits-token",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      TossPayments: () => {
        payment: () => { requestPayment: () => Promise<void> };
      };
    };
    browserGlobal.TossPayments = () => ({
      payment: () => ({ requestPayment: async () => undefined }),
    });
  });

  const availableCoupon = {
    id: 71,
    definitionId: 7,
    name: "회원 감사 쿠폰",
    discountType: "FIXED",
    discountValue: 5000,
    minOrderAmount: 10000,
    maxDiscountAmount: null,
    validFrom: "2026-08-01T00:00:00",
    validUntil: "2026-12-31T23:59:59",
    status: "AVAILABLE",
    claimedAt: "2026-08-08T10:00:00",
    reservedAt: null,
    usedAt: null,
  };
  const reservedCoupon = {
    ...availableCoupon,
    id: 72,
    definitionId: 8,
    name: "결제 처리 중 쿠폰",
    status: "RESERVED",
    reservedAt: "2026-08-08T11:00:00",
  };
  const newlyClaimedCoupon = {
    ...availableCoupon,
    id: 91,
    definitionId: 9,
    name: "여름 공개 쿠폰",
    discountValue: 3000,
  };

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 401,
        email: "benefits@example.com",
        name: "혜택 회원",
        phone: "01055555555",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/coupons/claimable") {
      await fulfillJson(route, claimed ? [] : [{
        definitionId: 9,
        name: "여름 공개 쿠폰",
        discountType: "FIXED",
        discountValue: 3000,
        minOrderAmount: 10000,
        maxDiscountAmount: null,
        validFrom: "2026-08-01T00:00:00",
        validUntil: "2026-12-31T23:59:59",
      }]);
      return;
    }
    if (pathname === "/api/v1/me/coupons") {
      if (request.method() === "POST") {
        expect(request.postDataJSON()).toEqual({ definitionId: 9 });
        claimed = true;
        await fulfillJson(route, newlyClaimedCoupon, 201);
        return;
      }
      await fulfillJson(
        route,
        claimed
          ? [availableCoupon, reservedCoupon, newlyClaimedCoupon]
          : [availableCoupon, reservedCoupon],
      );
      return;
    }
    if (pathname === "/api/v1/me/rewards") {
      await fulfillJson(route, {
        availableBalance: 50000,
        reservedBalance: 0,
        debtBalance: 0,
        history: [],
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, {
        cartVersion,
        items: [{
          available: true,
          careInstructions: null,
          price: 20000,
          productId: 52,
          productName: "혜택 적용 작품",
          productType: "READY_STOCK",
          productionLeadDays: null,
          qty: 1,
          specification: null,
          subtotal: 20000,
        }],
        totalAmount: 20000,
      });
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-08",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      preparedPayload = (request.postDataJSON() as { payload: Record<string, unknown> }).payload;
      await fulfillJson(route, {
        orderId: "benefit-covered-order",
        amount: 0,
        context: "ORDER",
        statusToken: "benefit-covered-status",
      });
      return;
    }
    if (pathname === "/api/v1/payments/confirm") {
      expect(request.postDataJSON()).toEqual({
        orderId: "benefit-covered-order",
        amount: 0,
      });
      await fulfillJson(route, {
        context: "ORDER",
        domainId: 999,
        accessToken: null,
        accessRecoveryRequired: false,
      });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-08-08T10:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/benefits");
  await expect(page.getByRole("heading", { name: "쿠폰·적립금" })).toBeVisible();
  await expect(page.getByText("₩50,000").first()).toBeVisible();
  const claimableCard = page.locator(".card").filter({ hasText: "여름 공개 쿠폰" }).first();
  await claimableCard.getByRole("button", { name: "쿠폰 받기" }).click();
  await expect(page.getByText("현재 새로 받을 수 있는 쿠폰이 없습니다.")).toBeVisible();
  await expect(page.getByText("사용 가능 2장 · 전체 3장")).toBeVisible();

  await page.goto("/cart");
  await expect(page.getByText("혜택 적용 작품", { exact: true })).toBeVisible();
  const couponSelect = page.getByLabel("사용할 쿠폰");
  await expect(couponSelect.locator("option")).toHaveCount(3);
  const couponLabels = await couponSelect.locator("option").allTextContents();
  expect(couponLabels.some((label) => label.includes("결제 처리 중 쿠폰"))).toBe(false);
  await couponSelect.selectOption("71");
  const rewardInput = page.getByLabel("사용할 적립금");
  await rewardInput.fill("99999");
  await expect(rewardInput).toHaveValue("15000");
  await expect(page.getByText(/예상 상품 결제액 ₩0/)).toBeVisible();

  await page.getByRole("button", { name: "매장 수령" }).click();
  await page.getByRole("button", { name: "결제하기", exact: true }).click();
  await expect.poll(() => preparedPayload).not.toBeNull();
  expect(preparedPayload).toMatchObject({
    issuedCouponId: 71,
    rewardAmount: 15000,
    cartCheckout: true,
    expectedCartVersion: cartVersion,
  });
  await expect(page).toHaveURL(/\/my\/orders\/999$/);
});
