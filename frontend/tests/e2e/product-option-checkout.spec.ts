import { expect, test, type Page, type Route } from "@playwright/test";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

test.afterEach(clearSsrUpstreamFixtures);

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function openOptionProduct(page: Page, quantity: number) {
  const product: ProductDetailResponse = {
    id: 80, name: "각인 키링", type: "MADE_TO_ORDER", price: 10000, available: true,
    description: null, category: null, imageUrl: null, specification: "가죽 키링",
    careInstructions: null, productionLeadDays: 7,
    optionGroups: [
      { key: "color", name: "색상", type: "SELECT", required: true, sortOrder: 0,
        inputMaxLength: null, inputPlaceholder: null, inputPriceAdjustment: null,
        values: [{ key: "brown", name: "브라운", sortOrder: 0 }, { key: "blue", name: "블루", sortOrder: 1 }] },
      { key: "engraving", name: "각인 문구", type: "TEXT", required: false, sortOrder: 1,
        inputMaxLength: 30, inputPlaceholder: null, inputPriceAdjustment: 1000, values: [] },
    ],
    variants: [
      { id: 801, active: true, quantity, priceAdjustment: 2000, selections: [{ groupKey: "color", valueKey: "brown" }] },
      { id: 802, active: true, quantity: 3, priceAdjustment: 4000, selections: [{ groupKey: "color", valueKey: "blue" }] },
    ],
  };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/80", product));
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/me") return json(route, { code: "UNAUTHORIZED" }, 401);
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
  });
}
