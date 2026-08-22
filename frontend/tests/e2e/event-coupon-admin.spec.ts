import { expect, test, type Page, type Route } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

const ADMIN_TOKEN_KEY = "hg_admin_token";
const ADMIN_TOKEN = "event-coupon-admin-token";

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

async function openAuthenticatedAdmin(page: Page, view: "events" | "coupons") {
  await page.addInitScript(([key, token]) => {
    sessionStorage.setItem(key, token);
  }, [ADMIN_TOKEN_KEY, ADMIN_TOKEN] as const);
  await page.goto(`/admin?view=${view}`);
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
}

test("공개 이벤트 목록은 종료된 항목을 숨기고 상세와 연관 상품을 연결한다", async ({ page }) => {
  const upcomingEvent = {
    id: 21,
    title: "겨울 공방 오픈데이",
    summary: "공방에서 새 작품을 만나보세요.",
    content: "작가의 신작과 제작 이야기를 소개합니다.",
    imageUrl: null,
    startAt: "2099-12-01T10:00:00",
    endAt: "2099-12-31T18:00:00",
    published: true,
    featured: true,
    relatedProductIds: [501],
    version: 1,
  };
  const endedEvent = {
    ...upcomingEvent,
    id: 22,
    title: "종료된 이벤트",
    startAt: "2000-01-01T10:00:00",
    endAt: "2000-01-02T18:00:00",
    featured: false,
    relatedProductIds: [],
  };

  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/events", [upcomingEvent, endedEvent]),
    ssrApiFixture("/events/21", upcomingEvent),
  );

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/events") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/events/21") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        version: 1,
        updatedAt: "2026-08-08T10:00:00",
      });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/events");
  await expect(page.getByRole("heading", { name: "겨울 공방 오픈데이" })).toBeVisible();
  await expect(page.getByText("종료된 이벤트")).toHaveCount(0);
  await expect(page.getByText("예정", { exact: true })).toBeVisible();
  await expect(page.getByText("추천", { exact: true })).toBeVisible();

  await page.getByRole("link", { name: /자세히 보기/ }).click();
  await expect(page).toHaveURL(/\/events\/21$/);
  await expect(page.getByText("작가의 신작과 제작 이야기를 소개합니다.")).toBeVisible();
  await expect(page.getByRole("link", { name: "작품 #501" })).toHaveAttribute(
    "href",
    "/products/501",
  );
});

test("열어 둔 이벤트가 종료되면 상세를 404로 전환하고 반복 조회를 멈춘다", async ({ page }) => {
  const now = new Date("2026-08-08T01:00:00Z");
  await page.clock.install({ time: now });
  let eventExpired = false;
  let expiredRequests = 0;
  const expiringEvent = {
    id: 23,
    title: "곧 종료되는 이벤트",
    summary: "종료 경계를 확인합니다.",
    content: "종료 뒤에는 공개하지 않습니다.",
    imageUrl: null,
    startAt: new Date(now.getTime() - 60_000).toISOString(),
    endAt: new Date(now.getTime() + 4_000).toISOString(),
    published: true,
    featured: false,
    relatedProductIds: [],
    version: 1,
  };

  await replaceSsrUpstreamFixtures(ssrApiFixture("/events/23", expiringEvent));

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/events/23") {
      if (!eventExpired) {
        await fulfillJson(route, expiringEvent);
      } else {
        expiredRequests += 1;
        await fulfillJson(route, { code: "NOT_FOUND", message: "이벤트를 찾을 수 없습니다." }, 404);
      }
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/events/23");
  await expect(page.getByRole("heading", { name: expiringEvent.title })).toBeVisible();
  eventExpired = true;
  await page.clock.fastForward(4_250);
  await expect(page.getByRole("heading", { name: "404" })).toBeVisible({ timeout: 8_000 });
  await expect(page.getByText(expiringEvent.title)).toHaveCount(0);
  expect(expiredRequests).toBe(1);

  await page.clock.fastForward(61_000);
  await new Promise((resolve) => setTimeout(resolve, 250));
  expect(expiredRequests).toBe(1);
});

