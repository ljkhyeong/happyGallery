import { expect, test, type Page } from "@playwright/test";

const GUEST_CART_STORAGE_KEY = "hg_guest_cart";
const MERGE_REQUEST_STORAGE_KEY = "hg_guest_cart_merge_request";
const GUEST_CART_LOCK_NAME = "hg_guest_cart";
const EMPTY_CART_VERSION = "0".repeat(64);

interface MergeRequest {
  expectedCustomerId: number;
  idempotencyKey: string;
  items: Array<{ productId: number; qty: number }>;
}

test("여러 탭의 로그인 병합과 잠금 대기 장바구니 수정은 순서대로 한 번씩 반영된다", async ({
  baseURL,
  context,
  page,
}) => {
  let authenticated = false;
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
    if (!authenticated) {
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
      body: JSON.stringify({ cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 }),
    });
  });
  await context.route(/\/api\/v1\/me\/cart\/merge$/, async (route) => {
    mergeRequests.push(route.request().postDataJSON() as MergeRequest);
    if (mergeRequests.length === 1) {
      await firstMergeRelease;
    }
    await route.fulfill({ status: 204 });
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

  const queuedCartEdit = secondPage.evaluate(async ([lockName, storageKey]) => {
    await navigator.locks.request(lockName, () => {
      const stored = localStorage.getItem(storageKey);
      const items = stored
        ? JSON.parse(stored) as Array<{ productId: number; qty: number; lineageId: string }>
        : [];
      localStorage.setItem(storageKey, JSON.stringify([
        ...items,
        { productId: 22, qty: 1, lineageId: "queued-lineage" },
      ]));
    });
  }, [GUEST_CART_LOCK_NAME, GUEST_CART_STORAGE_KEY] as const);
  await expect.poll(() => page.evaluate(async (lockName) => (
    (await navigator.locks.query()).pending?.some((lock) => lock.name === lockName) ?? false
  ), GUEST_CART_LOCK_NAME)).toBe(true);
  expect(await page.evaluate((storageKey) => {
    const stored = localStorage.getItem(storageKey);
    if (!stored) return false;
    return (JSON.parse(stored) as Array<{ productId: number }>)
      .some((item) => item.productId === 22);
  }, GUEST_CART_STORAGE_KEY)).toBe(false);

  if (!releaseFirstMerge) {
    throw new Error("첫 병합 응답 해제 함수가 준비되지 않았습니다.");
  }
  releaseFirstMerge();
  await queuedCartEdit;

  await expect.poll(() => mergeRequests.length).toBe(2);
  await Promise.all([page, secondPage].map((targetPage) =>
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
    [{ productId: 11, productVariantId: null, textInputs: [], qty: 2 }],
    [{ productId: 22, productVariantId: null, textInputs: [], qty: 1 }],
  ]);
  expect(mergeRequests.map((request) => request.expectedCustomerId)).toEqual([
    101,
    101,
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

test("@identity 병합 응답 전에 계정이 바뀌면 이전 요청과 비회원 항목을 보존한다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  const customerA = {
    id: 101,
    email: "cart-account-a@example.com",
    name: "장바구니 회원 A",
    phone: "01011111111",
    phoneVerified: true,
    localPasswordEnabled: true,
  };
  const customerB = {
    id: 202,
    email: "cart-account-b@example.com",
    name: "장바구니 회원 B",
    phone: "01022222222",
    phoneVerified: true,
    localPasswordEnabled: true,
  };
  let currentCustomer = customerA;
  let releaseMerge: (() => void) | undefined;
  const mergeRelease = new Promise<void>((resolve) => {
    releaseMerge = resolve;
  });
  const mergeRequests: MergeRequest[] = [];

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "cart-account-boundary-token",
    url: baseURL,
  }]);
  await page.addInitScript(([storageKey, items]) => {
    localStorage.setItem(storageKey, JSON.stringify(items));
  }, [
    GUEST_CART_STORAGE_KEY,
    [{ productId: 31, qty: 2, lineageId: "account-a-lineage" }],
  ] as const);
  await context.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(currentCustomer),
      });
      return;
    }
    if (pathname === "/api/v1/auth/login" && request.method() === "POST") {
      currentCustomer = customerB;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(customerB),
      });
      return;
    }
    if (pathname === "/api/v1/me/cart/merge") {
      mergeRequests.push(request.postDataJSON() as MergeRequest);
      await mergeRelease;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          cartVersion: EMPTY_CART_VERSION,
          items: [],
          totalAmount: 0,
        }),
      });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ count: 0 }),
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ name: "해피갤러리" }),
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([]),
    });
  });

  let otherPage: Page | undefined;
  try {
    await page.goto("/cart");
    await expect.poll(() => mergeRequests.length).toBe(1);

    otherPage = await context.newPage();
    await otherPage.goto("/login");
    await otherPage.getByLabel("이메일").fill(customerB.email);
    await otherPage.getByLabel("비밀번호").fill("password123!");
    await otherPage.getByRole("button", { name: "로그인", exact: true }).click();
    await expect(
      page.getByRole("link", { name: customerB.name, exact: true }),
    ).toBeVisible();

    if (!releaseMerge) {
      throw new Error("병합 응답 해제 함수가 준비되지 않았습니다.");
    }
    releaseMerge();

    await expect(
      page.getByRole("main").getByText(
        "다른 계정으로 로그인하기 전에 담은 상품이 남아 있습니다.",
      ),
    ).toBeVisible();
    await expect.poll(() => page.evaluate(
      ([cartKey, requestKey]) => {
        const cart = localStorage.getItem(cartKey);
        const mergeRequest = localStorage.getItem(requestKey);
        return {
          cart: cart ? JSON.parse(cart) : null,
          mergeRequest: mergeRequest ? JSON.parse(mergeRequest) : null,
        };
      },
      [GUEST_CART_STORAGE_KEY, MERGE_REQUEST_STORAGE_KEY] as const,
    )).toMatchObject({
      cart: [{ productId: 31, qty: 2, lineageId: "account-a-lineage" }],
      mergeRequest: {
        userId: customerA.id,
        items: [{ productId: 31, qty: 2, lineageId: "account-a-lineage" }],
      },
    });
    expect(mergeRequests).toHaveLength(1);
    expect(mergeRequests[0]?.expectedCustomerId).toBe(customerA.id);
  } finally {
    releaseMerge?.();
    await otherPage?.close();
  }
});
