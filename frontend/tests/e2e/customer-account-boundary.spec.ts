import { expect, test, type Page, type Route } from "@playwright/test";

interface Customer {
  id: number;
  email: string;
  name: string;
  phone: string;
  phoneVerified: boolean;
  localPasswordEnabled: boolean;
}

const EMPTY_CART_VERSION = "0".repeat(64);

const customerA: Customer = {
  id: 101,
  email: "customer-a@example.com",
  name: "회원 A",
  phone: "01011111111",
  phoneVerified: true,
  localPasswordEnabled: true,
};

const customerB: Customer = {
  id: 202,
  email: "customer-b@example.com",
  name: "회원 B",
  phone: "01022222222",
  phoneVerified: true,
  localPasswordEnabled: true,
};

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function flushBrowserTasks(page: Page) {
  await page.evaluate(() => new Promise<void>((resolve) => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => resolve());
    });
  }));
}

async function loginInPage(page: Page, customer: Customer) {
  await page.getByLabel("이메일").fill(customer.email);
  await page.getByLabel("비밀번호").fill("password123!");
  const loginResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/v1/auth/login"
    && response.request().method() === "POST");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await loginResponse;
  await expect(page.getByRole("link", { name: customer.name, exact: true }))
    .toBeVisible({ timeout: 15_000 });
}

test("@identity 최초 비회원 확인이 늦게 끝나도 공개 화면 입력을 유지한다", async ({
  page,
}) => {
  let releaseMe: (() => void) | undefined;
  const meRelease = new Promise<void>((resolve) => {
    releaseMe = resolve;
  });

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await meRelease;
      await fulfillJson(route, {
        code: "UNAUTHORIZED",
        message: "로그인이 필요합니다.",
      }, 401);
      return;
    }
    if (pathname === "/api/v1/products/42") {
      await fulfillJson(route, {
        id: 42,
        name: "비회원 입력 유지 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 30000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/92") {
      await fulfillJson(route, {
        id: 92,
        productId: 42,
        title: "공개 질문",
        content: "비회원도 볼 수 있는 공개 내용",
        authorName: "공개 작성자",
        secret: false,
        replyContent: null,
        repliedAt: null,
        createdAt: "2026-07-28T12:00:00",
      });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page") {
      await fulfillJson(route, {
        content: [{
          id: 92,
          title: "공개 질문",
          authorName: "공개 작성자",
          secret: false,
          hasReply: false,
          createdAt: "2026-07-28T12:00:00",
        }],
        hasMore: false,
        nextCursor: null,
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  await page.goto("/products/42");
  await page.getByRole("spinbutton", { name: "수량" }).fill("3");
  await page.getByRole("button", { name: "내용 보기" }).click();
  await expect(page.getByText("비회원도 볼 수 있는 공개 내용")).toBeVisible();

  if (!releaseMe) {
    throw new Error("지연된 회원 확인 응답 해제 함수가 준비되지 않았습니다.");
  }
  const meResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/v1/me"
      && response.status() === 401);
  releaseMe();
  await meResponse;
  await expect(page.getByRole("link", { name: "로그인", exact: true }).first())
    .toBeVisible();

  await expect(page.getByRole("spinbutton", { name: "수량" })).toHaveValue("3");
  await expect(page.getByText("비회원도 볼 수 있는 공개 내용")).toBeVisible();
});

test("@identity 이전 회원의 지연된 결제 준비 응답은 새 계정에서 부작용을 만들지 않는다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  let currentCustomer: Customer | null = customerA;
  let releasePrepare: (() => void) | undefined;
  let prepareStarted = false;
  const prepareRelease = new Promise<void>((resolve) => {
    releasePrepare = resolve;
  });

  await page.context().addCookies([{
    name: "XSRF-TOKEN",
    value: "customer-payment-boundary-token",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      accountBoundaryTossRequests: unknown[];
      TossPayments: () => {
        payment: () => {
          requestPayment: (request: unknown) => Promise<void>;
        };
      };
    };
    browserGlobal.accountBoundaryTossRequests = [];
    browserGlobal.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          browserGlobal.accountBoundaryTossRequests.push(request);
        },
      }),
    });
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      if (currentCustomer) {
        await fulfillJson(route, currentCustomer);
      } else {
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      }
      return;
    }
    if (pathname === "/api/v1/auth/logout") {
      currentCustomer = null;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      currentCustomer = customerB;
      await fulfillJson(route, customerB);
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      prepareStarted = true;
      await prepareRelease;
      await fulfillJson(route, {
        orderId: "payment-A",
        amount: 30000,
        context: "ORDER",
        statusToken: "status-A",
      });
      return;
    }
    if (pathname === "/api/v1/products/42") {
      await fulfillJson(route, {
        id: 42,
        name: "지연 결제 테스트 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 30000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page"
      || pathname === "/api/v1/me/products/42/qna/page") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
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
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-07",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  await page.goto("/products/42");
  await page.getByRole("button", { name: "매장 픽업" }).click();
  await page.getByRole("button", { name: "바로 구매하기" }).click();
  await expect.poll(() => prepareStarted).toBe(true);

  await page.getByRole("button", { name: "로그아웃" }).click();
  await page.getByRole("button", { name: "로그인 후 구매하기" }).click();
  await page.getByLabel("이메일").fill(customerB.email);
  await page.getByLabel("비밀번호").fill("password123!");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(page.getByText(customerB.name).first()).toBeVisible();

  if (!releasePrepare) {
    throw new Error("지연된 결제 준비 응답 해제 함수가 준비되지 않았습니다.");
  }
  const prepareResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/v1/payments/prepare");
  releasePrepare();
  await prepareResponse;
  await flushBrowserTasks(page);

  await expect.poll(() => page.evaluate(() => {
    const browserGlobal = globalThis as unknown as {
      accountBoundaryTossRequests: unknown[];
    };
    return browserGlobal.accountBoundaryTossRequests.length;
  })).toBe(0);
  await expect.poll(() => page.evaluate(() => ({
    statusToken: sessionStorage.getItem("hg_payment_status_token:payment-A"),
    returnHint: sessionStorage.getItem("hg_payment_return_hint"),
  }))).toEqual({ statusToken: null, returnHint: null });
});

