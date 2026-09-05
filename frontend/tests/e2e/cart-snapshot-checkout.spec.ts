import { expect, test, type Route } from "@playwright/test";
import { skipExternalFonts } from "./external-fonts";

test.beforeEach(skipExternalFonts);

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

test("@payment 선택 구매는 미선택 상품을 제외하고 충돌 뒤에도 선택을 유지해 수동 재결제한다", async ({
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
          cartItemId: 42,
          productVariantId: null,
          options: [],
          basePrice: 12000,
          variantPriceAdjustment: 0,
          textOptionPriceAdjustment: 0,
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
        }, {
          cartItemId: 84, productId: 84, productVariantId: null,
          productName: "나중에 구매할 작품", productType: "MADE_TO_ORDER",
          options: [], basePrice: 7000, price: 7000, qty: 1, subtotal: 7000,
          variantPriceAdjustment: 0, textOptionPriceAdjustment: 0,
          available: true, specification: null, careInstructions: null, productionLeadDays: 7,
        }],
        totalAmount: 12000 * qty + 7000,
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
  await page.getByRole("checkbox", { name: "전체 선택" }).uncheck();
  await expect(page.getByText("구매할 상품을 선택해 주세요.")).toBeVisible();
  await page.getByRole("checkbox", { name: "스냅샷 확인 작품 선택" }).check();
  await expect(page.getByRole("checkbox", { name: "나중에 구매할 작품 선택" })).not.toBeChecked();
  await page.getByRole("button", { name: "매장 수령" }).click();
  const checkoutButton = page.getByRole("button", { name: "결제하기", exact: true });
  await expect(checkoutButton).toBeDisabled();
  await expect(page.locator(".store-purchase-card").getByRole("alert")).toContainText(
    "장바구니 최신 정보를 확인할 수 없어 결제를 진행할 수 없습니다.",
  );
  await expect(page.locator(".store-purchase-card")).toContainText("₩12,000");
  await expect(page.locator(".store-purchase-card")).not.toContainText("₩19,000");
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
    selectedCartItemIds: [42],
    madeToOrderConsent: false,
  });
  await expect(page.locator(".store-purchase-card").getByRole("alert")).toContainText(
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
    selectedCartItemIds: [42],
  });
  await expect.poll(() => page.evaluate(() => {
    const browserGlobal = globalThis as unknown as {
      cartSnapshotTossRequests: unknown[];
    };
    return browserGlobal.cartSnapshotTossRequests.length;
  })).toBe(1);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy();
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
          cartItemId: 43,
          productVariantId: null,
          options: [],
          basePrice: 12000,
          variantPriceAdjustment: 0,
          textOptionPriceAdjustment: 0,
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

  await expect(page.locator(".store-purchase-card").getByRole("alert")).toContainText(
    "요청을 완료할 수 없습니다. 안내된 조건을 확인하거나 잠시 후 다시 시도해 주세요.",
  );
  await expect(page.locator(".store-purchase-card").getByRole("alert")).not.toContainText(
    "장바구니 내용이 변경되어 최신 정보로 갱신했습니다.",
  );
  await page.waitForTimeout(300);
  expect(cartReads).toBe(readsBeforeCheckout);
});

