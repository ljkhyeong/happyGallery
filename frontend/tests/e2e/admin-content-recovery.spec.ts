import { expect, test, type Page, type Route } from "@playwright/test";
import type { SmartStoreNoticePageResponse } from "../../src/generated/api/adminCatalog";
import type { SmartStoreAccountingReportResponse } from "../../src/generated/api/adminOperations";
import type { SmartStoreInquiryPageResponse, SmartStoreInquiryAnswerTemplateResponse } from "../../src/generated/api/productQna";
import { skipExternalFonts } from "./external-fonts";

const ADMIN_TOKEN_KEY = "hg_admin_token";
const ADMIN_TOKEN = "admin-content-recovery-token";
const EMPTY_CURSOR_PAGE = {
  content: [],
  nextCursor: null,
  hasMore: false,
};

test.beforeEach(async ({ page }) => {
  await skipExternalFonts({ page });
  await page.route("**/api/v1/me", (route) => json(route, { code: "UNAUTHORIZED" }, 401));
  await page.route("**/api/v1/workshop", (route) => json(route, { name: "해피갤러리", version: 1 }));
});

async function openAuthenticatedAdmin(page: Page, view: "support" | "settings" | "orders") {
  await page.addInitScript(([key, token]) => {
    sessionStorage.setItem(key, token);
  }, [ADMIN_TOKEN_KEY, ADMIN_TOKEN] as const);
  await page.goto(`/admin?view=${view}`);
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
}

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

