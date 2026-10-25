import { expect, test } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";
import {
  EMPTY_CURSOR_PAGE,
  EMPTY_REVIEW_PAGE,
  fulfillJson,
  navigateInApp,
  orderPolicyFixture,
  productDetailFixture,
  UNAUTHORIZED_ERROR,
} from "./mock-support";

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

test("@smoke client navigation의 최신 loader 데이터가 기존 query cache보다 우선한다", async ({ page }) => {
  const firstProduct = productDetailFixture({ name: "이전 loader 작품" });
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
      await fulfillJson(route, UNAUTHORIZED_ERROR, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/products/42/reviews") {
      await fulfillJson(route, EMPTY_REVIEW_PAGE);
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page") {
      await fulfillJson(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, orderPolicyFixture());
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

  const latestProduct = productDetailFixture({ name: "최신 loader 작품" });
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
  const selected = productDetailFixture({ name: "검색 조건 보존 작품" });
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
      await fulfillJson(route, UNAUTHORIZED_ERROR, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname.endsWith("/reviews")) {
      await fulfillJson(route, EMPTY_REVIEW_PAGE);
      return;
    }
    if (pathname.endsWith("/qna/page")) {
      await fulfillJson(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, orderPolicyFixture());
      return;
    }
    await fulfillJson(route, []);
  });
  const response = await page.goto(`/products?${query}`);
  expect(await response?.text()).toContain(selected.name);
  await expect(page.getByLabel("검색", { exact: true })).toHaveValue("보존");
  await expect(page.getByLabel("정렬", { exact: true })).toHaveValue("price_asc");
  await page.getByRole("link", { name: new RegExp(selected.name) }).click();
  // 목록 카드에도 같은 이름의 제목이 있으므로 상세 URL로 이동을 먼저 확인한다.
  await expect(page).toHaveURL(new RegExp(`/products/${selected.id}$`));
  await expect(page.getByRole("heading", { level: 1, name: selected.name })).toBeVisible();
  await page.goBack();
  await expect(page.getByLabel("검색", { exact: true })).toHaveValue("보존");
  await page.reload();
  await expect(page.getByRole("navigation", { name: "카테고리", exact: true }).getByRole("link", { name: "테스트" }))
    .toHaveAttribute("aria-current", "page");
  await expect(page.getByText(selected.name, { exact: true })).toBeVisible();
  await page.getByLabel("검색", { exact: true }).fill("없는작품");
  await expect(page.getByText("조건에 맞는 작품이 없습니다.")).toBeVisible();
  await expect(page.getByLabel("검색", { exact: true })).toBeFocused();
  await page.getByRole("button", { name: "초기화", exact: true }).click();
  await expect(page).toHaveURL(/\/products$/);
  await expect(page.getByText(selected.name, { exact: true })).toBeVisible();
});

test("머리글 카테고리는 그 카테고리 제목·경로·탭이 있는 작품 목록으로 이어진다", async ({ page }) => {
  const leather = productDetailFixture({ name: "카테고리 가죽 작품", category: "가죽" });
  const query = new URLSearchParams({ category: "가죽" });
  await replaceSsrUpstreamFixtures(
    ssrApiFixture(`/products?${query}`, [leather]),
    ssrApiFixture("/products/categories", ["가죽", "레진"]),
  );
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/products" || pathname === "/api/v1/products/categories") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/me") {
      await fulfillJson(route, UNAUTHORIZED_ERROR, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto(`/products?${query}`);

  // 전체 목록과 구분되도록 제목·문서 제목·경로가 카테고리를 따른다.
  await expect(page.getByRole("heading", { level: 1, name: "가죽" })).toBeVisible();
  await expect(page).toHaveTitle("가죽 작품 | 해피갤러리");
  await expect(page.getByRole("navigation", { name: "경로" }).getByRole("link", { name: "전체 작품" }))
    .toHaveAttribute("href", "/products");
  const tabs = page.getByRole("navigation", { name: "카테고리", exact: true });
  await expect(tabs.getByRole("link", { name: "가죽" })).toHaveAttribute("aria-current", "page");
  await expect(tabs.getByRole("link", { name: "레진" })).toHaveAttribute("href", "/products?category=%EB%A0%88%EC%A7%84");
  await expect(page.getByText(leather.name, { exact: true })).toBeVisible();
});