test("이벤트 상세에서 이탈하면 진행 중인 상세 요청을 브라우저에서 취소한다", async ({ page }) => {
  const now = new Date("2026-08-08T01:00:00Z");
  await page.clock.install({ time: now });
  const initialEvent = {
    id: 24,
    title: "요청 취소 확인 이벤트",
    summary: "상세 요청 취소를 확인합니다.",
    content: "이벤트 상세 내용",
    imageUrl: null,
    startAt: new Date(now.getTime() - 60_000).toISOString(),
    endAt: new Date(now.getTime() + 4_000).toISOString(),
    published: true,
    featured: false,
    relatedProductIds: [],
    version: 1,
  };
  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/events/24", initialEvent),
    ssrApiFixture("/events", []),
  );
  let notifyDetailRequestStarted: (() => void) | undefined;
  const detailRequestStarted = new Promise<void>((resolve) => {
    notifyDetailRequestStarted = resolve;
  });
  const detailRequestFailed = page.waitForEvent("requestfailed", (request) =>
    new URL(request.url()).pathname === "/api/v1/events/24");

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/events/24") {
      notifyDetailRequestStarted?.();
      await detailRequestFailed;
      return;
    }
    if (pathname === "/api/v1/events") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/events/24");
  await page.clock.fastForward(4_250);
  await detailRequestStarted;
  await page.getByRole("link", { name: "이벤트 목록" }).click();

  const failedRequest = await detailRequestFailed;
  expect(failedRequest.failure()?.errorText).toMatch(/ERR_ABORTED|cancel/i);
  await expect(page).toHaveURL(/\/events$/);
});

test("@admin 관리자는 이벤트를 등록·수정·삭제할 수 있다", async ({ page }) => {
  let storedEvent: Record<string, unknown> | null = null;
  let createdPayload: Record<string, unknown> | null = null;
  let updatedPayload: Record<string, unknown> | null = null;
  let deletedVersion: string | null = null;

  await page.route("**/api/v1/admin/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const { pathname } = url;

    if (pathname === "/api/v1/admin/events" && request.method() === "GET") {
      await fulfillJson(route, storedEvent ? [storedEvent] : []);
      return;
    }
    if (pathname === "/api/v1/admin/events" && request.method() === "POST") {
      createdPayload = request.postDataJSON() as Record<string, unknown>;
      storedEvent = {
        ...createdPayload,
        id: 31,
        imageUrl: createdPayload.imageUrl ?? null,
        relatedProductIds: createdPayload.relatedProductIds ?? [],
        version: 1,
      };
      await fulfillJson(route, storedEvent, 201);
      return;
    }
    if (pathname === "/api/v1/admin/events/31" && request.method() === "GET") {
      await fulfillJson(route, storedEvent);
      return;
    }
    if (pathname === "/api/v1/admin/events/31" && request.method() === "PUT") {
      updatedPayload = request.postDataJSON() as Record<string, unknown>;
      storedEvent = { ...storedEvent, ...updatedPayload, version: 2 };
      await fulfillJson(route, storedEvent);
      return;
    }
    if (pathname === "/api/v1/admin/events/31" && request.method() === "DELETE") {
      deletedVersion = url.searchParams.get("expectedVersion");
      storedEvent = null;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/admin/products") {
      await fulfillJson(route, [{ id: 501, name: "연관 작품" }]);
      return;
    }
    await fulfillJson(route, []);
  });

  await openAuthenticatedAdmin(page, "events");
  await page.getByRole("button", { name: "새 이벤트 작성" }).click();
  await page.getByLabel("제목").fill("공방 오픈데이");
  await page.getByLabel("요약").fill("새 작품을 소개합니다.");
  await page.getByLabel("내용").fill("공방에서 직접 만나보세요.");
  await page.getByLabel("시작 시각").fill("2099-11-01T10:00");
  await page.getByLabel("종료 시각").fill("2099-11-30T18:00");
  await page.getByLabel("연관 작품 (#501)").check();
  await page.getByLabel("공개", { exact: true }).check();
  await page.getByLabel("홈 추천").check();
  await page.getByRole("button", { name: "등록", exact: true }).click();

  await expect(page.getByText("공방 오픈데이", { exact: true })).toBeVisible();
  expect(createdPayload).toMatchObject({
    title: "공방 오픈데이",
    published: true,
    featured: true,
    relatedProductIds: [501],
  });

  const eventCard = page.locator(".card").filter({ hasText: "공방 오픈데이" }).last();
  await eventCard.getByRole("button", { name: "수정", exact: true }).click();
  const titleInput = page.getByLabel("제목");
  await expect(titleInput).toHaveValue("공방 오픈데이");
  await titleInput.fill("수정된 공방 오픈데이");
  const editForm = titleInput.locator("xpath=ancestor::form");
  await editForm.getByRole("button", { name: "수정", exact: true }).click();

  await expect(page.getByText("수정된 공방 오픈데이", { exact: true })).toBeVisible();
  expect(updatedPayload).toMatchObject({
    title: "수정된 공방 오픈데이",
    expectedVersion: 1,
  });

  page.once("dialog", (dialog) => void dialog.accept());
  const updatedCard = page.locator(".card")
    .filter({ hasText: "수정된 공방 오픈데이" })
    .last();
  await updatedCard.getByRole("button", { name: "삭제", exact: true }).click();
  await expect(page.getByText("등록된 이벤트가 없습니다.")).toBeVisible();
  expect(deletedVersion).toBe("2");
});

