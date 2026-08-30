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
