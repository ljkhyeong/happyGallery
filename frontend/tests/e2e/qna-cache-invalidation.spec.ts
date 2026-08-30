import { expect, test, type Page, type Route } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

const ADMIN_TOKEN_KEY = "hg_admin_token";
const EMPTY_CART_VERSION = "0".repeat(64);

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

async function navigateInApp(page: Page, path: string) {
  await page.evaluate((nextPath) => {
    window.history.pushState({}, "", nextPath);
    window.dispatchEvent(new PopStateEvent("popstate"));
  }, path);
}

function qnaCard(page: Page, title: string) {
  return page.getByText(title, { exact: true }).locator(
    "xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' card ')][1]",
  );
}

test("@admin 관리자 Q&A 답변 뒤 공개·회원 목록과 상세 캐시를 모두 다시 조회한다", async ({ page }) => {
  const pageErrors: string[] = [];
  const consoleErrors: string[] = [];
  page.on("pageerror", (error) => pageErrors.push(error.stack ?? error.message));
  page.on("console", (message) => {
    if (message.type() === "error") consoleErrors.push(message.text());
  });
  const replies = new Map<number, string>();
  let publicListReads = 0;
  let memberListReads = 0;
  let publicDetailReads = 0;
  let memberDetailReads = 0;
  const product = {
    id: 42,
    name: "Q&A 캐시 작품",
    description: null,
    category: "테스트",
    type: "READY_STOCK",
    price: 12000,
    imageUrl: null,
    available: true,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
    optionGroups: [],
    variants: [],
  };

  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));

  await page.addInitScript(([key, token]) => {
    sessionStorage.setItem(key, token);
  }, [ADMIN_TOKEN_KEY, "qna-cache-admin-token"] as const);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 501,
        email: "qna-cache@example.com",
        name: "Q&A 캐시 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, {
        cartVersion: EMPTY_CART_VERSION,
        items: [],
        totalAmount: 0,
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
    if (pathname === "/api/v1/products/42") {
      await route.fallback();
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
    if (pathname === "/api/v1/orders/policy") {
      await fulfillJson(route, {
        shippingFee: 3000,
        madeToOrderConsentVersion: "2026-08",
        madeToOrderConsentText: "주문제작 동의",
      });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/page") {
      publicListReads += 1;
      await fulfillJson(route, {
        content: [
          {
            id: 1,
            title: "공개 질문",
            authorName: "Q&A 캐시 회원",
            secret: false,
            hasReply: replies.has(1),
            createdAt: "2026-08-08T09:00:00",
          },
          {
            id: 2,
            title: "비밀 질문",
            authorName: "Q&A 캐시 회원",
            secret: true,
            hasReply: replies.has(2),
            createdAt: "2026-08-08T08:00:00",
          },
        ],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }
    if (pathname === "/api/v1/me/products/42/qna/page") {
      memberListReads += 1;
      await fulfillJson(route, {
        content: [
          {
            id: 1,
            title: "공개 질문",
            secret: false,
            hasReply: replies.has(1),
            createdAt: "2026-08-08T09:00:00",
          },
          {
            id: 2,
            title: "비밀 질문",
            secret: true,
            hasReply: replies.has(2),
            createdAt: "2026-08-08T08:00:00",
          },
        ],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }
    if (pathname === "/api/v1/products/42/qna/1") {
      publicDetailReads += 1;
      await fulfillJson(route, {
        id: 1,
        productId: 42,
        title: "공개 질문",
        content: "공개 질문 본문",
        authorName: "Q&A 캐시 회원",
        secret: false,
        replyContent: replies.get(1) ?? null,
        repliedAt: replies.has(1) ? "2026-08-08T11:00:00" : null,
        createdAt: "2026-08-08T09:00:00",
      });
      return;
    }
    if (pathname === "/api/v1/me/products/42/qna/2") {
      memberDetailReads += 1;
      await fulfillJson(route, {
        id: 2,
        productId: 42,
        title: "비밀 질문",
        content: "비밀 질문 본문",
        authorName: "Q&A 캐시 회원",
        secret: true,
        replyContent: replies.get(2) ?? null,
        repliedAt: replies.has(2) ? "2026-08-08T11:01:00" : null,
        createdAt: "2026-08-08T08:00:00",
      });
      return;
    }
    if (pathname === "/api/v1/admin/products") {
      await fulfillJson(route, [{ id: 42, name: "Q&A 캐시 작품" }]);
      return;
    }
    if (pathname === "/api/v1/admin/qna/unanswered") {
      const content = [
        {
          id: 1,
          productId: 42,
          userId: 501,
          authorName: "Q&A 캐시 회원",
          title: "공개 질문",
          content: "공개 질문 본문",
          secret: false,
          replyContent: null,
          repliedAt: null,
          createdAt: "2026-08-08T09:00:00",
        },
        {
          id: 2,
          productId: 42,
          userId: 501,
          authorName: "Q&A 캐시 회원",
          title: "비밀 질문",
          content: "비밀 질문 본문",
          secret: true,
          replyContent: null,
          repliedAt: null,
          createdAt: "2026-08-08T08:00:00",
        },
      ].filter((item) => !replies.has(item.id));
      await fulfillJson(route, { content, hasMore: false, nextCursor: null });
      return;
    }
    const replyMatch = pathname.match(/^\/api\/v1\/admin\/qna\/(\d+)\/reply$/);
    if (replyMatch && request.method() === "POST") {
      const id = Number(replyMatch[1]);
      const { replyContent } = request.postDataJSON() as { replyContent: string };
      replies.set(id, replyContent);
      await fulfillJson(route, {
        id,
        productId: 42,
        userId: 501,
        authorName: "Q&A 캐시 회원",
        title: id === 1 ? "공개 질문" : "비밀 질문",
        content: id === 1 ? "공개 질문 본문" : "비밀 질문 본문",
        secret: id === 2,
        replyContent,
        repliedAt: "2026-08-08T11:00:00",
        createdAt: id === 1 ? "2026-08-08T09:00:00" : "2026-08-08T08:00:00",
      });
      return;
    }
    if (pathname === "/api/v1/admin/inquiries") {
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }
    if (pathname === "/api/v1/admin/notices"
      || pathname === "/api/v1/products"
      || pathname === "/api/v1/classes") {
      await fulfillJson(route, []);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/products/42");
  await page.waitForTimeout(500);
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
  await expect(page.getByText("공개 질문", { exact: true })).toBeVisible();
  const publicCard = qnaCard(page, "공개 질문");
  await publicCard.getByRole("button", { name: "내용 보기" }).click();
  await expect(publicCard.getByText("공개 질문 본문", { exact: true })).toBeVisible();
  const secretCard = qnaCard(page, "비밀 질문");
  await secretCard.getByRole("button", { name: "작성자 전용 내용 보기" }).click();
  await expect(secretCard.getByText("비밀 질문 본문", { exact: true })).toBeVisible();

  await navigateInApp(page, "/admin?view=support");
  const qnaPanel = page.locator("section.admin-workspace-panel")
    .filter({ hasText: "상품 문의 관리" });
  const publicAdminCard = qnaPanel.getByText("공개 질문", { exact: true }).locator(
    "xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' card ')][1]",
  );
  await publicAdminCard.getByRole("textbox", { name: "Q&A 답변" }).fill("공개 답변");
  await publicAdminCard.getByRole("button", { name: "답변", exact: true }).click();
  await expect(qnaPanel.getByText("공개 질문", { exact: true })).toHaveCount(0);

  const secretAdminCard = qnaPanel.getByText("비밀 질문", { exact: true }).locator(
    "xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' card ')][1]",
  );
  await secretAdminCard.getByRole("textbox", { name: "Q&A 답변" }).fill("비밀 답변");
  await secretAdminCard.getByRole("button", { name: "답변", exact: true }).click();
  await expect(qnaPanel.getByText("비밀 질문", { exact: true })).toHaveCount(0);

  await navigateInApp(page, "/products/42");
  const refreshedPublicCard = qnaCard(page, "공개 질문");
  const refreshedSecretCard = qnaCard(page, "비밀 질문");
  await expect(refreshedPublicCard.getByText("답변완료", { exact: true })).toBeVisible();
  await expect(refreshedSecretCard.getByText("답변완료", { exact: true })).toBeVisible();

  await refreshedPublicCard.getByRole("button", { name: "내용 보기" }).click();
  await expect(refreshedPublicCard.getByText("공개 답변", { exact: true })).toBeVisible();
  await refreshedSecretCard.getByRole("button", { name: "작성자 전용 내용 보기" }).click();
  await expect(refreshedSecretCard.getByText("비밀 답변", { exact: true })).toBeVisible();

  expect(publicListReads).toBeGreaterThanOrEqual(2);
  expect(memberListReads).toBeGreaterThanOrEqual(2);
  expect(publicDetailReads).toBeGreaterThanOrEqual(2);
  expect(memberDetailReads).toBeGreaterThanOrEqual(2);
});
