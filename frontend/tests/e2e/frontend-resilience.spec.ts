import { expect, test, type Route } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  homeSsrFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

const EMPTY_CART_VERSION = "0".repeat(64);

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

const guestCustomerError = {
  code: "UNAUTHORIZED",
  message: "로그인이 필요합니다.",
};

const temporaryError = {
  code: "SERVICE_UNAVAILABLE",
  message: "잠시 후 다시 시도해 주세요.",
};

test("@smoke @payment 결제 복구 저장소 쓰기가 실패해도 현재 세션의 링크로 상태를 조회한다", async ({
  baseURL,
  context,
  page,
}) => {
  let paymentStatusToken: string | undefined;

  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "frontend-resilience-csrf",
    url: baseURL,
  }]);

  await page.addInitScript(() => {
    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function setItem(key: string, value: string) {
      if (
        this === window.sessionStorage
        && (
          key === "guest_payment_status_recovery"
          || key.startsWith("hg_payment_status_token:")
        )
      ) {
        throw new DOMException("sessionStorage disabled", "QuotaExceededError");
      }
      return originalSetItem.call(this, key, value);
    };
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, guestCustomerError, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/monitoring/client-events") {
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/bookings/phone-verifications") {
      await fulfillJson(route, { phone: "01012345678", verificationId: 1 });
      return;
    }
    if (pathname === "/api/v1/guest-records/payment-status-recovery") {
      await fulfillJson(route, {
        expiresAt: "2099-01-01T00:00:00",
        statusToken: "router-state-status-token",
        payments: [{
          orderId: "recovered-order",
          context: "ORDER",
          amount: 12000,
          status: "COMPLETED",
        }],
      });
      return;
    }
    if (pathname === "/api/v1/payments/recovered-order") {
      paymentStatusToken = request.headers()["x-payment-status-token"];
      await fulfillJson(route, {
        context: "ORDER",
        status: "COMPLETED",
        amount: 12000,
        domainId: 77,
        accessToken: "guest-order-access-token",
        accessRecoveryRequired: false,
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/guest");
  const recoveryCard = page.locator(".card")
    .filter({ hasText: "처리 중인 결제 결과 복구" })
    .first();
  await recoveryCard.getByLabel("휴대폰 번호").fill("01012345678");
  await recoveryCard.getByRole("button", { name: "인증코드 발송" }).click();
  await recoveryCard.getByLabel("인증코드").fill("123456");
  await recoveryCard.getByRole("button", { name: "결제 결과 복구" }).click();

  await expect(recoveryCard.getByText("결제 상태 조회 정보를 복구했습니다.")).toBeVisible();
  await expect.poll(() => page.evaluate(() => ({
    recovery: sessionStorage.getItem("guest_payment_status_recovery"),
    statusToken: sessionStorage.getItem("hg_payment_status_token:recovered-order"),
  }))).toEqual({ recovery: null, statusToken: null });

  await recoveryCard.getByRole("link", { name: /주문 결제/ }).click();
  await expect(page).toHaveURL(/\/guest\/payments\/recovered-order$/);
  await expect(page.getByRole("alert").getByText("결제가 완료되었습니다", { exact: true })).toBeVisible();
  expect(paymentStatusToken).toBe("router-state-status-token");
});

test("@payment 결제 복구 저장 직후 계정 경계가 바뀌면 복구값과 주문별 상태 토큰을 되돌린다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "payment-recovery-boundary-csrf",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    const originalSetItem = Storage.prototype.setItem;
    Storage.prototype.setItem = function setItem(key: string, value: string) {
      originalSetItem.call(this, key, value);
      if (
        this === window.sessionStorage
        && key === "guest_payment_status_recovery"
      ) {
        originalSetItem.call(
          window.localStorage,
          "hg_customer_session_boundary",
          JSON.stringify({
            epoch: "payment-recovery-next-account",
            customerId: 202,
          }),
        );
      }
    };
  });
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 101,
        email: "payment-recovery-a@example.com",
        name: "결제 복구 회원 A",
        phone: "01011111111",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/bookings/phone-verifications") {
      await fulfillJson(route, { phone: "01011111111", verificationId: 11 });
      return;
    }
    if (pathname === "/api/v1/guest-records/payment-status-recovery") {
      await fulfillJson(route, {
        expiresAt: "2099-01-01T00:00:00",
        statusToken: "stale-recovery-status-token",
        payments: [{
          orderId: "stale-recovery-order",
          context: "ORDER",
          amount: 12000,
          status: "COMPLETED",
        }],
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, {
        cartVersion: EMPTY_CART_VERSION,
        items: [],
        totalAmount: 0,
      });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/guest");
  const recoveryCard = page.locator(".card")
    .filter({ hasText: "처리 중인 결제 결과 복구" })
    .first();
  await recoveryCard.getByLabel("휴대폰 번호").fill("01011111111");
  await recoveryCard.getByRole("button", { name: "인증코드 발송" }).click();
  await recoveryCard.getByLabel("인증코드").fill("123456");
  await recoveryCard.getByRole("button", { name: "결제 결과 복구" }).click();

  await expect.poll(() => page.evaluate(() => ({
    recovery: sessionStorage.getItem("guest_payment_status_recovery"),
    statusToken: sessionStorage.getItem(
      "hg_payment_status_token:stale-recovery-order",
    ),
  }))).toEqual({ recovery: null, statusToken: null });
  await expect(
    recoveryCard.getByText("결제 상태 조회 정보를 복구했습니다."),
  ).toHaveCount(0);
});

