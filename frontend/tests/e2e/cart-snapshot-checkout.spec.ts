import { expect, test, type Route } from "@playwright/test";

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

test("@payment 장바구니 충돌은 최신 버전을 다시 조회하고 다음 수동 결제에만 사용한다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const firstVersion = "a".repeat(64);
  const refreshedVersion = "b".repeat(64);
  let cartReads = 0;
  const preparePayloads: Array<Record<string, unknown>> = [];

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "cart-snapshot-checkout-token",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      cartSnapshotTossRequests: unknown[];
      TossPayments: () => {
        payment: () => {
          requestPayment: (request: unknown) => Promise<void>;
        };
      };
    };
    browserGlobal.cartSnapshotTossRequests = [];
    browserGlobal.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          browserGlobal.cartSnapshotTossRequests.push(request);
        },
      }),
    });
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 301,
        email: "cart-snapshot@example.com",
        name: "장바구니 스냅샷 회원",
        phone: "01033333333",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart" && request.method() === "GET") {
      cartReads += 1;
      const refreshed = cartReads > 2;
      const qty = refreshed ? 2 : 1;
      await fulfillJson(route, {
        ...(cartReads === 1
          ? {}
          : { cartVersion: refreshed ? refreshedVersion : firstVersion }),
        items: [{
          available: true,
          careInstructions: null,
          price: 12000,
          productId: 42,
          productName: "스냅샷 확인 작품",
          productType: "READY_STOCK",
          productionLeadDays: null,
          qty,
          specification: null,
          subtotal: 12000 * qty,
        }],
        totalAmount: 12000 * qty,
      });
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      const body = request.postDataJSON() as {
        payload: Record<string, unknown>;
      };
      preparePayloads.push(body.payload);
      if (preparePayloads.length === 1) {
        await fulfillJson(route, {
          code: "CART_SNAPSHOT_CHANGED",
          message: "장바구니가 변경되었습니다. 최신 장바구니를 확인한 뒤 다시 결제해 주세요.",
        }, 409);
        return;
      }
      await fulfillJson(route, {
        orderId: "cart-snapshot-payment",
        amount: 24000,
        context: "ORDER",
        statusToken: "cart-snapshot-status",
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

  await page.goto("/cart");
  await expect(page.getByText("스냅샷 확인 작품", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "매장 수령" }).click();
  const checkoutButton = page.getByRole("button", { name: "결제하기", exact: true });
  await expect(checkoutButton).toBeDisabled();
  await expect(page.getByRole("alert")).toContainText(
    "장바구니 최신 정보를 확인할 수 없어 결제를 진행할 수 없습니다.",
  );
  expect(preparePayloads).toHaveLength(0);

  await page.getByRole("button", { name: "장바구니 다시 확인" }).click();
  await expect.poll(() => cartReads).toBe(2);
  await expect(checkoutButton).toBeEnabled();

  await checkoutButton.click();
  await expect.poll(() => preparePayloads.length).toBe(1);
  await expect.poll(() => cartReads).toBe(3);
  expect(preparePayloads[0]).toMatchObject({
    cartCheckout: true,
    expectedCartVersion: firstVersion,
  });
  await expect(page.getByRole("alert")).toContainText(
    "수량과 금액을 다시 확인한 뒤 결제를 진행해 주세요.",
  );
  await expect(page.locator("tbody tr").first().locator("td").nth(1)).toContainText("2");
  await expect(checkoutButton).toBeEnabled();

  await page.waitForTimeout(300);
  expect(preparePayloads).toHaveLength(1);

  await checkoutButton.click();
  await expect.poll(() => preparePayloads.length).toBe(2);
  expect(preparePayloads[1]).toMatchObject({
    cartCheckout: true,
    expectedCartVersion: refreshedVersion,
  });
  await expect.poll(() => page.evaluate(() => {
    const browserGlobal = globalThis as unknown as {
      cartSnapshotTossRequests: unknown[];
    };
    return browserGlobal.cartSnapshotTossRequests.length;
  })).toBe(1);
});

test("@payment 장바구니 버전과 무관한 409는 원래 오류를 표시하고 자동 갱신하지 않는다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const cartVersion = "c".repeat(64);
  let cartReads = 0;

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "cart-generic-conflict-token",
    url: baseURL,
  }]);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 302,
        email: "cart-conflict@example.com",
        name: "장바구니 충돌 회원",
        phone: "01044444444",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart" && request.method() === "GET") {
      cartReads += 1;
      await fulfillJson(route, {
        cartVersion,
        items: [{
          available: true,
          careInstructions: null,
          price: 12000,
          productId: 43,
          productName: "재고 충돌 작품",
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
    if (pathname === "/api/v1/payments/prepare") {
      await fulfillJson(route, {
        code: "CONFLICT",
        message: "결제 준비 중 상품 판매 상태가 변경되었습니다.",
      }, 409);
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

  await page.goto("/cart");
  await expect(page.getByText("재고 충돌 작품", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "매장 수령" }).click();
  const readsBeforeCheckout = cartReads;
  await page.getByRole("button", { name: "결제하기", exact: true }).click();

  await expect(page.getByRole("alert")).toContainText(
    "처리 중 충돌이 감지되었습니다. 잠시 후 다시 시도해 주세요.",
  );
  await expect(page.getByRole("alert")).not.toContainText(
    "장바구니 내용이 변경되어 최신 정보로 갱신했습니다.",
  );
  await page.waitForTimeout(300);
  expect(cartReads).toBe(readsBeforeCheckout);
});
