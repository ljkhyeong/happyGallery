import { expect, test, type Page, type Route } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function navigateInApp(page: Page, path: string) {
  await page.evaluate((nextPath) => {
    window.history.pushState({}, "", nextPath);
    window.dispatchEvent(new PopStateEvent("popstate"));
  }, path);
}

function product(name: string) {
  return {
    id: 42,
    name,
    description: null,
    category: "테스트",
    type: "READY_STOCK",
    price: 12000,
    imageUrl: null,
    available: true,
    stockQuantity: 10,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
    optionGroups: [],
    variants: [],
  };
}

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

test("@smoke client navigation의 최신 loader 데이터가 기존 query cache보다 우선한다", async ({ page }) => {
  const firstProduct = product("이전 loader 작품");
  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/products/42", firstProduct),
    ssrApiFixture("/events", []),
  );

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/products/42" || pathname === "/api/v1/events") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
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
    if (pathname === "/api/v1/products/42/qna/page") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
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
    await fulfillJson(route, []);
  });

  const initialResponse = await page.goto("/products/42");
  if (!initialResponse) throw new Error("상품 상세 문서 응답을 확인할 수 없습니다.");
  const initialHtml = await initialResponse.text();
  expect(initialHtml).toContain(firstProduct.name);
  expect(initialHtml).toContain("https://happy-gallery.com/products/42");
  expect(initialHtml).toContain('type="application/ld+json"');
  const jsonLdNonce = await page
    .locator('script[type="application/ld+json"]')
    .evaluate((script: HTMLScriptElement) => script.nonce);
  expect(jsonLdNonce).not.toBe("");
  expect(initialResponse.headers()["content-security-policy-report-only"])
    .toContain(`'nonce-${jsonLdNonce}'`);
  await expect(page.getByRole("heading", { name: firstProduct.name })).toBeVisible();
  await navigateInApp(page, "/events");
  await expect(page.getByRole("heading", { name: "이벤트", exact: true })).toBeVisible();

  const latestProduct = product("최신 loader 작품");
  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/products/42", latestProduct),
    ssrApiFixture("/events", []),
  );
  await navigateInApp(page, "/products/42");

  await expect(page).toHaveTitle(`${latestProduct.name} | 해피갤러리`);
  await expect(page.getByRole("heading", { name: latestProduct.name })).toBeVisible();
  await expect.poll(() => page
    .locator('script[type="application/ld+json"]')
    .evaluate((script) => script.textContent ?? ""))
    .toContain(latestProduct.name);
  await expect(page.getByText(firstProduct.name, { exact: true })).toHaveCount(0);
});

test("상품 검색 조건은 SSR·뒤로 가기·새로고침에서 유지되고 빈 검색을 초기화할 수 있다", async ({ page }) => {
  const selected = product("검색 조건 보존 작품");
  const query = new URLSearchParams({ type: "READY_STOCK", category: "테스트", keyword: "보존", sort: "price_asc" });
  const emptyQuery = new URLSearchParams({ type: "READY_STOCK", category: "테스트", keyword: "없는작품", sort: "price_asc" });
  await replaceSsrUpstreamFixtures(
    ssrApiFixture(`/products?${query}`, [selected]),
    ssrApiFixture(`/products?${emptyQuery}`, []),
    ssrApiFixture("/products", [selected]),
    ssrApiFixture("/products/categories", ["테스트"]),
    ssrApiFixture("/products/42", selected),
  );
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/products" || pathname === "/api/v1/products/categories" || pathname === "/api/v1/products/42") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname.endsWith("/reviews")) {
      await fulfillJson(route, { content: [], filteredCount: 0, hasMore: false, nextCursor: null,
        summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
      return;
    }
    if (pathname.endsWith("/qna/page")) {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, { shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
      return;
    }
    await fulfillJson(route, []);
  });
  const response = await page.goto(`/products?${query}`);
  expect(await response?.text()).toContain(selected.name);
  await expect(page.getByLabel("검색", { exact: true })).toHaveValue("보존");
  await expect(page.getByLabel("정렬", { exact: true })).toHaveValue("price_asc");
  await page.getByRole("link", { name: new RegExp(selected.name) }).click();
  await expect(page.getByRole("heading", { name: selected.name })).toBeVisible();
  await page.goBack();
  await expect(page.getByLabel("검색", { exact: true })).toHaveValue("보존");
  await page.reload();
  await expect(page.getByLabel("카테고리", { exact: true })).toHaveValue("테스트");
  await expect(page.getByText(selected.name, { exact: true })).toBeVisible();
  await page.getByLabel("검색", { exact: true }).fill("없는작품");
  await expect(page.getByText("조건에 맞는 상품이 없습니다.")).toBeVisible();
  await expect(page.getByLabel("검색", { exact: true })).toBeFocused();
  await page.getByRole("button", { name: "초기화", exact: true }).click();
  await expect(page).toHaveURL(/\/products$/);
  await expect(page.getByText(selected.name, { exact: true })).toBeVisible();
});