test("@identity 비회원의 지연된 결제 준비 응답은 로그인한 계정에서 부작용을 만들지 않는다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  let currentCustomer: Customer | null = null;
  let releasePrepare: (() => void) | undefined;
  let prepareStarted = false;
  let guestPreparePayload: Record<string, unknown> | null = null;
  const prepareRelease = new Promise<void>((resolve) => {
    releasePrepare = resolve;
  });

  await page.context().addCookies([{
    name: "XSRF-TOKEN",
    value: "guest-payment-boundary-token",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      accountBoundaryTossRequests: unknown[];
      TossPayments: () => {
        payment: () => {
          requestPayment: (request: unknown) => Promise<void>;
        };
      };
    };
    browserGlobal.accountBoundaryTossRequests = [];
    browserGlobal.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          browserGlobal.accountBoundaryTossRequests.push(request);
        },
      }),
    });
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      if (currentCustomer) {
        await fulfillJson(route, currentCustomer);
      } else {
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      }
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      currentCustomer = customerB;
      await fulfillJson(route, customerB);
      return;
    }
    if (pathname === "/api/v1/bookings/phone-verifications") {
      await fulfillJson(route, {
        phone: "01033333333",
        verificationId: 301,
      });
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      const body = request.postDataJSON() as {
        payload: Record<string, unknown>;
      };
      guestPreparePayload = body.payload;
      prepareStarted = true;
      await prepareRelease;
      await fulfillJson(route, {
        orderId: "guest-payment",
        amount: 30000,
        context: "ORDER",
        statusToken: "guest-status",
      });
      return;
    }
    if (pathname === "/api/v1/products") {
      await fulfillJson(route, [{
        id: 42,
        name: "비회원 지연 결제 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 30000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      }]);
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
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-07", documentPath: "/terms/2026-07" },
        privacy: { version: "2026-07", documentPath: "/privacy/2026-07" },
      });
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  await page.goto("/orders/new?productId=42&qty=1");
  await page.getByLabel("휴대폰 번호").fill("01033333333");
  await page.getByRole("button", { name: "인증코드 발송" }).click();
  await page.getByLabel("인증코드").fill("123456");
  await page.getByRole("button", { name: "확인", exact: true }).click();
  await page.getByLabel("주문자 이름").fill("비회원 주문자");
  await expect(page.getByLabel("상품")).toContainText("비회원 지연 결제 작품");
  await page.getByRole("button", { name: "매장 픽업" }).click();
  await page.locator("#guest-order-policy-consent").check();
  await expect(page.getByRole("button", { name: "결제 진행하기" }))
    .toBeEnabled();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect.poll(() => prepareStarted).toBe(true);
  expect(guestPreparePayload).toMatchObject({
    phone: "01033333333",
    verificationCode: "123456",
  });
  expect(guestPreparePayload).not.toHaveProperty("userId");

  await page.getByRole("link", { name: "로그인 후 주문하기" }).click();
  await page.getByLabel("이메일").fill(customerB.email);
  await page.getByLabel("비밀번호").fill("password123!");
  await page.getByRole("button", { name: "로그인", exact: true }).click();
  await expect(page.getByText(customerB.name).first()).toBeVisible();

  if (!releasePrepare) {
    throw new Error("지연된 비회원 결제 준비 응답 해제 함수가 준비되지 않았습니다.");
  }
  const prepareResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/v1/payments/prepare");
  releasePrepare();
  await prepareResponse;
  await flushBrowserTasks(page);

  await expect.poll(() => page.evaluate(() => {
    const browserGlobal = globalThis as unknown as {
      accountBoundaryTossRequests: unknown[];
    };
    return browserGlobal.accountBoundaryTossRequests.length;
  })).toBe(0);
  await expect.poll(() => page.evaluate(() => ({
    statusToken: sessionStorage.getItem("hg_payment_status_token:guest-payment"),
    returnHint: sessionStorage.getItem("hg_payment_return_hint"),
  }))).toEqual({ statusToken: null, returnHint: null });
});

