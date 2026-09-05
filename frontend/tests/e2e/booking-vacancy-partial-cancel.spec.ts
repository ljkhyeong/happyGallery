import { expect, test, type Route } from "@playwright/test";

const EMPTY_CART_VERSION = "0".repeat(64);

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

test("@smoke 비회원 빈자리 알림 신청 상태는 새로고침 후 복원되고 취소할 수 있다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  let canceled = false;
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "vacancy-alert-restore-token",
    url: baseURL,
  }]);
  await page.addInitScript(() => {
    sessionStorage.setItem("guest_vacancy_alerts", JSON.stringify({
      owner: { boundaryEpoch: null, boundaryCustomerId: null },
      value: [{
        alertId: 700,
        slotId: 77,
        status: "WAITING",
        accessToken: "guest-vacancy-access-token",
      }],
    }));
  });

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
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [{
        id: 42,
        name: "빈자리 알림 클래스",
        category: "LEATHER",
        description: null,
        durationMin: 90,
        bufferMin: 10,
        capacity: 4,
        price: 50000,
        imageUrl: null,
        passEligible: false,
        preparationInfo: null,
        status: "ACTIVE",
        targetAudience: null,
      }]);
      return;
    }
    if (pathname === "/api/v1/slots/upcoming") {
      await fulfillJson(route, [{
        id: 77,
        classId: 42,
        startAt: "2099-01-02T10:00:00",
        endAt: "2099-01-02T11:30:00",
        capacity: 4,
        bookedCount: 4,
        remainingCapacity: 0,
      }]);
      return;
    }
    if (
      pathname === "/api/v1/slots/77/vacancy-alerts"
      && request.method() === "DELETE"
    ) {
      expect(request.headers()["x-access-token"])
        .toBe("guest-vacancy-access-token");
      canceled = true;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-08-28T12:00:00",
        version: 1,
      });
      return;
    }
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-08", documentPath: "/terms/2026-08" },
        privacy: { version: "2026-08", documentPath: "/privacy/2026-08" },
      });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption("42");
  await expect(page.getByRole("button", { name: "빈자리 알림 취소" })).toBeVisible();

  await page.reload();
  await page.getByLabel("클래스").selectOption("42");
  await page.getByRole("button", { name: "빈자리 알림 취소" }).click();

  await expect.poll(() => canceled).toBe(true);
  await expect(page.getByRole("button", { name: "빈자리 알림", exact: true }))
    .toBeVisible();
  await expect.poll(() => page.evaluate(() =>
    sessionStorage.getItem("guest_vacancy_alerts"),
  )).toBeNull();
});

test("@smoke 회원은 예약 화면에서 사라진 빈자리 알림을 마이페이지에서 취소할 수 있다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  let cancelCount = 0;
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "member-vacancy-alert-token",
    url: baseURL,
  }]);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 102,
        email: "vacancy-alert@example.com",
        name: "빈자리 알림 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === "/api/v1/me/vacancy-alerts") {
      await fulfillJson(route, cancelCount === 0 ? [{
        alertId: 701,
        slotId: 78,
        className: "닫힌 회차 가죽 클래스",
        startAt: "2099-01-03T14:00:00",
        endAt: "2099-01-03T16:00:00",
        status: "WAITING",
        accessToken: null,
      }] : []);
      return;
    }
    if (
      pathname === "/api/v1/me/slots/78/vacancy-alerts"
      && request.method() === "DELETE"
    ) {
      cancelCount += 1;
      await route.fulfill({ status: 204 });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, {
        items: [],
        totalAmount: 0,
        cartVersion: EMPTY_CART_VERSION,
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
        updatedAt: "2026-08-29T12:00:00",
        version: 1,
      });
      return;
    }
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, {
        terms: { version: "2026-08", documentPath: "/terms/2026-08" },
        privacy: { version: "2026-08", documentPath: "/privacy/2026-08" },
      });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto("/my/vacancy-alerts");
  await expect(page.getByText("닫힌 회차 가죽 클래스", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "닫힌 회차 가죽 클래스 빈자리 알림 취소" }).click();

  await expect.poll(() => cancelCount).toBe(1);
  await expect(page.getByText("닫힌 회차 가죽 클래스", { exact: true })).toHaveCount(0);
  await expect(page.getByText("신청 중인 빈자리 알림이 없습니다.")).toBeVisible();
});

