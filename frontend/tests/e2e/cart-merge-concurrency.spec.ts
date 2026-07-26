import { expect, test, type Page } from "@playwright/test";

const GUEST_CART_STORAGE_KEY = "hg_guest_cart";
const MERGE_REQUEST_STORAGE_KEY = "hg_guest_cart_merge_request";
const GUEST_CART_LOCK_NAME = "hg_guest_cart";

interface MergeRequest {
  idempotencyKey: string;
  items: Array<{ productId: number; qty: number }>;
}

test("여러 탭의 로그인 병합과 비회원 상품 추가는 순서대로 한 번씩 반영된다", async ({
  baseURL,
  context,
  page,
}) => {
  let authenticated = false;
  let guestProductPage: Page | null = null;
  let releaseFirstMerge: (() => void) | undefined;
  const firstMergeRelease = new Promise<void>((resolve) => {
    releaseFirstMerge = resolve;
  });
  const mergeRequests: MergeRequest[] = [];

  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "cart-merge-test-token",
    url: baseURL,
  }]);
  await context.route(/\/api\/v1\/me$/, async (route) => {
    if (!authenticated || route.request().frame().page() === guestProductPage) {
      await route.fulfill({
        status: 401,
        contentType: "application/json",
        body: JSON.stringify({ code: "UNAUTHORIZED", message: "로그인이 필요합니다." }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: 101,
        email: "cart-merge@example.com",
        name: "장바구니 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      }),
    });
  });
  await context.route(/\/api\/v1\/me\/cart$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ items: [], totalAmount: 0 }),
    });
  });
  await context.route(/\/api\/v1\/me\/cart\/merge$/, async (route) => {
    mergeRequests.push(route.request().postDataJSON() as MergeRequest);
    if (mergeRequests.length === 1) {
      await firstMergeRelease;
    }
    await route.fulfill({ status: 204 });
  });
  await context.route(/\/api\/v1\/products\/22$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: 22,
        name: "추가 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 12000,
        imageUrl: null,
        available: true,
      }),
    });
  });
  await context.route(/\/api\/v1\/products\/22\/qna$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: "[]",
    });
  });
  await context.route(/\/api\/v1\/me\/notifications\/unread-count$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0 }),
    });
  });
  await context.route(/\/api\/v1\/workshop$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ name: "해피갤러리" }),
    });
  });

  const secondPage = await context.newPage();
  await Promise.all([page.goto("/cart"), secondPage.goto("/cart")]);

  await page.evaluate(([storageKey, value]) => {
    localStorage.setItem(storageKey, value);
  }, [
    GUEST_CART_STORAGE_KEY,
    JSON.stringify([{ productId: 11, qty: 2, lineageId: "shared-lineage" }]),
  ] as const);
  await expect(secondPage.locator('a[href="/cart"] .badge')).toHaveText("2");

  authenticated = true;
  await Promise.all([page.reload(), secondPage.reload()]);
  await expect.poll(() => mergeRequests.length).toBe(1);

  guestProductPage = await context.newPage();
  await guestProductPage.goto("/products/22");
  const addButton = guestProductPage.getByRole("button", { name: "장바구니 담기" });
  await addButton.click();
  await expect(guestProductPage.getByRole("button", { name: "담는 중..." })).toBeVisible();
  expect(await guestProductPage.evaluate((storageKey) => {
    const stored = localStorage.getItem(storageKey);
    if (!stored) return false;
    return (JSON.parse(stored) as Array<{ productId: number }>)
      .some((item) => item.productId === 22);
  }, GUEST_CART_STORAGE_KEY)).toBe(false);

  if (!releaseFirstMerge) {
    throw new Error("첫 병합 응답 해제 함수가 준비되지 않았습니다.");
  }
  releaseFirstMerge();

  await expect.poll(() => mergeRequests.length).toBe(2);
  await expect(guestProductPage.getByText("장바구니에 추가되었습니다.")).toBeVisible();
  await Promise.all([page, secondPage, guestProductPage].map((targetPage) =>
    targetPage.evaluate(async (lockName) => {
      await navigator.locks.request(lockName, () => undefined);
    }, GUEST_CART_LOCK_NAME),
  ));
  await Promise.all([
    expect(page.getByText("장바구니가 비어 있습니다.")).toBeVisible(),
    expect(secondPage.getByText("장바구니가 비어 있습니다.")).toBeVisible(),
  ]);
  expect(mergeRequests).toHaveLength(2);
  expect(mergeRequests.map((request) => request.items)).toEqual([
    [{ productId: 11, qty: 2 }],
    [{ productId: 22, qty: 1 }],
  ]);
  expect(mergeRequests[0]?.idempotencyKey).not.toBe(mergeRequests[1]?.idempotencyKey);
  await expect.poll(() => page.evaluate(
    ([cartKey, requestKey]) => ({
      cart: localStorage.getItem(cartKey),
      mergeRequest: localStorage.getItem(requestKey),
    }),
    [GUEST_CART_STORAGE_KEY, MERGE_REQUEST_STORAGE_KEY] as const,
  )).toEqual({ cart: null, mergeRequest: null });
});
