import { expect, test, type Page, type Route } from "@playwright/test";

const ADMIN_TOKEN_KEY = "hg_admin_token";
const ADMIN_TOKEN = "picker-resilience-admin-token";
const EMPTY_CURSOR_PAGE = {
  content: [],
  nextCursor: null,
  hasMore: false,
};
const temporaryError = {
  code: "SERVICE_UNAVAILABLE",
  message: "잠시 후 다시 시도해 주세요.",
};
const adminClass = {
  id: 1,
  name: "회복 클래스",
  category: "RESIN",
  durationMin: 120,
  price: 50000,
  bufferMin: 30,
  passEligible: true,
  status: "ACTIVE",
  description: null,
  imageUrl: null,
  preparationInfo: null,
  targetAudience: null,
};

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

async function openAuthenticatedAdmin(page: Page, view: "today" | "bookings" | "classes") {
  await page.addInitScript(([key, token]) => {
    sessionStorage.setItem(key, token);
  }, [ADMIN_TOKEN_KEY, ADMIN_TOKEN] as const);
  await page.goto(`/admin?view=${view}`);
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
}

function adminPanel(page: Page, title: string) {
  return page.locator("section.admin-workspace-panel")
    .filter({ has: page.getByRole("heading", { name: title, exact: true }) });
}

test("@admin 클래스·슬롯 선택기는 조회 실패를 빈 상태로 단정하지 않고 다시 조회한다", async ({
  page,
}) => {
  let classesAvailable = false;
  let slotsAvailable = false;

  await page.route("**/api/v1/admin/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/admin/classes") {
      await fulfillJson(route, classesAvailable ? [adminClass] : temporaryError, classesAvailable ? 200 : 503);
      return;
    }
    if (pathname === "/api/v1/admin/slots") {
      await fulfillJson(route, slotsAvailable ? [] : temporaryError, slotsAvailable ? 200 : 503);
      return;
    }

    await fulfillJson(route, []);
  });

  await openAuthenticatedAdmin(page, "classes");
  const createPanel = adminPanel(page, "슬롯 생성");
  const bulkPanel = adminPanel(page, "슬롯 일괄 생성");
  const listPanel = adminPanel(page, "슬롯 목록");

  await expect(createPanel.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(createPanel.getByLabel("클래스")).toHaveCount(0);
  await expect(createPanel.getByText("등록된 클래스가 없습니다.")).toHaveCount(0);
  await expect(bulkPanel.getByLabel("클래스")).toHaveCount(0);
  await expect(listPanel.getByRole("combobox")).toHaveCount(0);
  await expect(listPanel.getByText("클래스를 선택하면 슬롯 목록이 표시됩니다.")).toHaveCount(0);

  classesAvailable = true;
  await createPanel.getByRole("button", { name: "다시 시도" }).click();
  await expect(createPanel.getByLabel("클래스")).toBeVisible();
  await expect(bulkPanel.getByLabel("클래스")).toBeVisible();
  await expect(listPanel.getByRole("combobox")).toBeVisible();

  await listPanel.getByRole("combobox").selectOption("1");
  await expect(listPanel.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(listPanel.getByText("해당 클래스에 슬롯이 없습니다.")).toHaveCount(0);

  slotsAvailable = true;
  await listPanel.getByRole("button", { name: "다시 시도" }).click();
  await expect(listPanel.getByText("해당 클래스에 슬롯이 없습니다.")).toBeVisible();
});