test("@identity sessionStorage가 차단된 소셜 callback은 기본 경로로 완료된다", async ({
  page,
}) => {
  await page.addInitScript(() => {
    const originalGetItem = Storage.prototype.getItem;
    const originalSetItem = Storage.prototype.setItem;
    const originalRemoveItem = Storage.prototype.removeItem;
    Storage.prototype.getItem = function getItem(key: string) {
      if (this === window.sessionStorage) {
        throw new DOMException("sessionStorage disabled", "SecurityError");
      }
      return originalGetItem.call(this, key);
    };
    Storage.prototype.setItem = function setItem(key: string, value: string) {
      if (this === window.sessionStorage) {
        throw new DOMException("sessionStorage disabled", "SecurityError");
      }
      originalSetItem.call(this, key, value);
    };
    Storage.prototype.removeItem = function removeItem(key: string) {
      if (this === window.sessionStorage) {
        throw new DOMException("sessionStorage disabled", "SecurityError");
      }
      originalRemoveItem.call(this, key);
    };
  });
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 101,
        email: "social-callback@example.com",
        name: "소셜 콜백 회원",
        phone: "01011111111",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, {
        cartVersion: EMPTY_CART_VERSION,
        items: [],
        totalAmount: 0,
      });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/auth/callback");

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("link", { name: "소셜 콜백 회원" })).toBeVisible();
});

test("공개 Q&A 실패는 재시도하고 홈 loader 실패는 오류 경계로 응답한다", async ({
  page,
}) => {
  let qnaAttempts = 0;
  const product = {
    id: 42,
    name: "오류 복구 작품",
    description: null,
    category: "테스트",
    type: "READY_STOCK",
    price: 12000,
    imageUrl: null,
    available: true,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
  };

  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, guestCustomerError, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/products/42") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page") {
      qnaAttempts += 1;
      await fulfillJson(
        route,
        qnaAttempts <= 3
          ? temporaryError
          : { content: [], hasMore: false, nextCursor: null },
        qnaAttempts <= 3 ? 503 : 200,
      );
      return;
    }
    if (pathname === "/api/v1/products/42/reviews") {
      await fulfillJson(route, {
        content: [],
        filteredCount: 0,
        hasMore: false,
        nextCursor: null,
        summary: {
          averageRating: 0,
          histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 },
          reviewCount: 0,
        },
      });
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-07",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }
    if (pathname === "/api/v1/products" || pathname === "/api/v1/classes") {
      await fulfillJson(route, []);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/products/42");
  const qnaSection = page.locator(".card").filter({ hasText: "Q&A" }).last();
  await expect(qnaSection.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(qnaSection.getByText("등록된 Q&A가 없습니다.")).toHaveCount(0);
  await qnaSection.getByRole("button", { name: "다시 시도" }).click();
  await expect(qnaSection.getByText("등록된 Q&A가 없습니다.")).toBeVisible();

  const homeFixtures = homeSsrFixtures({ workshop: { name: "해피갤러리" } })
    .map((fixture) => fixture.path === "/api/v1/notices"
      ? { ...fixture, status: 503, json: temporaryError }
      : fixture);
  await replaceSsrUpstreamFixtures(...homeFixtures);
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "페이지를 불러오지 못했습니다" })).toBeVisible();
  await expect(page.getByText("공지사항이 없습니다.")).toHaveCount(0);
});

