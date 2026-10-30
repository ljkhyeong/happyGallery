import { expect, test } from "@playwright/test";
import {
  clearSsrUpstreamFixtures,
  homeSsrFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";
import {
  classFixture,
  currentPoliciesFixture,
  fulfillJson,
  fulfillMemberCommon,
  memberFixture,
  TEMPORARY_ERROR,
  UNAUTHORIZED_ERROR,
} from "./mock-support";

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

const workshop = {
  name: "해피갤러리",
  phone: "010-9635-5608",
  naverTalkUrl: "https://talk.naver.com/example",
  kakaoTalkId: "happygallery",
};

test("공방 정보 SSR 조회 실패는 오류 경계로 응답하고 새 문서 요청에서 복구한다", async ({ page }) => {
  await replaceSsrUpstreamFixtures({
    ...ssrApiFixture("/workshop", TEMPORARY_ERROR),
    status: 503,
  });

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, UNAUTHORIZED_ERROR, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await route.fallback();
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/group-classes");
  await expect(page.getByRole("heading", { name: "페이지를 불러오지 못했습니다" })).toBeVisible();
  await expect(page.getByText("수업 문의 채널을 준비하고 있습니다.")).toHaveCount(0);

  await replaceSsrUpstreamFixtures(ssrApiFixture("/workshop", workshop));
  await page.reload();
  await expect(page.getByRole("link", { name: /전화 문의 010-9635-5608/ }))
    .toBeVisible();
});

test("카테고리와 이용권 정책 조회 실패는 정상 기본값으로 숨기지 않는다", async ({ page }) => {
  let policyAttempts = 0;
  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/products", []),
    {
      ...ssrApiFixture("/products/categories", TEMPORARY_ERROR),
      status: 503,
    },
  );

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, UNAUTHORIZED_ERROR, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (pathname === "/api/v1/products/categories") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/products") {
      await route.fallback();
      return;
    }
    if (pathname === "/api/v1/payments/pass-policy") {
      policyAttempts += 1;
      await fulfillJson(
        route,
        policyAttempts <= 2
          ? TEMPORARY_ERROR
          : { totalPrice: 240000, totalCredits: 4, validityDays: 45 },
        policyAttempts <= 2 ? 503 : 200,
      );
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/products");
  await expect(page.getByRole("heading", { name: "페이지를 불러오지 못했습니다" })).toBeVisible();
  await replaceSsrUpstreamFixtures(
    ssrApiFixture("/products", []),
    ssrApiFixture("/products/categories", ["LEATHER"]),
  );
  await page.reload();
  await expect(page.getByRole("navigation", { name: "카테고리", exact: true }).getByRole("link", { name: "LEATHER" }))
    .toHaveCount(1);

  await page.goto("/passes/purchase");
  await expect(page.getByText("이용 기간을 불러오지 못했습니다."))
    .toBeVisible();
  await expect(page.getByText(/결제일 포함 90일/)).toHaveCount(0);
  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByText("결제일 포함 45일", { exact: true })).toBeVisible();
});

test("읽지 않은 알림 수 조회 실패는 0건으로 표시하지 않고 복구한다", async ({ page }) => {
  let unreadAttempts = 0;

  await replaceSsrUpstreamFixtures(...homeSsrFixtures({ workshop }));

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      unreadAttempts += 1;
      await fulfillJson(
        route,
        unreadAttempts <= 2 ? TEMPORARY_ERROR : { count: 3 },
        unreadAttempts <= 2 ? 503 : 200,
      );
      return;
    }
    if (await fulfillMemberCommon(route, {
      member: memberFixture({ id: 103, email: "notification-resilience@example.com", name: "알림 복구 회원" }),
    })) return;
    if (pathname === "/api/v1/me/notifications") {
      await fulfillJson(route, []);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/");
  const notificationButton = page.getByRole("button", {
    name: "알림, 읽지 않은 알림 수 확인 실패",
  });
  await expect(notificationButton).toBeVisible();
  await notificationButton.click();
  const popover = page.getByRole("dialog", { name: "알림 목록" });
  await expect(popover.getByRole("alert")).toContainText("서비스를 일시적으로 사용할 수 없습니다.");

  await popover.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByRole("button", { name: "알림" }).getByText("3")).toBeVisible();
});

test("@smoke 이용권 링크 예약은 이용권 조회가 복구되기 전 예약금 결제로 전환하지 않는다", async ({
  page,
}) => {
  let passAttempts = 0;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (await fulfillMemberCommon(route, {
      member: memberFixture({ id: 104, email: "pass-intent@example.com", name: "이용권 의도 회원" }),
    })) return;
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, currentPoliciesFixture("2026-07", { terms: "/terms", privacy: "/privacy" }));
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [classFixture({ id: 42, name: "이용권 가능 클래스", durationMin: 120, passEligible: true })]);
      return;
    }
    if (pathname === "/api/v1/slots/upcoming") {
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
    if (pathname === "/api/v1/me/passes/page") {
      passAttempts += 1;
      await fulfillJson(
        route,
        passAttempts <= 3
          ? TEMPORARY_ERROR
          : {
              content: [{
                expiresAt: "2099-12-31T00:00:00",
                passId: 9,
                planCode: "REGULAR_CRAFT_4",
                planName: "정규 공예 4회권",
                purchasedAt: "2098-12-01T00:00:00",
                refund: null,
                remainingCredits: 3,
                totalCredits: 4,
                totalPrice: 240000,
              }],
              hasMore: false,
              nextCursor: null,
            },
        passAttempts <= 3 ? 503 : 200,
      );
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new?passId=9&classId=42");
  const slot = page.getByRole("button", { name: /2099\. 01\. 02\. 오전 10:00/ });
  await expect(slot).toBeVisible();
  await slot.click();
  // 이용권 목록을 받기 전에도 이용권 예약 화면이라 결제 수단 없이 이용권 예약 버튼을 막아 둔다.
  const submitButton = page.getByRole("button", { name: "이용권으로 예약하기" });
  await expect(submitButton).toBeDisabled();
  await expect(page.getByRole("heading", { name: "결제 수단" })).toHaveCount(0);
  await page.getByRole("button", { name: "다시 시도" }).click();

  await expect(submitButton).toBeEnabled();
  await expect(page.getByLabel("사용할 이용권")).toHaveValue("9");
});