test("@admin 수기 예약 선택기는 클래스와 슬롯 조회 실패를 각각 복구한다", async ({ page }) => {
  let classesAvailable = false;
  let slotsAvailable = false;

  await page.route("**/api/v1/admin/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/admin/classes") {
      await fulfillJson(route, classesAvailable ? [adminClass] : temporaryError, classesAvailable ? 200 : 503);
      return;
    }
    if (pathname === "/api/v1/admin/slots") {
      await fulfillJson(route, slotsAvailable ? [] : temporaryError, slotsAvailable ? 200 : 503);
      return;
    }
    if (pathname === "/api/v1/admin/bookings/cancellation-tasks") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/admin/bookings") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/admin/passes/search") {
      await fulfillJson(route, {
        content: [],
        page: 0,
        size: 10,
        totalCount: 0,
        totalPages: 0,
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await openAuthenticatedAdmin(page, "bookings");
  await page.getByRole("button", { name: "수기 예약 등록" }).click();
  const modal = page.getByRole("dialog", { name: "전화·메신저·방문 예약 등록" });

  await expect(modal.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(modal.getByLabel("클래스")).toHaveCount(0);

  classesAvailable = true;
  await modal.getByRole("button", { name: "다시 시도" }).click();
  await modal.getByLabel("클래스").selectOption("1");

  await expect(modal.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(modal.getByLabel("예약 시간")).toBeDisabled();
  await expect(modal.getByLabel("예약 시간")).toContainText("슬롯을 다시 조회해 주세요");
  await expect(modal.getByText("예약 가능한 슬롯이 없습니다")).toHaveCount(0);

  slotsAvailable = true;
  await modal.getByRole("button", { name: "다시 시도" }).click();
  await expect(modal.getByLabel("예약 시간")).toContainText("예약 가능한 슬롯이 없습니다");
});

test("@admin 환불 조회 실패는 확인 필요 0건으로 단정하지 않고 다시 조회한다", async ({ page }) => {
  let refundsAvailable = false;

  await page.route("**/api/v1/admin/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/admin/refunds/failed") {
      await fulfillJson(
        route,
        refundsAvailable ? EMPTY_CURSOR_PAGE : temporaryError,
        refundsAvailable ? 200 : 503,
      );
      return;
    }
    if (pathname === "/api/v1/admin/order-claims" || pathname === "/api/v1/admin/orders") {
      await fulfillJson(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (
      pathname === "/api/v1/admin/payment-attempts/reconciliation-required"
      || pathname === "/api/v1/admin/notifications/failed"
      || pathname === "/api/v1/admin/bookings/cancellation-tasks"
      || pathname === "/api/v1/admin/bookings"
    ) {
      await fulfillJson(route, []);
      return;
    }

    await fulfillJson(route, []);
  });

  await openAuthenticatedAdmin(page, "today");
  const refundPanel = adminPanel(page, "환불 확인 필요");

  await expect(refundPanel.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(refundPanel.getByText("확인이 필요한 환불이 없습니다.")).toHaveCount(0);

  refundsAvailable = true;
  await refundPanel.getByRole("button", { name: "다시 시도" }).click();
  await expect(refundPanel.getByText("확인이 필요한 환불이 없습니다.")).toBeVisible();
});

test("예약 슬롯과 소셜 본인 확인 선택기는 조회 실패 후 수동 재시도로 복구한다", async ({ page }) => {
  let publicClassesAvailable = false;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(
        route,
        publicClassesAvailable ? [] : temporaryError,
        publicClassesAvailable ? 200 : 503,
      );
      return;
    }
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-07", documentPath: "/terms" },
        privacy: { version: "2026-07", documentPath: "/privacy" },
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new");
  const selectionCard = page.locator(".card").filter({ hasText: "2. 클래스 / 날짜 / 시간 선택" });
  await expect(selectionCard.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(selectionCard.getByLabel("클래스")).toHaveCount(0);

  publicClassesAvailable = true;
  await selectionCard.getByRole("button", { name: "다시 시도" }).click();
  await expect(selectionCard.getByLabel("클래스")).toBeVisible();
});

test("@identity 소셜 provider 조회 실패 중에는 동작하지 않는 step-up 버튼을 노출하지 않는다", async ({
  page,
}) => {
  let providersAvailable = false;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 301,
        email: "social-only@example.com",
        name: "소셜 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: false,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    if (pathname === "/api/v1/me/social-accounts") {
      await fulfillJson(
        route,
        providersAvailable ? { linkedProviders: ["GOOGLE"] } : temporaryError,
        providersAvailable ? 200 : 503,
      );
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/my");
  const phoneSection = page.getByRole("heading", { name: "휴대폰 번호" })
    .locator("xpath=ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' border-top ')][1]");
  await phoneSection.getByRole("button", { name: "변경" }).click();
  const modal = page.getByRole("dialog", { name: "휴대폰 번호 변경" });

  await expect(modal.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(modal.getByRole("button", { name: "연결된 소셜 계정으로 본인 확인" }))
    .toHaveCount(0);

  providersAvailable = true;
  await modal.getByRole("button", { name: "다시 시도" }).click();
  await expect(modal.getByRole("button", { name: "연결된 소셜 계정으로 본인 확인" }))
    .toBeEnabled();
});