test("P8-CONTENT-1 @admin 공지 수정은 기존 본문을 불러오고 충돌 뒤 초안을 보존한다", async ({ page }) => {
  let detailReads = 0;
  let updateAttempts = 0;
  let retriedBody: Record<string, unknown> | undefined;

  await page.route("**/api/v1/admin/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const { pathname } = url;

    if (pathname === "/api/v1/admin/notices" && request.method() === "GET") {
      await json(route, [{
        id: 1,
        title: "운영 안내",
        pinned: true,
        viewCount: 3,
        version: 1,
        createdAt: "2026-07-28T10:00:00",
      }]);
      return;
    }

    if (pathname === "/api/v1/admin/notices/1" && request.method() === "GET") {
      detailReads += 1;
      await json(route, {
        id: 1,
        title: detailReads === 1 ? "운영 안내" : "다른 관리자가 수정한 제목",
        content: detailReads === 1 ? "기존 공지 본문" : "다른 관리자가 수정한 본문",
        pinned: detailReads === 1,
        viewCount: 3,
        version: detailReads === 1 ? 1 : 2,
        createdAt: "2026-07-28T10:00:00",
      });
      return;
    }

    if (pathname === "/api/v1/admin/notices/1" && request.method() === "PUT") {
      updateAttempts += 1;
      if (updateAttempts === 1) {
        await json(route, {
          code: "CONFLICT",
          message: "다른 관리자가 먼저 수정했습니다.",
        }, 409);
        return;
      }

      retriedBody = request.postDataJSON() as Record<string, unknown>;
      await json(route, {
        id: 1,
        ...retriedBody,
        viewCount: 3,
        version: 3,
        createdAt: "2026-07-28T10:00:00",
      });
      return;
    }

    if (pathname === "/api/v1/admin/products") {
      await json(route, []);
      return;
    }
    if (pathname === "/api/v1/admin/qna/unanswered") {
      await json(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/admin/inquiries") {
      await json(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/admin/smartstore-inquiries/page") {
      await json(route, {
        content: [], totalPages: 0, totalCount: 0, page: 0, size: 50,
      } satisfies SmartStoreInquiryPageResponse);
      return;
    }
    if (pathname === "/api/v1/admin/smartstore-inquiries/template") {
      await json(route, { content: "", questionType: "", subject: "" } satisfies SmartStoreInquiryAnswerTemplateResponse);
      return;
    }
    if (pathname === "/api/v1/admin/smartstore-notices") {
      await json(route, {
        notices: [], page: 1, size: 100, totalElements: 0, totalPages: 0,
      } satisfies SmartStoreNoticePageResponse);
      return;
    }

    throw new Error(`정의하지 않은 관리자 요청: ${request.method()} ${pathname}`);
  });

  await openAuthenticatedAdmin(page, "support");
  await page.getByRole("button", { name: "수정", exact: true }).click();

  const content = page.getByPlaceholder("내용");
  const editForm = content.locator("xpath=ancestor::form");
  await expect(content).toHaveValue("기존 공지 본문");
  await content.fill("충돌 전에 작성한 내 초안");
  await editForm.getByRole("button", { name: "수정", exact: true }).click();

  await expect(page.getByText("작성 중인 초안은 그대로 보존했습니다.")).toBeVisible();
  await expect(content).toHaveValue("충돌 전에 작성한 내 초안");

  await page.getByRole("button", { name: "내 초안 유지" }).click();
  await editForm.getByRole("button", { name: "수정", exact: true }).click();

  await expect.poll(() => updateAttempts).toBe(2);
  expect(retriedBody).toMatchObject({
    expectedVersion: 2,
    content: "충돌 전에 작성한 내 초안",
  });
});

test("P8-CONTENT-2 @admin 설정 충돌과 인증·로그아웃 실패를 안전하게 복구한다", async ({ page }) => {
  let workshopReads = 0;
  let workshopUpdates = 0;
  let retriedWorkshop: Record<string, unknown> | undefined;

  const workshopProfile = (version: number, name: string) => ({
    name,
    phone: "01096355608",
    postalCode: "27360",
    addressLine1: "충북 충주시 계명대로 161",
    addressLine2: "1층",
    businessHours: null,
    mapUrl: null,
    parkingInfo: null,
    businessRegistrationNumber: "303-11-87052",
    representativeName: "대표자",
    email: "owner@example.com",
    mailOrderRegistrationNumber: "신고번호",
    introduction: null,
    kakaoTalkId: "ssim1972",
    naverTalkUrl: null,
    naverBlogUrl: "https://blog.naver.com/ssim1972",
    instagramUrl: null,
    smartStoreUrl: null,
    version,
    updatedAt: "2026-07-28T10:00:00",
  });

  await page.route("**/api/v1/admin/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/admin/workshop" && request.method() === "GET") {
      workshopReads += 1;
      await json(
        route,
        workshopReads === 1
          ? workshopProfile(1, "해피갤러리")
          : workshopProfile(2, "서버에서 수정한 공방명"),
      );
      return;
    }

    if (pathname === "/api/v1/admin/workshop" && request.method() === "PUT") {
      workshopUpdates += 1;
      if (workshopUpdates === 1) {
        await json(route, {
          code: "CONFLICT",
          message: "다른 관리자가 먼저 수정했습니다.",
        }, 409);
        return;
      }

      retriedWorkshop = request.postDataJSON() as Record<string, unknown>;
      await json(route, {
        ...workshopProfile(3, String(retriedWorkshop.name)),
        ...retriedWorkshop,
        version: 3,
      });
      return;
    }

    if (pathname === "/api/v1/admin/auth/mfa" && request.method() === "GET") {
      await json(route, {
        enabled: false,
        enrollmentPending: false,
        recoveryCodesRemaining: 0,
      });
      return;
    }

    if (pathname === "/api/v1/admin/auth/password" && request.method() === "PATCH") {
      await json(route, {
        code: "INVALID_CREDENTIALS",
        message: "현재 비밀번호가 올바르지 않습니다.",
      }, 401);
      return;
    }

    if (pathname === "/api/v1/admin/auth/logout" && request.method() === "POST") {
      await json(route, {
        code: "SERVICE_UNAVAILABLE",
        message: "세션 저장소를 확인할 수 없습니다.",
      }, 503);
      return;
    }

    throw new Error(`정의하지 않은 관리자 요청: ${request.method()} ${pathname}`);
  });

  await openAuthenticatedAdmin(page, "settings");
  const workshopName = page.getByLabel("공방명");
  await expect(workshopName).toHaveValue("해피갤러리");
  await workshopName.fill("내가 작성한 공방명");
  await page.getByRole("button", { name: "공방 정보 저장" }).click();

  await expect(page.getByText("작성 중인 초안은 그대로 보존했습니다.")).toBeVisible();
  await expect(workshopName).toHaveValue("내가 작성한 공방명");
  await page.getByRole("button", { name: "내 초안 유지" }).click();
  await page.getByRole("button", { name: "공방 정보 저장" }).click();

  await expect.poll(() => workshopUpdates).toBe(2);
  expect(retriedWorkshop).toMatchObject({
    expectedVersion: 2,
    name: "내가 작성한 공방명",
  });

  await page.getByLabel("현재 비밀번호").first().fill("wrong-password");
  await page.getByLabel("새 비밀번호", { exact: true }).fill("new-password-1234");
  await page.getByLabel("새 비밀번호 확인").fill("new-password-1234");
  await page.getByRole("button", { name: "비밀번호 변경" }).click();

  await expect(page.getByText("로그인 정보가 올바르지 않습니다.")).toBeVisible();
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
  await expect.poll(() => page.evaluate((key) => sessionStorage.getItem(key), ADMIN_TOKEN_KEY))
    .toBe(ADMIN_TOKEN);

  await page.getByRole("button", { name: "로그아웃", exact: true }).click();
  await expect(page.getByRole("heading", { name: "관리자 로그인" })).toBeVisible();
  await expect.poll(() => page.evaluate((key) => sessionStorage.getItem(key), ADMIN_TOKEN_KEY))
    .toBeNull();
  await expect(page.getByText("이 브라우저에서는 로그아웃했지만 다른 기기의 로그인 상태는 유지될 수 있습니다. 다른 기기에서도 로그아웃해 주세요.")).toBeVisible();
});