test("@smoke 회원은 예약 인원을 부분취소하고 환불 접수 결과를 확인한다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const bookingId = 990002;
  let participantCount = 3;
  let requestedParticipantCount: number | null = null;
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "booking-partial-cancel-token",
    url: baseURL,
  }]);

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me") {
      await fulfillJson(route, {
        id: 101,
        email: "partial-cancel@example.com",
        name: "부분취소 회원",
        phone: "01012345678",
        phoneVerified: true,
        localPasswordEnabled: true,
      });
      return;
    }
    if (pathname === `/api/v1/me/bookings/${bookingId}/participants`) {
      const body = request.postDataJSON() as { participantCount: number };
      requestedParticipantCount = body.participantCount;
      participantCount = body.participantCount;
      await fulfillJson(route, {
        bookingId,
        status: "BOOKED",
        participantCount,
        canceledParticipantCount: 1,
        depositAmount: 20000,
        balanceAmount: 0,
        refundAmount: 10000,
        refund: {
          amount: 10000,
          pgRefundAmount: 10000,
          restoreCoupon: false,
          rewardRestoreAmount: 0,
          rewardRevokeAmount: 0,
          status: "REQUESTED",
        },
      });
      return;
    }
    if (pathname === `/api/v1/me/bookings/${bookingId}`) {
      await fulfillJson(route, {
        bookingId,
        classId: 1,
        slotId: 88,
        status: "BOOKED",
        className: "부분취소 클래스",
        startAt: "2099-01-12T18:00:00",
        endAt: "2099-01-12T19:00:00",
        participantCount,
        depositAmount: participantCount === 3 ? 30000 : 20000,
        balanceAmount: 0,
        balanceStatus: "UNPAID",
        passBooking: false,
        cancelPolicy: {
          cancellable: true,
          refundable: true,
          deadlineAt: "2099-01-11T00:00:00",
          passCreditRestorable: false,
          manualCompensationRequired: false,
          warningCode: null,
        },
        refund: null,
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
    if (pathname === `/api/v1/me/reviews/bookings/${bookingId}`) {
      await fulfillJson(route, []);
      return;
    }
    if (pathname === `/api/v1/me/reviews/classes/${bookingId}/creation-state`) {
      await fulfillJson(route, {
        sourceId: bookingId,
        sourceType: "BOOKING",
        targetType: "CLASS",
        status: "NOT_REVIEWABLE",
      });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, {
        name: "해피갤러리",
        updatedAt: "2026-08-28T12:00:00",
        version: 1,
      });
      return;
    }
    await fulfillJson(route, []);
  });

  await page.goto(`/my/bookings/${bookingId}`);
  await expect(page.getByText("3명", { exact: true })).toBeVisible();
  await page.getByLabel("남길 예약 인원").selectOption("2");
  await page.getByRole("button", { name: "인원 부분취소" }).click();

  const dialog = page.getByRole("dialog", { name: "예약 인원 부분취소" });
  await expect(dialog.getByText("현재 3명 예약을 2명으로 변경합니다."))
    .toBeVisible();
  await dialog.getByRole("button", { name: "1명 부분취소" }).click();

  await expect.poll(() => requestedParticipantCount).toBe(2);
  await expect(page.getByText("2명", { exact: true })).toBeVisible();
  await expect(page.getByText(
    "1명 부분취소와 ₩10,000 환불 요청이 접수되었습니다.",
  )).toBeVisible();
});
