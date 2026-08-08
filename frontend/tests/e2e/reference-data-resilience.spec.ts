import { expect, test, type Route } from "@playwright/test";

const EMPTY_CART_VERSION = "0".repeat(64);

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

const temporaryError = {
  code: "SERVICE_UNAVAILABLE",
  message: "잠시 후 다시 시도해 주세요.",
};

const workshop = {
  name: "해피갤러리",
  phone: "010-9635-5608",
  naverTalkUrl: "https://talk.naver.com/example",
  kakaoTalkId: "happygallery",
};

test("공방 정보 조회 실패는 문의 채널 부재로 단정하지 않고 다시 조회한다", async ({ page }) => {
  let workshopAttempts = 0;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      workshopAttempts += 1;
      await fulfillJson(
        route,
        workshopAttempts <= 2 ? temporaryError : workshop,
        workshopAttempts <= 2 ? 503 : 200,
      );
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/group-classes");
  const inquirySection = page.locator(".group-class-inquiry");
  await expect(inquirySection.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await expect(inquirySection.getByText("수업 문의 채널을 준비하고 있습니다.")).toHaveCount(0);

  await inquirySection.getByRole("button", { name: "다시 시도" }).click();
  await expect(inquirySection.getByRole("link", { name: /전화 문의 010-9635-5608/ }))
    .toBeVisible();
});

test("카테고리와 8회권 정책 조회 실패는 정상 기본값으로 숨기지 않는다", async ({ page }) => {
  let categoryAttempts = 0;
  let policyAttempts = 0;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (pathname === "/api/v1/products/categories") {
      categoryAttempts += 1;
      await fulfillJson(
        route,
        categoryAttempts <= 2 ? temporaryError : ["LEATHER"],
        categoryAttempts <= 2 ? 503 : 200,
      );
      return;
    }
    if (pathname === "/api/v1/products") {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === "/api/v1/payments/pass-policy") {
      policyAttempts += 1;
      await fulfillJson(
        route,
        policyAttempts <= 2
          ? temporaryError
          : { totalPrice: 240000, totalCredits: 8, validityDays: 45 },
        policyAttempts <= 2 ? 503 : 200,
      );
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/products");
  await expect(page.getByRole("button", { name: "다시 시도" })).toBeVisible();
  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByLabel("카테고리").getByRole("option", { name: "LEATHER" }))
    .toHaveCount(1);

  await page.goto("/passes/purchase");
  await expect(page.getByText("이용 기간은 서버 판매 정책을 확인한 뒤 표시합니다."))
    .toBeVisible();
  await expect(page.getByText(/결제일 포함 90일/)).toHaveCount(0);
  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByText(/결제일 포함 45일/)).toBeVisible();
});

test("읽지 않은 알림 수 조회 실패는 0건으로 표시하지 않고 복구한다", async ({ page }) => {
  let unreadAttempts = 0;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 103,
        email: "notification-resilience@example.com",
        name: "알림 복구 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      unreadAttempts += 1;
      await fulfillJson(
        route,
        unreadAttempts <= 2 ? temporaryError : { count: 3 },
        unreadAttempts <= 2 ? 503 : 200,
      );
      return;
    }
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
  await expect(page.getByText("읽지 않은 알림 수를 확인하지 못했습니다.")).toBeVisible();

  await page.getByRole("button", { name: "다시 시도" }).click();
  await expect(page.getByRole("button", { name: "알림" }).getByText("3")).toBeVisible();
});

test("@smoke 8회권 링크 예약은 이용권 조회가 복구되기 전 예약금 결제로 전환하지 않는다", async ({
  page,
}) => {
  let passAttempts = 0;

  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 104,
        email: "pass-intent@example.com",
        name: "8회권 의도 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
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
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-07", documentPath: "/terms" },
        privacy: { version: "2026-07", documentPath: "/privacy" },
      });
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [{
        bufferMin: 30,
        category: "LEATHER",
        description: null,
        durationMin: 120,
        id: 42,
        imageUrl: null,
        name: "8회권 가능 클래스",
        passEligible: true,
        preparationInfo: null,
        price: 50000,
        status: "ACTIVE",
        targetAudience: null,
      }]);
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
          ? temporaryError
          : {
              content: [{
                expiresAt: "2099-12-31T00:00:00",
                passId: 9,
                planCode: "REGULAR_CRAFT_8",
                planName: "정규 공예 8회권",
                purchasedAt: "2098-12-01T00:00:00",
                refund: null,
                remainingCredits: 7,
                totalCredits: 8,
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
  const submitButton = page.getByRole("button", { name: "결제 진행하기" });
  await expect(submitButton).toBeDisabled();
  await page.getByRole("button", { name: "다시 시도" }).click();

  await expect(page.getByRole("button", { name: "8회권으로 예약하기" })).toBeEnabled();
  await expect(page.getByLabel("사용할 8회권")).toHaveValue("9");
});

test("@smoke 8회권 링크 예약은 호환 클래스가 바뀌어도 링크의 이용권을 다시 선택한다", async ({
  page,
}) => {
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const { pathname } = url;

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 106,
        email: "pass-class-switch@example.com",
        name: "8회권 클래스 전환 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, workshop);
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
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-07", documentPath: "/terms" },
        privacy: { version: "2026-07", documentPath: "/privacy" },
      });
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [
        {
          bufferMin: 30,
          category: "LEATHER",
          description: null,
          durationMin: 120,
          id: 42,
          imageUrl: null,
          name: "8회권 클래스 A",
          passEligible: true,
          preparationInfo: null,
          price: 50000,
          status: "ACTIVE",
          targetAudience: null,
        },
        {
          bufferMin: 30,
          category: "UPCYCLING",
          description: null,
          durationMin: 90,
          id: 43,
          imageUrl: null,
          name: "8회권 클래스 B",
          passEligible: true,
          preparationInfo: null,
          price: 45000,
          status: "ACTIVE",
          targetAudience: null,
        },
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
          planCode: "REGULAR_CRAFT_8",
          planName: "정규 공예 8회권",
          purchasedAt: "2098-12-01T00:00:00",
          refund: null,
          remainingCredits: 7,
          totalCredits: 8,
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
  await expect(page.getByRole("button", { name: "8회권으로 예약하기" })).toBeEnabled();

  await page.getByLabel("예약금 결제").check();
  await expect(page.getByRole("button", { name: "결제 진행하기" })).toBeEnabled();
  await page.getByLabel("클래스").selectOption("43");
  await page.getByRole("button", { name: /2099\. 01\. 03\. 오전 10:00/ }).click();

  await expect(page.getByLabel("사용할 8회권")).toHaveValue("9");
  await expect(page.getByRole("button", { name: "8회권으로 예약하기" })).toBeEnabled();
});