test("@smoke 이용권 링크 예약은 호환 클래스가 바뀌어도 링크의 이용권을 다시 선택한다", async ({
  page,
}) => {
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const { pathname } = url;

    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (await fulfillMemberCommon(route, {
      member: memberFixture({ id: 106, email: "pass-class-switch@example.com", name: "이용권 클래스 전환 회원" }),
    })) return;
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, currentPoliciesFixture("2026-07", { terms: "/terms", privacy: "/privacy" }));
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [
        classFixture({ id: 42, name: "이용권 클래스 A", durationMin: 120, passEligible: true }),
        classFixture({ id: 43, name: "이용권 클래스 B", category: "UPCYCLING", price: 45000, passEligible: true }),
      ]);
      return;
    }
    if (pathname === "/api/v1/slots/upcoming") {
      const classId = Number(url.searchParams.get("classId"));
      await fulfillJson(route, [{
        bookedCount: 0,
        capacity: 8,
        classId,
        endAt: classId === 43 ? "2099-01-03T12:00:00" : "2099-01-02T12:00:00",
        id: classId === 43 ? 4301 : 4201,
        remainingCapacity: 8,
        startAt: classId === 43 ? "2099-01-03T10:00:00" : "2099-01-02T10:00:00",
      }]);
      return;
    }
    if (pathname === "/api/v1/me/passes/page") {
      await fulfillJson(route, {
        content: [{
          expiresAt: "2099-12-31T00:00:00",
          passId: 9,
          planCode: "REGULAR_CRAFT_4",
          planName: "정규 공예 4회권",
          purchasedAt: "2098-12-01T00:00:00",
          refund: null,
          remainingCredits: 3,
          totalCredits: 4,
          totalPrice: 240000,
        }],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new?passId=9&classId=42");
  await page.getByRole("button", { name: /2099\. 01\. 02\. 오전 10:00/ }).click();
  await expect(page.getByRole("button", { name: "이용권으로 예약하기" })).toBeEnabled();

  await page.getByLabel("예약금 결제").check();
  await expect(page.getByRole("button", { name: "결제하기" })).toBeEnabled();
  await page.getByLabel("클래스").selectOption("43");
  await page.getByRole("button", { name: /2099\. 01\. 03\. 오전 10:00/ }).click();

  await expect(page.getByLabel("사용할 이용권")).toHaveValue("9");
  await expect(page.getByRole("button", { name: "이용권으로 예약하기" })).toBeEnabled();
});

test("이용권 링크로 이용권을 받지 않는 수업을 열면 시간을 고르기 전에 일반 예약으로 안내한다", async ({
  page,
}) => {
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const { pathname } = url;

    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (await fulfillMemberCommon(route, {
      member: memberFixture({ id: 107, email: "pass-fallback@example.com", name: "이용권 불가 수업 회원" }),
    })) return;
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, currentPoliciesFixture("2026-07", { terms: "/terms", privacy: "/privacy" }));
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [
        classFixture({ id: 42, name: "이용권 가능 클래스", passEligible: true }),
        classFixture({ id: 44, name: "이용권 불가 클래스", passEligible: false }),
      ]);
      return;
    }
    if (pathname === "/api/v1/slots/upcoming") {
      const classId = Number(url.searchParams.get("classId"));
      await fulfillJson(route, [{
        bookedCount: 0,
        capacity: 8,
        classId,
        endAt: "2099-01-02T12:00:00",
        id: classId * 100 + 1,
        remainingCapacity: 8,
        startAt: "2099-01-02T10:00:00",
      }]);
      return;
    }
    if (pathname === "/api/v1/me/passes/page") {
      await fulfillJson(route, {
        content: [{
          expiresAt: "2099-12-31T00:00:00",
          passId: 9,
          planCode: "REGULAR_CRAFT_4",
          planName: "정규 공예 4회권",
          purchasedAt: "2098-12-01T00:00:00",
          refund: null,
          remainingCredits: 3,
          totalCredits: 4,
          totalPrice: 240000,
        }],
        hasMore: false,
        nextCursor: null,
      });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new?passId=9&classId=44");
  const fallbackNotice = page.getByText("링크에서 선택한 이용권을 이 클래스에 사용할 수 없습니다.", { exact: false });
  await expect(fallbackNotice).toBeVisible();
  await expect(page.getByRole("heading", { name: "체험 예약", level: 1 })).toBeVisible();
  await expect(page.getByRole("heading", { name: "결제 수단" })).toBeVisible();
  await expect(page.getByRole("button", { name: "결제하기" })).toBeDisabled();

  await page.getByRole("button", { name: "예약금 결제로 계속" }).click();
  await expect(fallbackNotice).toHaveCount(0);
  await page.getByRole("button", { name: /2099\. 01\. 02\. 오전 10:00/ }).click();
  await expect(page.getByRole("button", { name: "결제하기" })).toBeEnabled();
});