test("@admin 관리자는 공개 발급 쿠폰을 등록·수정·비활성화할 수 있다", async ({ page }) => {
  let storedCoupon: Record<string, unknown> | null = null;
  let createdPayload: Record<string, unknown> | null = null;
  let updatedPayload: Record<string, unknown> | null = null;
  let deactivatedVersion: string | null = null;

  await page.route("**/api/v1/admin/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const { pathname } = url;

    if (pathname === "/api/v1/admin/coupons" && request.method() === "GET") {
      await fulfillJson(route, storedCoupon ? [storedCoupon] : []);
      return;
    }
    if (pathname === "/api/v1/admin/coupons" && request.method() === "POST") {
      createdPayload = request.postDataJSON() as Record<string, unknown>;
      storedCoupon = {
        ...createdPayload,
        id: 41,
        maxDiscountAmount: createdPayload.maxDiscountAmount ?? null,
        version: 1,
      };
      await fulfillJson(route, storedCoupon, 201);
      return;
    }
    if (pathname === "/api/v1/admin/coupons/41" && request.method() === "GET") {
      await fulfillJson(route, storedCoupon);
      return;
    }
    if (pathname === "/api/v1/admin/coupons/41" && request.method() === "PUT") {
      updatedPayload = request.postDataJSON() as Record<string, unknown>;
      storedCoupon = { ...storedCoupon, ...updatedPayload, version: 2 };
      await fulfillJson(route, storedCoupon);
      return;
    }
    if (pathname === "/api/v1/admin/coupons/41" && request.method() === "DELETE") {
      deactivatedVersion = url.searchParams.get("expectedVersion");
      storedCoupon = { ...storedCoupon, active: false, version: 3 };
      await route.fulfill({ status: 204 });
      return;
    }
    await fulfillJson(route, []);
  });

  await openAuthenticatedAdmin(page, "coupons");
  await page.getByRole("button", { name: "새 쿠폰 등록" }).click();
  await page.getByLabel("쿠폰 이름").fill("회원 감사 쿠폰");
  await page.getByLabel("할인 금액", { exact: true }).fill("5000");
  await page.getByLabel("최소 상품 금액").fill("10000");
  await page.getByLabel("사용 시작").fill("2099-10-01T00:00");
  await page.getByLabel("사용 종료").fill("2099-12-31T23:59");
  await page.getByLabel("회원이 직접 발급 가능").check();
  await page.getByRole("button", { name: "등록", exact: true }).click();

  await expect(page.getByText("회원 감사 쿠폰", { exact: true })).toBeVisible();
  expect(createdPayload).toMatchObject({
    name: "회원 감사 쿠폰",
    discountType: "FIXED",
    discountValue: 5000,
    minOrderAmount: 10000,
    active: true,
    publiclyClaimable: true,
  });
  expect(createdPayload).not.toHaveProperty("maxDiscountAmount");

  const couponCard = page.locator(".card").filter({ hasText: "회원 감사 쿠폰" }).last();
  await couponCard.getByRole("button", { name: "수정", exact: true }).click();
  const nameInput = page.getByLabel("쿠폰 이름");
  await expect(nameInput).toHaveValue("회원 감사 쿠폰");
  await nameInput.fill("수정된 회원 감사 쿠폰");
  const editForm = nameInput.locator("xpath=ancestor::form");
  await editForm.getByRole("button", { name: "수정", exact: true }).click();

  await expect(page.getByText("수정된 회원 감사 쿠폰", { exact: true })).toBeVisible();
  expect(updatedPayload).toMatchObject({
    name: "수정된 회원 감사 쿠폰",
    expectedVersion: 1,
  });

  page.once("dialog", (dialog) => void dialog.accept());
  const updatedCard = page.locator(".card")
    .filter({ hasText: "수정된 회원 감사 쿠폰" })
    .last();
  await updatedCard.getByRole("button", { name: "사용 중지", exact: true }).click();
  await expect(updatedCard.getByText("사용 중지", { exact: true })).toBeVisible();
  await expect(updatedCard.getByRole("button", { name: "사용 중지", exact: true })).toBeDisabled();
  expect(deactivatedVersion).toBe("2");
});