test("@identity 이전 세대의 me 요청 중에도 새 계정 refresh는 별도 요청을 사용한다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  let meRequestCount = 0;
  let failNextMe = false;
  let releaseFirstMe: (() => void) | undefined;
  const firstMeRelease = new Promise<void>((resolve) => {
    releaseFirstMe = resolve;
  });

  await page.context().addCookies([{
    name: "XSRF-TOKEN",
    value: "customer-refresh-boundary-token",
    url: baseURL,
  }]);
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      meRequestCount += 1;
      if (meRequestCount === 1) {
        await firstMeRelease;
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      } else if (failNextMe) {
        failNextMe = false;
        await fulfillJson(route, {
          code: "SERVICE_UNAVAILABLE",
          message: "일시적인 조회 실패",
        }, 503);
      } else {
        await fulfillJson(route, customerB);
      }
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      await fulfillJson(route, customerB);
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
    if (pathname === "/api/v1/me/social-accounts") {
      await fulfillJson(route, { linkedProviders: [] });
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  await page.goto(
    `/login?redirect=${encodeURIComponent("/auth/callback?linked=google")}`,
  );
  await expect.poll(() => meRequestCount).toBe(1);
  const firstMeResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/v1/me"
      && response.status() === 401);
  await page.getByLabel("이메일").fill(customerB.email);
  await page.getByLabel("비밀번호").fill("password123!");

  try {
    await page.getByRole("button", { name: "로그인", exact: true }).click();
    await expect.poll(() => meRequestCount).toBe(2);
    await expect(page).toHaveURL(`${baseURL}/my`);
  } finally {
    releaseFirstMe?.();
  }

  await firstMeResponse;
  await expect(page.getByText(customerB.name).first()).toBeVisible();
  await flushBrowserTasks(page);
  await expect(page.getByText(customerB.name).first()).toBeVisible();

  const boundaryBeforeFailure = await page.evaluate(() =>
    localStorage.getItem("hg_customer_session_boundary"));
  failNextMe = true;
  const failedMeResponse = page.waitForResponse((response) =>
    new URL(response.url()).pathname === "/api/v1/me"
      && response.status() === 503);
  await page.evaluate(() => {
    window.dispatchEvent(new Event("pageshow"));
  });
  await failedMeResponse;
  await expect.poll(() => meRequestCount).toBe(3);
  await flushBrowserTasks(page);

  await expect(page.getByText(customerB.name).first()).toBeVisible();
  expect(await page.evaluate(() =>
    localStorage.getItem("hg_customer_session_boundary")))
    .toBe(boundaryBeforeFailure);
});

