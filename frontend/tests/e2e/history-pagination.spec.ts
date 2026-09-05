import { expect, test, type BrowserContext, type Route } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function installCsrfCookie(context: BrowserContext, baseURL: string | undefined) {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "history-pagination-csrf",
    url: baseURL,
  }]);
}

const member = {
  id: 501,
  email: "history@example.com",
  name: "이력 회원",
  phone: "01012345678",
  phoneVerified: true,
  localPasswordEnabled: true,
};
const EMPTY_CART_VERSION = "0".repeat(64);

async function fulfillCommon(route: Route) {
  const { pathname } = new URL(route.request().url());
  if (pathname === "/api/v1/me") {
    await fulfillJson(route, member);
    return true;
  }
  if (pathname === "/api/v1/me/cart") {
    await fulfillJson(route, {
      cartVersion: EMPTY_CART_VERSION,
      items: [],
      totalAmount: 0,
    });
    return true;
  }
  if (pathname === "/api/v1/me/notifications/unread-count") {
    await fulfillJson(route, { count: 0 });
    return true;
  }
  if (pathname === "/api/v1/workshop") {
    await fulfillJson(route, { name: "해피갤러리" });
    return true;
  }
  return false;
}

test("회원 주문은 첫 페이지에서 더 보기를 눌러 다음 커서 이력을 이어 본다", async ({ page }) => {
  const requestedCursors: Array<string | null> = [];

  await page.route("**/api/v1/**", async (route) => {
    if (await fulfillCommon(route)) return;

    const url = new URL(route.request().url());
    if (url.pathname === "/api/v1/me/orders/page") {
      const cursor = url.searchParams.get("cursor");
      requestedCursors.push(cursor);
      await fulfillJson(route, cursor === "orders-next"
        ? {
            content: [{
              orderId: 102,
              status: "DELIVERED",
              totalAmount: 22000,
              paidAt: "2026-08-02T12:00:00",
              createdAt: "2026-08-02T11:59:00",
            }],
            hasMore: false,
            nextCursor: null,
          }
        : {
            content: [{
              orderId: 101,
              status: "PAID_APPROVAL_PENDING",
              totalAmount: 11000,
              paidAt: "2026-08-03T12:00:00",
              createdAt: "2026-08-03T11:59:00",
            }],
            hasMore: true,
            nextCursor: "orders-next",
          });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my/orders");
  await expect(page.getByText("주문 #101")).toBeVisible();
  await expect(page.getByText("주문 #102")).toHaveCount(0);
  await expect(page.getByText("검색 결과 1건 표시 중 · 더 보기로 계속 조회")).toBeVisible();

  await page.getByRole("button", { name: "주문 더 보기" }).click();

  await expect(page.getByText("주문 #101")).toBeVisible();
  await expect(page.getByText("주문 #102")).toBeVisible();
  await expect(page.getByText("검색 결과 2건 표시 중")).toBeVisible();
  expect(requestedCursors[0]).toBeNull();
  expect(requestedCursors.at(-1)).toBe("orders-next");
});

test("공개 Q&A 더 보기는 같은 크기의 내 Q&A 페이지도 함께 전진시켜 비밀글 소유권을 판정한다", async ({ page }) => {
  const publicCursors: Array<string | null> = [];
  const ownerCursors: Array<string | null> = [];
  const product = {
    id: 42,
    name: "페이지 Q&A 작품",
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

  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));

  await page.route("**/api/v1/**", async (route) => {
    if (await fulfillCommon(route)) return;

    const url = new URL(route.request().url());
    if (url.pathname === "/api/v1/products/42") {
      await route.fallback();
      return;
    }
    if (url.pathname === "/api/v1/products/42/reviews") {
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
    if (url.pathname === "/api/v1/products/42/qna/page") {
      const cursor = url.searchParams.get("cursor");
      publicCursors.push(cursor);
      await fulfillJson(route, cursor === "public-next"
        ? {
            content: [{
              id: 91,
              title: "두 번째 비밀 질문",
              authorName: "이력 회원",
              secret: true,
              hasReply: false,
              createdAt: "2026-08-01T10:00:00",
            }],
            hasMore: false,
            nextCursor: null,
          }
        : {
            content: [{
              id: 92,
              title: "첫 번째 공개 질문",
              authorName: "다른 회원",
              secret: false,
              hasReply: false,
              createdAt: "2026-08-02T10:00:00",
            }],
            hasMore: true,
            nextCursor: "public-next",
          });
      return;
    }
    if (url.pathname === "/api/v1/me/products/42/qna/page") {
      const cursor = url.searchParams.get("cursor");
      ownerCursors.push(cursor);
      await fulfillJson(route, cursor === "owner-next"
        ? {
            content: [{
              id: 91,
              title: "두 번째 비밀 질문",
              secret: true,
              hasReply: false,
              createdAt: "2026-08-01T10:00:00",
            }],
            hasMore: false,
            nextCursor: null,
          }
        : {
            content: [],
            hasMore: true,
            nextCursor: "owner-next",
          });
      return;
    }
    if (url.pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-08",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/products/42");
  await expect(page.getByText("첫 번째 공개 질문")).toBeVisible();
  await page.getByRole("button", { name: "Q&A 더 보기" }).click();

  await expect(page.getByText("두 번째 비밀 질문")).toBeVisible();
  await expect(page.getByRole("button", { name: "작성자 전용 내용 보기" })).toBeVisible();
  expect(publicCursors[0]).toBeNull();
  expect(publicCursors.at(-1)).toBe("public-next");
  expect(ownerCursors[0]).toBeNull();
  expect(ownerCursors.at(-1)).toBe("owner-next");
});

test("비회원 복구 화면은 POST 배열 대신 토큰 기반 GET 페이지를 기준으로 전체 이력을 이어 본다", async ({
  baseURL,
  context,
  page,
}) => {
  await installCsrfCookie(context, baseURL);
  const orderCursors: Array<string | null> = [];
  const observedTokens: string[] = [];

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (url.pathname === "/api/v1/me/cart") {
      await fulfillJson(route, {
        cartVersion: EMPTY_CART_VERSION,
        items: [],
        totalAmount: 0,
      });
      return;
    }
    if (url.pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (url.pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (url.pathname === "/api/v1/bookings/phone-verifications") {
      await fulfillJson(route, { phone: "01012345678", verificationId: 77 });
      return;
    }
    if (url.pathname === "/api/v1/guest-records/recovery" && request.method() === "POST") {
      await fulfillJson(route, {
        accessToken: "canonical-recovery-token",
        expiresAt: "2099-08-08T12:00:00",
        orders: [{
          orderId: 999,
          status: "DELIVERED",
          totalAmount: 99900,
          createdAt: "2026-08-08T12:00:00",
        }],
        bookings: [],
      });
      return;
    }
    if (url.pathname === "/api/v1/guest-records/recovery/orders") {
      const cursor = url.searchParams.get("cursor");
      orderCursors.push(cursor);
      observedTokens.push(request.headers()["x-access-token"] ?? "");
      await fulfillJson(route, cursor === "guest-orders-next"
        ? {
            content: [{
              orderId: 702,
              status: "DELIVERED",
              totalAmount: 22000,
              createdAt: "2026-08-01T12:00:00",
            }],
            hasMore: false,
            nextCursor: null,
          }
        : {
            content: [{
              orderId: 701,
              status: "PAID_APPROVAL_PENDING",
              totalAmount: 11000,
              createdAt: "2026-08-02T12:00:00",
            }],
            hasMore: true,
            nextCursor: "guest-orders-next",
          });
      return;
    }
    if (url.pathname === "/api/v1/guest-records/recovery/bookings") {
      observedTokens.push(request.headers()["x-access-token"] ?? "");
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/guest");
  const recoveryCard = page.locator(".card")
    .filter({ hasText: "주문·예약 조회 정보 복구" })
    .first();
  await recoveryCard.getByLabel("휴대폰 번호").fill("01012345678");
  await recoveryCard.getByRole("button", { name: "인증코드 발송" }).click();
  await recoveryCard.getByLabel("인증코드").fill("123456");
  await recoveryCard.getByRole("button", { name: "조회 정보 복구" }).click();

  await expect(recoveryCard.getByText("주문 #701")).toBeVisible();
  await expect(recoveryCard.getByText("주문 #999")).toHaveCount(0);
  await recoveryCard.getByRole("button", { name: "비회원 주문 더 보기" }).click();
  await expect(recoveryCard.getByText("주문 #702")).toBeVisible();
  await expect(recoveryCard.getByText("비회원 주문 · 불러온 2건")).toBeVisible();
  expect(orderCursors).toEqual([null, "guest-orders-next"]);
  expect(observedTokens).toEqual([
    "canonical-recovery-token",
    "canonical-recovery-token",
    "canonical-recovery-token",
  ]);
});

test("관리자 상품별 Q&A는 page API 커서를 이전·다음 이력 UI에 연결한다", async ({ page }) => {
  const requestedCursors: Array<string | null> = [];
  await page.addInitScript(() => {
    sessionStorage.setItem("hg_admin_token", "admin-qna-page-token");
  });

  await page.route("**/api/v1/admin/**", async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname === "/api/v1/admin/smartstore-notices") {
      await fulfillJson(route, { notices: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
      return;
    }
    if (url.pathname === "/api/v1/admin/smartstore-inquiries/page"
        || url.pathname === "/api/v1/admin/smartstore-inquiries/customers/page") {
      await fulfillJson(route, { content: [], page: 0, size: 50, totalCount: 0, totalPages: 0 });
      return;
    }
    if (url.pathname === "/api/v1/admin/products") {
      await fulfillJson(route, [{ id: 42, name: "관리 Q&A 작품" }]);
      return;
    }
    if (url.pathname === "/api/v1/admin/qna/unanswered") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }
    if (url.pathname === "/api/v1/admin/qna/page") {
      const cursor = url.searchParams.get("cursor");
      requestedCursors.push(cursor);
      await fulfillJson(route, cursor === "admin-qna-next"
        ? {
            content: [{
              id: 802,
              productId: 42,
              userId: 502,
              authorName: "두 번째 회원",
              title: "두 번째 관리자 Q&A",
              content: "두 번째 내용",
              secret: false,
              replyContent: "답변 완료",
              repliedAt: "2026-08-02T13:00:00",
              createdAt: "2026-08-02T12:00:00",
            }],
            hasMore: false,
            nextCursor: null,
          }
        : {
            content: [{
              id: 801,
              productId: 42,
              userId: 501,
              authorName: "첫 번째 회원",
              title: "첫 번째 관리자 Q&A",
              content: "첫 번째 내용",
              secret: false,
              replyContent: "답변 완료",
              repliedAt: "2026-08-03T13:00:00",
              createdAt: "2026-08-03T12:00:00",
            }],
            hasMore: true,
            nextCursor: "admin-qna-next",
          });
      return;
    }
    if (url.pathname === "/api/v1/admin/inquiries") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }
    if (url.pathname === "/api/v1/admin/notices") {
      await fulfillJson(route, []);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/admin?view=support");
  const qnaPanel = page.locator("section.admin-workspace-panel")
    .filter({ hasText: "상품 문의 관리" });
  await qnaPanel.getByRole("button", { name: "상품별" }).click();
  await qnaPanel.getByLabel("상품", { exact: true }).selectOption("42");
  await expect(qnaPanel.getByText("첫 번째 관리자 Q&A")).toBeVisible();

  await qnaPanel.getByRole("button", { name: "다음" }).click();
  await expect(qnaPanel.getByText("두 번째 관리자 Q&A")).toBeVisible();
  await qnaPanel.getByRole("button", { name: "이전" }).click();
  await expect(qnaPanel.getByText("첫 번째 관리자 Q&A")).toBeVisible();

  expect(requestedCursors[0]).toBeNull();
  expect(requestedCursors).toContain("admin-qna-next");
});

test("예약 생성의 8회권 후보는 첫 페이지에 없더라도 다음 페이지까지 순회한다", async ({ page }) => {
  const requestedCursors: Array<string | null> = [];
  const pageErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.stack ?? error.message));
  page.on("console", (message) => {
    if (message.type() === "error") pageErrors.push(message.text());
  });

  await page.route("**/api/v1/**", async (route) => {
    if (await fulfillCommon(route)) return;

    const url = new URL(route.request().url());
    if (url.pathname === "/api/v1/classes") {
      await fulfillJson(route, [{
        bufferMin: 30,
        category: "LEATHER",
        description: null,
        durationMin: 120,
        id: 42,
        imageUrl: null,
        name: "페이지 8회권 클래스",
        passEligible: true,
        preparationInfo: null,
        price: 50000,
        status: "ACTIVE",
        targetAudience: null,
      }]);
      return;
    }
    if (url.pathname === "/api/v1/slots/upcoming") {
      await fulfillJson(route, [{
        bookedCount: 0,
        capacity: 8,
        classId: 42,
        endAt: "2099-01-02T12:00:00",
        id: 4201,
        remainingCapacity: 8,
        startAt: "2099-01-02T10:00:00",
      }]);
      return;
    }
    if (url.pathname === "/api/v1/me/passes/page") {
      const cursor = url.searchParams.get("cursor");
      requestedCursors.push(cursor);
      const passId = cursor === "passes-next" ? 9 : 10;
      await fulfillJson(route, {
        content: [{
          expiresAt: "2099-12-31T00:00:00",
          passId,
          planCode: "REGULAR_CRAFT_8",
          planName: "정규 공예 8회권",
          purchasedAt: cursor === "passes-next"
            ? "2098-11-01T00:00:00"
            : "2098-12-01T00:00:00",
          refund: null,
          remainingCredits: 7,
          totalCredits: 8,
          totalPrice: 240000,
        }],
        hasMore: cursor !== "passes-next",
        nextCursor: cursor === "passes-next" ? null : "passes-next",
      });
      return;
    }
    if (url.pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-08", documentPath: "/terms/2026-08" },
        privacy: { version: "2026-08", documentPath: "/privacy/2026-08" },
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new?passId=9&classId=42");
  await page.waitForTimeout(500);
  expect(pageErrors).toEqual([]);
  await page.getByRole("button", { name: /2099\. 01\. 02\. 오전 10:00/ }).click();
  await expect(page.getByLabel("사용할 8회권")).toHaveValue("9");
  expect(requestedCursors[0]).toBeNull();
  expect(requestedCursors.at(-1)).toBe("passes-next");
});

for (const scenario of [
  { path: "orders", label: "주문 번호·상품명 검색", more: "주문 더 보기", sort: "AMOUNT_DESC", status: "DELIVERED",
    item: { orderId: 9090, status: "DELIVERED", totalAmount: 12000, createdAt: "2026-08-01T12:00:00", paidAt: null } },
  { path: "bookings", label: "예약 검색", more: "예약 더 보기", sort: "LATEST", status: "CANCELED",
    item: { bookingId: 9090, status: "CANCELED", className: "이력 검색 클래스", startAt: "2099-01-01T10:00:00", participantCount: 1, depositAmount: 10000 } },
  { path: "passes", label: "8회권 번호 검색", more: "8회권 더 보기", sort: "CREDITS_DESC", status: "EXPIRED",
    item: { passId: 9090, planName: "정규 공예 8회권", planCode: "REGULAR_CRAFT_8", purchasedAt: "2026-01-01T12:00:00", expiresAt: "2026-02-01T12:00:00", remainingCredits: 2, totalCredits: 8, totalPrice: 240000, refund: null, receiptUrl: null } },
]) {
  test(`회원 ${scenario.path} 검색은 조건을 서버로 보내고 빈 결과에서도 조건을 바꿀 수 있다`, async ({ page }) => {
    const requests: URL[] = [];
    await page.route("**/api/v1/**", async (route) => {
      if (await fulfillCommon(route)) return;
      const url = new URL(route.request().url());
      if (url.pathname === `/api/v1/me/${scenario.path}/page`) {
        requests.push(url);
        const filtered = url.searchParams.has("keyword") || url.searchParams.has("status");
        const next = url.searchParams.has("cursor");
        const hasMore = !filtered && !next;
        await fulfillJson(route, {
          content: next || url.searchParams.get("keyword") === "없는내역" ? [] : [scenario.item],
          hasMore, nextCursor: hasMore ? "previous-filter-cursor" : null,
        });
        return;
      }
      await fulfillJson(route, []);
    });
    await page.goto(`/my/${scenario.path}`);
    await page.getByRole("button", { name: scenario.more, exact: true }).click();
    await expect.poll(() => requests.some((url) => url.searchParams.has("cursor"))).toBe(true);
    await page.getByLabel(scenario.label, { exact: true }).fill("9090");
    await expect.poll(() => requests.at(-1)?.searchParams.get("keyword")).toBe("9090");
    expect(requests.at(-1)?.searchParams.get("cursor")).toBeNull();
    await page.getByLabel("정렬", { exact: true }).selectOption(scenario.sort);
    await page.getByLabel("상태", { exact: true }).selectOption(scenario.status);
    await expect.poll(() => requests.at(-1)?.searchParams.get("status")).toBe(scenario.status);
    expect(requests.at(-1)?.searchParams.get("sort")).toBe(scenario.sort);
    expect(requests.at(-1)?.searchParams.get("cursor")).toBeNull();
    await page.getByLabel(scenario.label, { exact: true }).fill("없는내역");
    await expect(page.getByText(/검색 조건에 맞는 .* 내역이 없습니다/)).toBeVisible();
    await expect(page.getByLabel("상태", { exact: true })).toBeVisible();
    await page.getByRole("button", { name: "초기화", exact: true }).click();
    await expect(page.getByLabel(scenario.label, { exact: true })).toHaveValue("");
  });
}
