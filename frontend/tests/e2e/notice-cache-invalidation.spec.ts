import { expect, test, type Page, type Route } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  homeSsrFixtures,
  replaceSsrUpstreamFixtures,
} from "./ssr-upstream-fixture";

const ADMIN_TOKEN_KEY = "hg_admin_token";
const workshop = {
  name: "해피갤러리",
  updatedAt: "2026-08-08T10:00:00",
  version: 1,
};

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

test("@admin 관리자 공지 저장 뒤 공개 공지 loader와 캐시가 최신 내용을 표시한다", async ({ page }) => {
  let publicTitle = "변경 전 공지";
  let publicVersion = 1;
  const updatePublicHomeFixture = () => replaceSsrUpstreamFixtures(...homeSsrFixtures({
    workshop,
    notices: [{
      id: 1,
      title: publicTitle,
      pinned: true,
      viewCount: 0,
      version: publicVersion,
      createdAt: "2026-08-08T10:00:00",
    }],
  }));

  await page.addInitScript(([key, token]) => {
    sessionStorage.setItem(key, token);
  }, [ADMIN_TOKEN_KEY, "notice-cache-admin-token"] as const);
  await updatePublicHomeFixture();

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        code: "UNAUTHORIZED",
        message: "로그인이 필요합니다.",
      }, 401);
      return;
    }
    if (pathname === "/api/v1/notices") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/admin/notices" && request.method() === "GET") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/admin/notices" && request.method() === "POST") {
      const body = request.postDataJSON() as { title: string };
      publicTitle = body.title;
      publicVersion += 1;
      await updatePublicHomeFixture();
      await fulfillJson(route, {
        id: 2,
        ...body,
        content: "공개 캐시 갱신 내용",
        pinned: false,
        viewCount: 0,
        version: 1,
        createdAt: "2026-08-08T11:00:00",
      }, 201);
      return;
    }
    if (pathname === "/api/v1/admin/qna/unanswered"
      || pathname === "/api/v1/admin/inquiries") {
      await fulfillJson(route, { content: [], nextCursor: null, hasMore: false });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/products"
      || pathname === "/api/v1/classes"
      || pathname === "/api/v1/events") {
      await route.fallback();
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/");
  await expect(page.getByText("변경 전 공지", { exact: true })).toBeVisible();

  await navigateInApp(page, "/admin?view=support");
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "새 공지 작성" }).click();
  await page.getByLabel("제목").fill("변경 후 공지");
  await page.getByLabel("내용").fill("공개 캐시 갱신 내용");
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect(page.getByText("공지사항이 등록되었습니다.")).toBeVisible();

  await navigateInApp(page, "/");
  await expect(page.getByText("변경 후 공지", { exact: true })).toBeVisible();
});