test("@identity 다른 탭의 계정 전환과 로그아웃이 이전 탭의 폼과 지연 결제를 정리한다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  const context = page.context();
  let currentCustomer: Customer | null = customerA;
  let releasePrepare: (() => void) | undefined;
  let prepareStarted = false;
  let preparedUserId: number | null = null;
  const prepareRelease = new Promise<void>((resolve) => {
    releasePrepare = resolve;
  });

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "cross-tab-customer-boundary-token",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      accountBoundaryTossRequests: unknown[];
      TossPayments: () => {
        payment: () => {
          requestPayment: (request: unknown) => Promise<void>;
        };
      };
    };
    browserGlobal.accountBoundaryTossRequests = [];
    browserGlobal.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          browserGlobal.accountBoundaryTossRequests.push(request);
        },
      }),
    });
  });

  await context.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      if (currentCustomer) {
        await fulfillJson(route, currentCustomer);
      } else {
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      }
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      currentCustomer = customerB;
      await fulfillJson(route, customerB);
      return;
    }
    if (pathname === "/api/v1/auth/logout") {
      currentCustomer = null;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      const body = request.postDataJSON() as {
        payload: { userId?: number };
      };
      preparedUserId = body.payload.userId ?? null;
      prepareStarted = true;
      await prepareRelease;
      await fulfillJson(route, {
        orderId: "cross-tab-payment-A",
        amount: 30000,
        context: "ORDER",
        statusToken: "cross-tab-status-A",
      });
      return;
    }
    if (pathname === "/api/v1/products/42") {
      await fulfillJson(route, {
        id: 42,
        name: "탭 경계 테스트 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 30000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      });
      return;
    }
    if (
      pathname === "/api/v1/products/42/qna/page"
      || pathname === "/api/v1/me/products/42/qna/page"
    ) {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
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
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-07",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  let otherPage: Page | undefined;
  try {
    await page.goto("/products/42");
    await expect(page.getByText(customerA.name).first()).toBeVisible();
    await page.getByRole("button", { name: "택배 배송" }).click();
    await page.getByLabel("기본 주소").fill("A 탭에서 입력한 배송지");
    await page.getByRole("button", { name: "매장 픽업" }).click();
    await page.getByRole("button", { name: "바로 구매하기" }).click();
    await expect.poll(() => prepareStarted).toBe(true);
    expect(preparedUserId).toBe(customerA.id);

    otherPage = await context.newPage();
    await otherPage.goto(
      `/login?redirect=${encodeURIComponent("/products/42")}`,
    );
    await loginInPage(otherPage, customerB);

    await expect(page.getByRole("link", { name: customerB.name, exact: true })).toBeVisible();
    await page.getByRole("button", { name: "택배 배송" }).click();
    await expect(page.getByLabel("받는 분")).toHaveValue(customerB.name);
    await expect(page.getByLabel("연락처")).toHaveValue(customerB.phone);
    await expect(page.getByLabel("기본 주소")).toHaveValue("");

    if (!releasePrepare) {
      throw new Error("지연된 탭 간 결제 준비 응답 해제 함수가 준비되지 않았습니다.");
    }
    const prepareResponse = page.waitForResponse((response) =>
      new URL(response.url()).pathname === "/api/v1/payments/prepare");
    releasePrepare();
    await prepareResponse;
    await flushBrowserTasks(page);

    await expect.poll(() => page.evaluate(() => {
      const browserGlobal = globalThis as unknown as {
        accountBoundaryTossRequests: unknown[];
      };
      return browserGlobal.accountBoundaryTossRequests.length;
    })).toBe(0);
    await expect.poll(() => page.evaluate(() => ({
      statusToken: sessionStorage.getItem(
        "hg_payment_status_token:cross-tab-payment-A",
      ),
      returnHint: sessionStorage.getItem("hg_payment_return_hint"),
    }))).toEqual({ statusToken: null, returnHint: null });

    await otherPage.getByRole("button", { name: "로그아웃" }).click();
    await expect(
      page.getByRole("link", { name: "로그인", exact: true }).first(),
    ).toBeVisible();
    await expect(page.getByText(customerB.name)).toHaveCount(0);
    await expect(page.getByRole("button", { name: "로그인 후 구매하기" }))
      .toBeVisible();
  } finally {
    releasePrepare?.();
    await otherPage?.close();
  }
});

test("@identity 다른 탭에서 계정이 바뀌면 이전 비회원 복구 토큰을 조회 화면에 이어 쓰지 않는다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  const context = page.context();
  let currentCustomer: Customer = customerA;

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "guest-recovery-boundary-token",
    url: baseURL,
  }]);
  await context.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      await fulfillJson(route, currentCustomer);
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      currentCustomer = customerB;
      await fulfillJson(route, customerB);
      return;
    }
    if (pathname === "/api/v1/bookings/phone-verifications") {
      await fulfillJson(route, {
        phone: "01011111111",
        verificationId: 701,
      });
      return;
    }
    if (pathname === "/api/v1/guest-records/recovery") {
      await fulfillJson(route, {
        accessToken: "guest-token-A",
        expiresAt: "2099-07-29T12:00:00",
        orders: [{
          orderId: 701,
          status: "PAID_APPROVAL_PENDING",
          totalAmount: 30000,
          createdAt: "2026-07-29T12:00:00",
        }],
        bookings: [],
      });
      return;
    }
    if (pathname === "/api/v1/guest-records/recovery/orders") {
      await fulfillJson(route, {
        content: [{
          orderId: 701,
          status: "PAID_APPROVAL_PENDING",
          totalAmount: 30000,
          createdAt: "2026-07-29T12:00:00",
        }],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }
    if (pathname === "/api/v1/guest-records/recovery/bookings") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }
    if (pathname === "/api/v1/orders/701") {
      await fulfillJson(route, {
        code: "NOT_FOUND",
        message: "테스트용 상세 응답 없음",
      }, 404);
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-29T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  let otherPage: Page | undefined;
  try {
    await page.goto("/guest");
    await expect(page.getByText(customerA.name).first()).toBeVisible();
    const recoverySection = page
      .getByRole("heading", { name: "주문·예약 조회 정보 복구" })
      .locator("..");
    await recoverySection.getByLabel("휴대폰 번호").fill("01011111111");
    await recoverySection.getByRole("button", { name: "인증코드 발송" }).click();
    await recoverySection.getByLabel("인증코드").fill("123456");
    await recoverySection.getByRole("button", { name: "조회 정보 복구" }).click();

    await expect(page.getByText("주문 #701")).toBeVisible();
    await page.getByText("주문 #701").click();
    await expect(page.getByLabel("조회 코드")).toHaveValue("guest-token-A");

    otherPage = await context.newPage();
    await otherPage.goto("/login");
    await loginInPage(otherPage, customerB);

    await expect(page.getByRole("link", { name: customerB.name, exact: true })).toBeVisible();
    await expect(page.getByLabel("조회 코드")).toHaveValue("");
    await expect(page.getByText("주문 #701")).toHaveCount(0);
    await expect.poll(() => page.evaluate(() =>
      sessionStorage.getItem("guest_record_recovery")))
      .toContain("guest-token-A");
  } finally {
    await otherPage?.close();
  }
});