test("@identity 내 정보와 소셜 조회 실패는 0건이나 미연결로 단정하지 않고 다시 조회한다", async ({
  page,
}) => {
  const failingPaths = new Set([
    "/api/v1/me/orders",
    "/api/v1/me/bookings",
    "/api/v1/me/passes",
    "/api/v1/me/inquiries",
    "/api/v1/me/inquiries/page",
    "/api/v1/me/social-accounts",
  ]);

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 101,
        email: "resilience@example.com",
        name: "조회 복구 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (failingPaths.has(pathname)) {
      await fulfillJson(route, temporaryError, 503);
      return;
    }
    if (pathname === "/api/v1/me/social-accounts") {
      await fulfillJson(route, { linkedProviders: [] });
      return;
    }
    if (
      pathname === "/api/v1/me/orders"
      || pathname === "/api/v1/me/bookings"
      || pathname === "/api/v1/me/passes"
      || pathname === "/api/v1/me/inquiries"
    ) {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/me/inquiries/page") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my");
  const ordersSection = page.locator("#my-orders");
  const socialSection = page.getByRole("heading", { name: "소셜 로그인" })
    .locator("xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' border-top ')][1]");

  await expect(ordersSection.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(ordersSection.getByText("주문 내역이 없습니다.")).toHaveCount(0);
  await expect(page.getByText("아직 주문이 없습니다.")).toHaveCount(0);
  await expect(page.getByText("총 0건")).toHaveCount(0);
  await expect(socialSection.getByRole("button", { name: "연결", exact: true })).toHaveCount(0);
  await expect(socialSection.getByRole("button", { name: "다시 시도" })).toBeVisible();

  failingPaths.delete("/api/v1/me/orders");
  await ordersSection.getByRole("button", { name: "다시 시도" }).click();
  await expect(ordersSection.getByText("주문 내역이 없습니다.")).toBeVisible();

  failingPaths.delete("/api/v1/me/social-accounts");
  await socialSection.getByRole("button", { name: "다시 시도" }).click();
  await expect(socialSection.getByRole("button", { name: "연결", exact: true }))
    .toHaveCount(2);

  await page.goto("/my/inquiries");
  await expect(page.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(page.getByText("등록된 문의가 없습니다.")).toHaveCount(0);

  failingPaths.delete("/api/v1/me/inquiries/page");
  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByText("등록된 문의가 없습니다.")).toBeVisible();
});