test("@payment 각인 두 항목의 합계가 재고를 넘으면 한 항목만 선택해 구매할 수 있다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  const cartVersion = "d".repeat(64);
  const preparePayloads: Array<Record<string, unknown>> = [];
  const cartWrites: string[] = [];
  await context.addCookies([{
    name: "XSRF-TOKEN", value: "cart-selected-stock-token", url: baseURL,
  }]);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      selectedStockPayments: unknown[];
      TossPayments: () => { payment: () => { requestPayment: (request: unknown) => Promise<void> } };
    };
    browserGlobal.selectedStockPayments = [];
    browserGlobal.TossPayments = () => ({
      payment: () => ({ requestPayment: async (request) => {
        browserGlobal.selectedStockPayments.push(request);
      } }),
    });
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    if (pathname.startsWith("/api/v1/me/cart") && request.method() !== "GET") {
      cartWrites.push(request.method());
    }
    if (pathname === "/api/v1/me/cart" && request.method() === "GET") {
      await fulfillJson(route, {
        cartVersion, totalAmount: 0,
        items: ["HAPPY", "GALLERY"].map((value, index) => ({
          cartItemId: 91 + index, productId: 90, productVariantId: 900,
          productName: "각인 키링", productType: "MADE_TO_ORDER",
          options: [{ type: "TEXT", groupName: "각인 문구", value, priceAdjustment: 2000, sortOrder: 0 }],
          basePrice: 20000, variantPriceAdjustment: 0, textOptionPriceAdjustment: 2000,
          price: 22000, qty: 1, subtotal: 22000, available: false, availableQuantity: 1,
          specification: "소가죽 키링", careInstructions: null, productionLeadDays: 5,
        })),
      });
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      preparePayloads.push((request.postDataJSON() as { payload: Record<string, unknown> }).payload);
      await fulfillJson(route, {
        orderId: "cart-selected-stock-payment", amount: 22000, context: "ORDER",
        statusToken: "cart-selected-stock-status",
      });
      return;
    }
    const responses: Record<string, unknown> = {
      "/api/v1/me": {
        id: 303, email: "selected-stock@example.com", name: "선택 구매 회원",
        phone: "01033335556", phoneVerified: true, localPasswordEnabled: true,
      },
      "/api/v1/orders/policy": {
        shippingFee: 3000, madeToOrderConsentVersion: "2026-08",
        madeToOrderConsentText: "주문제작 동의",
      },
      "/api/v1/me/notifications/unread-count": { count: 0 },
      "/api/v1/workshop": { name: "해피갤러리", updatedAt: "2026-08-08T10:00:00", version: 1 },
      "/api/v1/me/coupons": [],
      "/api/v1/me/rewards": { availableBalance: 0, reservedBalance: 0, debtBalance: 0, history: [] },
    };
    if (Object.hasOwn(responses, pathname)) {
      await fulfillJson(route, responses[pathname]);
      return;
    }
    await fulfillJson(route, { code: "NOT_FOUND", message: "테스트에 정의되지 않은 요청입니다." }, 404);
  });

  await page.goto("/cart");
  await expect(page.locator("#cart-select-91")).toBeChecked();
  await expect(page.locator("#cart-select-92")).toBeChecked();
  await page.getByRole("button", { name: "매장 수령" }).click();
  await page.getByRole("checkbox", { name: "주문제작 동의", exact: true }).check();
  const checkoutButton = page.getByRole("button", { name: "결제하기", exact: true });
  const warning = page.getByRole("alert").filter({ hasText: "같은 상품·옵션은 합계 1개까지" });
  await expect(warning).toContainText("현재 2개를 선택했습니다.");
  await expect(checkoutButton).toBeDisabled();
  expect(preparePayloads).toHaveLength(0);

  await page.locator("#cart-select-92").uncheck();
  await expect(warning).toHaveCount(0);
  await expect(page.locator(".store-purchase-card")).toContainText("₩22,000");
  await expect(page.locator("tbody tr")).toHaveCount(2);
  await expect(checkoutButton).toBeEnabled();
  await checkoutButton.click();
  await expect.poll(() => preparePayloads.length).toBe(1);
  expect(preparePayloads[0]).toMatchObject({
    cartCheckout: true, expectedCartVersion: cartVersion, selectedCartItemIds: [91],
    madeToOrderConsent: true,
  });
  await expect.poll(() => page.evaluate(() =>
    (globalThis as unknown as { selectedStockPayments: unknown[] }).selectedStockPayments.length,
  )).toBe(1);
  expect(cartWrites).toEqual([]);
});