test("@identity 계정 전환 뒤 이전 결제 확정 결과와 정리가 새 계정에 적용되지 않는다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  const context = page.context();
  let currentCustomer: Customer | null = customerA;
  let releaseConfirm: (() => void) | undefined;
  let confirmStarted = false;
  const confirmRelease = new Promise<void>((resolve) => {
    releaseConfirm = resolve;
  });

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "payment-result-boundary-token",
    url: baseURL,
  }]);
  await context.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      if (currentCustomer) {
        await fulfillJson(route, currentCustomer);
      } else {
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      }
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      currentCustomer = customerB;
      await fulfillJson(route, customerB);
      return;
    }
    if (pathname === "/api/v1/payments/confirm") {
      confirmStarted = true;
      await confirmRelease;
      await fulfillJson(route, {
        context: "ORDER",
        domainId: 901,
        accessToken: null,
        accessRecoveryRequired: false,
      });
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  let otherPage: Page | undefined;
  try {
    await page.goto("/login");
    await expect(page.getByText(customerA.name).first()).toBeVisible();
    await page.evaluate(() => {
      const boundary = JSON.parse(
        localStorage.getItem("hg_customer_session_boundary") ?? "null",
      ) as { epoch: string; customerId: number };
      const owner = {
        boundaryEpoch: boundary.epoch,
        boundaryCustomerId: boundary.customerId,
      };
      sessionStorage.setItem(
        "hg_payment_status_token:shared-payment",
        JSON.stringify({ owner, value: "shared-status-token" }),
      );
      sessionStorage.setItem(
        "hg_payment_return_hint",
        JSON.stringify({
          owner,
          value: { customerName: "같은 이름", customerPhone: "01011111111" },
        }),
      );
    });

    await page.goto(
      "/payments/success"
      + "?paymentKey=shared-payment-key"
      + "&orderId=shared-payment"
      + "&amount=30000",
    );
    await expect.poll(() => confirmStarted).toBe(true);

    otherPage = await context.newPage();
    await otherPage.goto("/login");
    await loginInPage(otherPage, customerB);
    await expect(page.getByText("회원 계정이 변경되었습니다")).toBeVisible();

    await page.evaluate(() => {
      const boundary = JSON.parse(
        localStorage.getItem("hg_customer_session_boundary") ?? "null",
      ) as { epoch: string; customerId: number };
      const owner = {
        boundaryEpoch: boundary.epoch,
        boundaryCustomerId: boundary.customerId,
      };
      const request = {
        paymentKey: "shared-payment-key",
        orderId: "shared-payment",
        amount: 30000,
      };
      sessionStorage.setItem(
        "hg_payment_confirm_request",
        JSON.stringify({ owner, value: request }),
      );
      sessionStorage.setItem(
        "hg_payment_status_token:shared-payment",
        JSON.stringify({ owner, value: "shared-status-token" }),
      );
      sessionStorage.setItem(
        "hg_payment_return_hint",
        JSON.stringify({
          owner,
          value: { customerName: "같은 이름", customerPhone: "01011111111" },
        }),
      );
    });

    if (!releaseConfirm) {
      throw new Error("지연된 결제 확정 응답 해제 함수가 준비되지 않았습니다.");
    }
    releaseConfirm();
    await flushBrowserTasks(page);

    await expect(page.getByRole("heading", { name: "결제 완료" })).toHaveCount(0);
    await expect.poll(() => page.evaluate(() => {
      const readEnvelope = (key: string) => {
        const raw = sessionStorage.getItem(key);
        return raw ? JSON.parse(raw) : null;
      };
      return {
        confirm: readEnvelope("hg_payment_confirm_request"),
        status: readEnvelope("hg_payment_status_token:shared-payment"),
        hint: readEnvelope("hg_payment_return_hint"),
      };
    })).toMatchObject({
      confirm: {
        owner: { boundaryCustomerId: customerB.id },
        value: { orderId: "shared-payment" },
      },
      status: {
        owner: { boundaryCustomerId: customerB.id },
        value: "shared-status-token",
      },
      hint: {
        owner: { boundaryCustomerId: customerB.id },
        value: { customerName: "같은 이름" },
      },
    });
  } finally {
    releaseConfirm?.();
    await otherPage?.close();
  }
});

