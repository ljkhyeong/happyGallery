import { expect, test } from "@playwright/test";
import { EMPTY_CART_VERSION, fulfillJson, fulfillMemberCommon, installCsrfCookie, memberFixture } from "./mock-support";

const item = (id: number, name: string, options: { type: string; groupName: string; value: string; priceAdjustment: number }[] = []) => ({
  orderItemId: id, productId: id, productName: name, productType: id === 42 ? "READY_STOCK" : "MADE_TO_ORDER",
  qty: id === 42 ? 3 : 1, unitPrice: 10000, grossAmount: 30000, couponDiscountAmount: 0, rewardUsedAmount: 0, netPaidAmount: 30000,
  productVariantId: id === 42 ? null : id * 10, basePrice: 10000, variantPriceAdjustment: 0, textOptionPriceAdjustment: 0,
  options, specification: null, careInstructions: null, productionLeadDays: null,
});

test("다시 담기는 현재 가격과 재고를 확인하고 재시도 키를 유지하며 변경 옵션과 각인은 재선택을 안내한다 @payment", async ({ page, context, baseURL }) => {
  await installCsrfCookie(context, baseURL);
  const items = [item(42, "이전 주문 머그"), item(43, "옵션 변경 상품", [{ type: "SELECT", groupName: "색상", value: "파랑", priceAdjustment: 0 }]),
    item(44, "각인 상품", [{ type: "TEXT", groupName: "각인", value: "홍길동", priceAdjustment: 0 }])];
  const requests: { idempotencyKey: string; items: unknown[]; expectedCustomerId: number }[] = [];
  let cartQty = 0;
  const member = memberFixture({ email: "reorder@example.com" });
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/v1/me/cart") return fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, totalAmount: cartQty * 15000,
      items: cartQty ? [{ productId: 42, productVariantId: null, qty: cartQty, available: true }] : [] });
    if (await fulfillMemberCommon(route, { member })) return;
    if (path === "/api/v1/me/cart/merge") {
      requests.push(route.request().postDataJSON());
      if (requests.length === 1) return fulfillJson(route, { code: "SERVICE_UNAVAILABLE", message: "잠시 후 재시도" }, 503);
      cartQty = 2; return route.fulfill({ status: 204 });
    }
    if (path === "/api/v1/me/orders/200") return fulfillJson(route, { orderId: 200, orderNumber: "다시 담기 주문", status: "DELIVERED", items,
      productAmount: 50000, shippingFee: 0, totalAmount: 50000, pgPaidAmount: 50000, couponDiscountAmount: 0, rewardUsedAmount: 0,
      refund: null, receiptUrl: null, paidAt: "2026-09-05T10:00:00", approvalDeadlineAt: null, fulfillment: null });
    if (path.startsWith("/api/v1/products/")) {
      const id = Number(path.split("/").at(-1));
      return fulfillJson(route, { id, name: "현재 상품명", category: "공예", type: id === 42 ? "READY_STOCK" : "MADE_TO_ORDER", price: 15000,
        imageUrl: null, available: true, stockQuantity: 2, specification: null, careInstructions: null, productionLeadDays: 5,
        optionGroups: id === 43 ? [{ key: "color", name: "색상", type: "SELECT", required: true, sortOrder: 0, values: [{ key: "red", name: "빨강", sortOrder: 0 }] }]
          : id === 44 ? [{ key: "engraving", name: "각인", type: "TEXT", required: false, sortOrder: 0, values: [], inputMaxLength: 20, inputPriceAdjustment: 0 }] : [],
        variants: id === 42 ? [] : [{ id: id * 10, active: true, quantity: 5, priceAdjustment: 0,
          selections: id === 43 ? [{ groupKey: "color", valueKey: "red" }] : [] }],
      });
    }
    return fulfillJson(route, []);
  });
  await page.goto("/my/orders/200");
  await page.getByRole("button", { name: "이전 주문 머그 다시 담기" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText("현재 개당 가격:")).toContainText("15,000");
  await expect(dialog.getByRole("button", { name: "장바구니에 담기" })).toBeDisabled();
  await dialog.getByLabel("다시 담을 수량").fill("2");
  await dialog.getByRole("button", { name: "장바구니에 담기" }).click();
  await expect(dialog.getByRole("alert")).toBeVisible();
  await dialog.getByRole("button", { name: "장바구니에 담기" }).click();
  await expect(dialog).toHaveCount(0);
  expect(requests).toHaveLength(2);
  expect(requests[1]).toEqual(requests[0]);
  expect(requests[1]).toMatchObject({ expectedCustomerId: 501, items: [{ productId: 42, productVariantId: null, textInputs: [], qty: 2 }] });
  await page.getByRole("button", { name: "이전 주문 머그 다시 담기" }).click();
  await expect(dialog.getByRole("button", { name: "장바구니에 담기" })).toBeDisabled();
  await dialog.getByRole("button", { name: "닫기", exact: true }).click();
  // 옵션이 바뀐 작품은 처음부터 고르고, 각인만 다시 쓰면 되는 작품은 같은 옵션 조합을 골라 둔다.
  for (const [name, href] of [["옵션 변경 상품", "/products/43"], ["각인 상품", "/products/44?variantId=440"]]) {
    await page.getByRole("button", { name: `${name} 다시 담기` }).click();
    await expect(dialog.getByRole("link", { name: "옵션 다시 선택" })).toHaveAttribute("href", href);
    await expect(dialog.getByRole("button", { name: "장바구니에 담기" })).toHaveCount(0);
    await dialog.getByRole("button", { name: "닫기", exact: true }).click();
  }
});

test("비회원은 조회한 주문의 상품을 현재 가격으로 이 기기의 장바구니에 다시 담는다 @payment", async ({ page }) => {
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (await fulfillMemberCommon(route, { member: null })) return;
    if (path === "/api/v1/orders/200") {
      expect(route.request().headers()["x-access-token"]).toBe("reorder-guest");
      return fulfillJson(route, { orderId: 200, orderNumber: "비회원 주문", status: "DELIVERED", items: [{ ...item(42, "비회원 머그"), qty: 1 }],
        productAmount: 10000, shippingFee: 0, totalAmount: 10000, pgPaidAmount: 10000, couponDiscountAmount: 0, rewardUsedAmount: 0,
        refund: null, receiptUrl: null, paidAt: "2026-09-05T10:00:00", approvalDeadlineAt: null, fulfillment: null });
    }
    if (path === "/api/v1/products/42") return fulfillJson(route, { id: 42, name: "현재 머그", category: "공예", type: "READY_STOCK", price: 15000,
      imageUrl: null, available: true, stockQuantity: 5, specification: null, careInstructions: null, productionLeadDays: null,
      optionGroups: [], variants: [] });
    return fulfillJson(route, []);
  });
  await page.goto("/guest/orders");
  await page.getByLabel("주문 번호", { exact: true }).fill("200");
  await page.getByLabel("조회 코드", { exact: true }).fill("reorder-guest");
  await page.getByRole("button", { name: "조회", exact: true }).click();
  await page.getByRole("button", { name: "비회원 머그 다시 담기" }).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText("현재 개당 가격:")).toContainText("15,000");
  await dialog.getByRole("button", { name: "장바구니에 담기" }).click();
  await expect(dialog).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => JSON.parse(localStorage.getItem("hg_guest_cart") ?? "[]"))).toMatchObject([
    { productId: 42, productVariantId: null, textInputs: [], qty: 1 },
  ]);
});