test("@admin 발급된 쿠폰 조건 변경 오류를 동시 수정 충돌로 오인하지 않는다", async ({ page }) => {
  const storedCoupon = {
    id: 42,
    name: "이미 발급된 쿠폰",
    discountType: "FIXED",
    discountValue: 5000,
    minOrderAmount: 10000,
    maxDiscountAmount: null,
    validFrom: "2099-10-01T00:00:00",
    validUntil: "2099-12-31T23:59:00",
    active: true,
    publiclyClaimable: true,
    version: 3,
  };

  await page.route("**/api/v1/admin/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/admin/coupons" && request.method() === "GET") {
      await fulfillJson(route, [storedCoupon]);
      return;
    }
    if (pathname === "/api/v1/admin/coupons/42" && request.method() === "GET") {
      await fulfillJson(route, storedCoupon);
      return;
    }
    if (pathname === "/api/v1/admin/coupons/42" && request.method() === "PUT") {
      await fulfillJson(route, {
        code: "COUPON_TERMS_IMMUTABLE",
        message: "이미 발급된 쿠폰의 이름·할인 조건·유효기간은 변경할 수 없습니다.",
      }, 409);
      return;
    }
    await fulfillJson(route, []);
  });

  await openAuthenticatedAdmin(page, "coupons");
  const couponCard = page.locator(".card").filter({ hasText: storedCoupon.name }).last();
  await couponCard.getByRole("button", { name: "수정", exact: true }).click();
  const nameInput = page.getByLabel("쿠폰 이름");
  await nameInput.fill("소급 변경 시도");
  const editForm = nameInput.locator("xpath=ancestor::form");
  await editForm.getByRole("button", { name: "수정", exact: true }).click();

  await expect(page.getByRole("alert").getByText(
    "이미 발급된 쿠폰의 이름·할인 조건·유효기간은 변경할 수 없습니다.",
  )).toBeVisible();
  await expect(page.getByText("다른 관리자가 먼저 수정했습니다.")).toHaveCount(0);
});