test("@identity 소셜 재인증 뒤 실제 연결 callback에도 같은 회원 소유권을 이어간다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  await page.context().addCookies([{
    name: "XSRF-TOKEN",
    value: "customer-social-boundary-token",
    url: baseURL,
  }]);
  await page.addInitScript((customerId) => {
    if (sessionStorage.getItem("account_boundary_social_initialized")) return;
    sessionStorage.setItem("account_boundary_social_initialized", "true");
    sessionStorage.setItem("customer_continuation_owner", String(customerId));
    sessionStorage.setItem("social_reauthentication", "google");
    sessionStorage.setItem("social_account_link_target", "naver");
  }, customerA.id);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, customerA);
      return;
    }
    if (
      pathname === "/api/v1/me/social-accounts/naver/authorization"
      && request.method() === "POST"
    ) {
      await fulfillJson(route, {
        authorizationUrl: `${baseURL}/oauth-next`,
      });
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }

    await fulfillJson(route, {});
  });

  await page.goto("/auth/callback?reauthenticated=true");
  await expect(page).toHaveURL(`${baseURL}/oauth-next`);
  await expect.poll(() => page.evaluate(() => ({
    owner: sessionStorage.getItem("customer_continuation_owner"),
    provider: sessionStorage.getItem("social_account_link"),
  }))).toEqual({
    owner: String(customerA.id),
    provider: "naver",
  });
});