test("@order 클레임 내역 조회 실패 중에는 접수 가능 수량과 신청 폼을 노출하지 않는다", async ({
  page,
}) => {
  let claimAttempts = 0;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 102,
        email: "claim-resilience@example.com",
        name: "클레임 복구 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/me/orders/73") {
      await fulfillJson(route, {
        approvalDeadlineAt: null,
        fulfillment: null,
        items: [{
          careInstructions: null,
          orderItemId: 731,
          productId: 42,
          productName: "클레임 대상 작품",
          productType: "READY_STOCK",
          productionLeadDays: null,
          qty: 1,
          specification: null,
          unitPrice: 12000,
        }],
        orderId: 73,
        orderNumber: "HG-RESILIENCE-73",
        paidAt: "2026-07-28T10:00:00",
        refund: null,
        shippingFee: 0,
        status: "DELIVERED",
        totalAmount: 12000,
      });
      return;
    }
    if (pathname === "/api/v1/me/orders/73/claims") {
      claimAttempts += 1;
      await fulfillJson(
        route,
        claimAttempts <= 2 ? temporaryError : [],
        claimAttempts <= 2 ? 503 : 200,
      );
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/orders/73");
  const claimSection = page.locator("section").filter({ hasText: "반품·교환" }).last();
  await expect(claimSection.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(claimSection.getByLabel("접수 유형")).toHaveCount(0);
  await expect(claimSection.getByText("접수 가능 1개")).toHaveCount(0);

  await claimSection.getByRole("button", { name: "다시 시도" }).click();
  await expect(claimSection.getByLabel("접수 유형")).toBeVisible();
  await expect(claimSection.getByText("접수 가능 1개")).toBeVisible();
});

test("@smoke @order 주문 혜택과 환불 구성 요소를 서버 스냅샷대로 구분해 표시한다", async ({
  page,
}) => {
  const order = {
    approvalDeadlineAt: null,
    couponDiscountAmount: 2000,
    fulfillment: null,
    issuedCouponId: 71,
    items: [{
      careInstructions: null,
      couponDiscountAmount: 2000,
      grossAmount: 12000,
      netPaidAmount: 7000,
      orderItemId: 751,
      productId: 44,
      productName: "혜택 적용 작품",
      productType: "READY_STOCK",
      productionLeadDays: null,
      qty: 1,
      rewardUsedAmount: 3000,
      specification: null,
      unitPrice: 12000,
    }],
    orderId: 75,
    orderNumber: "HG-BENEFIT-75",
    paidAt: "2026-08-08T10:00:00",
    pgPaidAmount: 7000,
    productAmount: 12000,
    refund: {
      amount: 10000,
      pgRefundAmount: 7000,
      restoreCoupon: true,
      rewardRestoreAmount: 3000,
      rewardRevokeAmount: 70,
      status: "SUCCEEDED",
    },
    rewardEarnBase: 7000,
    rewardUsedAmount: 3000,
    shippingFee: 0,
    status: "CUSTOMER_CANCELED",
    totalAmount: 10000,
  };

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 106,
        email: "benefit-display@example.com",
        name: "혜택 표시 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/me/orders/75") {
      await fulfillJson(route, order);
      return;
    }
    if (pathname === "/api/v1/me/orders/76") {
      await fulfillJson(route, {
        ...order,
        couponDiscountAmount: 12000,
        issuedCouponId: 72,
        orderId: 76,
        orderNumber: "HG-COUPON-ONLY-76",
        pgPaidAmount: 0,
        refund: {
          amount: 0,
          pgRefundAmount: 0,
          restoreCoupon: true,
          rewardRestoreAmount: 0,
          rewardRevokeAmount: 0,
          status: "SUCCEEDED",
        },
        rewardEarnBase: 0,
        rewardUsedAmount: 0,
        totalAmount: 0,
        items: order.items.map((item) => ({
          ...item,
          couponDiscountAmount: 12000,
          netPaidAmount: 0,
          rewardUsedAmount: 0,
        })),
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/orders/75");
  const orderCard = page.locator(".card").filter({ hasText: "HG-BENEFIT-75" }).first();
  await expect(orderCard.getByText("카드·간편결제 금액", { exact: true }).first().locator("..")).toContainText(
    "₩7,000",
  );
  await expect(orderCard.locator("tfoot tr").filter({ hasText: "상품 금액" })).toContainText("₩12,000");
  await expect(orderCard.locator("tfoot tr").filter({ hasText: "쿠폰 할인" })).toContainText("-₩2,000");
  await expect(orderCard.locator("tfoot tr").filter({ hasText: "적립금 사용" })).toContainText("-₩3,000");
  await expect(orderCard.locator("tfoot tr").filter({ hasText: "카드·간편결제 금액" })).toContainText("₩7,000");

  const refundAlert = orderCard.getByRole("alert").filter({ hasText: "환불 완료" });
  await expect(refundAlert).toContainText("₩10,000의 고객 반환 처리가 완료되었습니다.");
  await expect(refundAlert.getByText(/결제사 환불 ₩7,000 · 완료/)).toBeVisible();
  await expect(refundAlert.getByText(/적립금 복원 3,000P · 완료/)).toBeVisible();
  await expect(refundAlert.getByText(/지급 적립금 회수 70P · 완료/)).toBeVisible();
  await expect(refundAlert.getByText(/쿠폰 사용 상태 정리 · 완료/)).toBeVisible();

  await page.goto("/my/orders/76");
  const couponOnlyCard = page.locator(".card").filter({ hasText: "HG-COUPON-ONLY-76" }).first();
  await expect(couponOnlyCard.getByRole("alert").getByText(
    "결제 금액 반환 없이 쿠폰 사용 상태 정리가 완료되었습니다.",
  )).toBeVisible();
  await expect(couponOnlyCard.getByRole("alert").getByText(
    /쿠폰 사용 상태 정리 · 완료/,
  )).toBeVisible();
});

test("@smoke @order 주문 취소 실패는 확인 모달 안에서 사유를 표시하고 재시도를 허용한다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "order-cancel-resilience-token",
    url: baseURL,
  }]);

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 105,
        email: "order-action-resilience@example.com",
        name: "주문 취소 복구 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/me/orders/74" && route.request().method() === "GET") {
      await fulfillJson(route, {
        approvalDeadlineAt: "2099-01-02T10:00:00",
        fulfillment: null,
        items: [{
          careInstructions: null,
          orderItemId: 741,
          productId: 43,
          productName: "취소 복구 작품",
          productType: "READY_STOCK",
          productionLeadDays: null,
          qty: 1,
          specification: null,
          unitPrice: 15000,
        }],
        orderId: 74,
        orderNumber: "HG-RESILIENCE-74",
        paidAt: "2026-07-28T10:00:00",
        refund: null,
        shippingFee: 0,
        status: "PAID_APPROVAL_PENDING",
        totalAmount: 15000,
      });
      return;
    }
    if (pathname === "/api/v1/me/orders/74" && route.request().method() === "DELETE") {
      await fulfillJson(route, temporaryError, 503);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/orders/74");
  await page.getByRole("button", { name: "주문 취소" }).click();
  const dialog = page.getByRole("dialog", { name: "주문 취소 및 환불 요청" });
  await dialog.getByRole("button", { name: "취소하고 환불 요청" }).click();

  await expect(dialog).toBeVisible();
  await expect(dialog.getByText("서비스를 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."))
    .toBeVisible();
  await expect(dialog.getByRole("button", { name: "취소하고 환불 요청" })).toBeEnabled();
});