test("P8-CONTENT-3 @admin 주문 조회 실패는 빈 목록으로 단정하지 않고 다시 조회한다", async ({
  page,
}) => {
  let orderAttempts = 0;

  await page.route("**/api/v1/admin/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/admin/orders") {
      orderAttempts += 1;
      await json(
        route,
        orderAttempts <= 2
          ? { code: "SERVICE_UNAVAILABLE", message: "잠시 후 다시 시도해 주세요." }
          : EMPTY_CURSOR_PAGE,
        orderAttempts <= 2 ? 503 : 200,
      );
      return;
    }
    if (pathname === "/api/v1/admin/order-claims") {
      await json(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/admin/smartstore-orders") {
      await json(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/admin/smartstore-settlements/issues") {
      await json(route, []);
      return;
    }
    if (pathname === "/api/v1/admin/smartstore-settlements/accounting") {
      const params = new URL(route.request().url()).searchParams;
      await json(route, {
        from: params.get("from")!, to: params.get("to")!, vatAvailableThrough: "2026-07-31",
        dailySettlements: [], commissionDetails: [], dailyVat: [],
      } satisfies SmartStoreAccountingReportResponse);
      return;
    }

    throw new Error(`정의하지 않은 관리자 요청: ${route.request().method()} ${pathname}`);
  });

  await openAuthenticatedAdmin(page, "orders");
  const orderPanel = page.locator("section").filter({ hasText: "주문 목록" }).last();
  await expect(orderPanel.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(orderPanel.getByText("해당 조건의 주문이 없습니다.")).toHaveCount(0);

  await orderPanel.getByRole("button", { name: "다시 시도" }).click();
  await expect(orderPanel.getByText("해당 조건의 주문이 없습니다.")).toBeVisible();
});