test("@identity 계정이 바뀌면 비밀 Q&A와 주문 배송 정보가 이전되지 않는다", async ({
  baseURL,
  page,
}) => {
  if (!baseURL) {
    throw new Error("Playwright baseURL이 필요합니다.");
  }

  let currentCustomer: Customer | null = customerA;
  await page.context().addCookies([{
    name: "XSRF-TOKEN",
    value: "customer-boundary-test-token",
    url: baseURL,
  }]);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      if (currentCustomer) {
        await fulfillJson(route, currentCustomer);
      } else {
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      }
      return;
    }
    if (pathname === "/api/v1/auth/logout") {
      currentCustomer = null;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/auth/login") {
      const email = (request.postDataJSON() as { email: string }).email;
      currentCustomer = email === customerA.email ? customerA : customerB;
      await fulfillJson(route, currentCustomer);
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
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        addressLine1: null,
        addressLine2: null,
        businessHours: null,
        businessRegistrationNumber: null,
        email: null,
        instagramUrl: null,
        introduction: null,
        kakaoTalkId: null,
        mailOrderRegistrationNumber: null,
        mapUrl: null,
        naverBlogUrl: null,
        naverTalkUrl: null,
        parkingInfo: null,
        phone: null,
        postalCode: null,
        representativeName: null,
        smartStoreUrl: null,
        updatedAt: "2026-07-28T12:00:00",
        version: 1,
      });
      return;
    }
    if (pathname === "/api/v1/products/42") {
      await fulfillJson(route, {
        id: 42,
        name: "계정 경계 테스트 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 30000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page") {
      await fulfillJson(route, {
        content: [{
          id: 91,
          title: "비밀 질문",
          authorName: "회원 A",
          secret: true,
          hasReply: true,
          createdAt: "2026-07-28T12:00:00",
        }],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }
    if (pathname === "/api/v1/me/products/42/qna/91") {
      await fulfillJson(route, {
        id: 91,
        productId: 42,
        title: "비밀 질문",
        content: "A 계정만 볼 수 있는 내용",
        authorName: "회원 A",
        secret: true,
        replyContent: "A 계정 전용 답변",
        repliedAt: "2026-07-28T13:00:00",
        createdAt: "2026-07-28T12:00:00",
      });
      return;
    }
    if (pathname === "/api/v1/me/products/42/qna/page") {
      await fulfillJson(route, {
        content: currentCustomer?.id === customerA.id
          ? [{
              id: 91,
              title: "비밀 질문",
              secret: true,
              hasReply: true,
              createdAt: "2026-07-28T12:00:00",
            }]
          : [],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }
    if (pathname === "/api/v1/products") {
      await fulfillJson(route, [{
        id: 42,
        name: "계정 경계 테스트 작품",
        description: null,
        category: "테스트",
        type: "READY_STOCK",
        price: 30000,
        imageUrl: null,
        available: true,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      }]);
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
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-07", documentPath: "/terms/2026-07" },
        privacy: { version: "2026-07", documentPath: "/privacy/2026-07" },
      });
      return;
    }

    await fulfillJson(route, {});
  });

  await page.goto("/products/42");
  await page.getByRole("button", { name: "작성자 전용 내용 보기" }).click();
  await expect(page.getByText("A 계정만 볼 수 있는 내용")).toBeVisible();

  await page.getByRole("button", { name: "택배 배송" }).click();
  await page.getByLabel("기본 주소").fill("A 계정 배송지");
  await page.getByRole("button", { name: "로그아웃" }).click();

  await expect(page.getByText("A 계정만 볼 수 있는 내용")).toHaveCount(0);
  await expect(page.getByLabel("기본 주소")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "작성자 전용 내용 보기" })).toHaveCount(0);

  await page.goto(`/login?redirect=${encodeURIComponent("/products/42")}`);
  await page.getByLabel("이메일").fill(customerB.email);
  await page.getByLabel("비밀번호").fill("password123!");
  await page.getByRole("button", { name: "로그인", exact: true }).click();

  await expect(page.getByText("작성자만 볼 수 있는 비밀글입니다.")).toBeVisible();
  await page.getByRole("button", { name: "택배 배송" }).click();
  await expect(page.getByLabel("받는 분")).toHaveValue(customerB.name);
  await expect(page.getByLabel("연락처")).toHaveValue(customerB.phone);
  await expect(page.getByLabel("기본 주소")).toHaveValue("");

  await page.goto("/orders/new");
  await page.getByLabel("주문자 이름").fill("B 주문자 정보");
  await page.getByLabel("상품").selectOption("42");
  await page.getByRole("button", { name: "추가" }).click();
  await page.getByRole("button", { name: "택배 배송" }).click();
  await page.getByLabel("기본 주소").fill("B 계정 배송지");
  await page.getByRole("button", { name: "로그아웃" }).click();

  await expect(page.getByText("B 주문자 정보")).toHaveCount(0);
  await expect(page.getByLabel("기본 주소")).toHaveCount(0);
  await expect(page.getByRole("button", { name: "비회원 다중 상품 주문 계속" })).toBeVisible();

  await page.goto(`/login?redirect=${encodeURIComponent("/orders/new")}`);
  await page.getByLabel("이메일").fill(customerA.email);
  await page.getByLabel("비밀번호").fill("password123!");
  await page.getByRole("button", { name: "로그인", exact: true }).click();

  await expect(page.getByLabel("주문자 이름")).toHaveValue(customerA.name);
  await expect(page.getByText("계정 경계 테스트 작품", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "택배 배송" }).click();
  await expect(page.getByLabel("받는 분")).toHaveValue(customerA.name);
  await expect(page.getByLabel("연락처")).toHaveValue(customerA.phone);
  await expect(page.getByLabel("기본 주소")).toHaveValue("");
});