test("@smoke @identity 인증과 회원 장바구니 오류를 비회원·빈 상태로 오인하지 않고 복구한다", async ({
  baseURL,
  context,
  page,
}) => {
  let meAttempts = 0;
  let cartAttempts = 0;
  let updateAttempts = 0;
  let deleteAttempts = 0;
  let releaseUpdate: (() => void) | undefined;
  let releaseDelete: (() => void) | undefined;
  const updateBlocked = new Promise<void>((resolve) => {
    releaseUpdate = resolve;
  });
  const deleteBlocked = new Promise<void>((resolve) => {
    releaseDelete = resolve;
  });

  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "frontend-resilience-csrf",
    url: baseURL,
  }]);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      meAttempts += 1;
      if (meAttempts === 1) {
        await fulfillJson(route, temporaryError, 503);
        return;
      }
      await fulfillJson(route, {
        id: 103,
        email: "cart-resilience@example.com",
        name: "장바구니 복구 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/cart" && request.method() === "GET") {
      cartAttempts += 1;
      if (cartAttempts <= 2) {
        await fulfillJson(route, temporaryError, 503);
        return;
      }
      await fulfillJson(route, {
        cartVersion: "a".repeat(64),
        items: [{
          available: true,
          careInstructions: null,
          price: 12000,
          productId: 42,
          productName: "복구 확인 작품",
          productType: "READY_STOCK",
          productionLeadDays: null,
          qty: 1,
          specification: null,
          subtotal: 12000,
        }],
        totalAmount: 12000,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart/items/42" && request.method() === "PUT") {
      updateAttempts += 1;
      await updateBlocked;
      await fulfillJson(route, temporaryError, 503);
      return;
    }
    if (pathname === "/api/v1/me/cart/items/42" && request.method() === "DELETE") {
      deleteAttempts += 1;
      await deleteBlocked;
      await fulfillJson(route, temporaryError, 503);
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-07",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/cart");
  await expect(page.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(page.getByText("로그인하면 장바구니를 이용할 수 있습니다.")).toHaveCount(0);

  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(page.getByText("장바구니가 비어 있습니다.")).toHaveCount(0);

  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByText("복구 확인 작품", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "매장 수령" }).click();

  const increaseButton = page.getByRole("button", { name: "+", exact: true });
  const deleteButton = page.getByRole("button", { name: "삭제", exact: true });
  const checkoutButton = page.getByRole("button", { name: "결제하기", exact: true });
  await expect(checkoutButton).toBeEnabled();
  await increaseButton.click();
  await expect(page.getByRole("status")).toContainText("장바구니 변경을 반영하고 있습니다.");
  await expect(increaseButton).toBeDisabled();
  await expect(deleteButton).toBeDisabled();
  await expect(checkoutButton).toBeDisabled();
  expect(updateAttempts).toBe(1);

  releaseUpdate?.();
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(page.getByRole("alert")).toContainText(
    "서비스를 일시적으로 사용할 수 없습니다.",
  );
  await expect(page.getByText("복구 확인 작품", { exact: true })).toBeVisible();
  await expect(checkoutButton).toBeEnabled();

  await deleteButton.click();
  await expect(page.getByRole("status")).toContainText("장바구니 변경을 반영하고 있습니다.");
  await expect(increaseButton).toBeDisabled();
  await expect(deleteButton).toBeDisabled();
  await expect(checkoutButton).toBeDisabled();
  expect(deleteAttempts).toBe(1);

  releaseDelete?.();
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(page.getByRole("alert")).toContainText(
    "서비스를 일시적으로 사용할 수 없습니다.",
  );
  await expect(page.getByText("복구 확인 작품", { exact: true })).toBeVisible();
});
