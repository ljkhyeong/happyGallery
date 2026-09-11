import { expect, test, type Page } from "@playwright/test";
import type { ClassResponse, PublicSlotResponse, VacancyAlertResponse } from "../../src/generated/api/booking";
import type { NotificationResponse } from "../../src/generated/api/notification";

const bookingClass: ClassResponse = {
  id: 42, name: "가죽 카드지갑", category: "LEATHER", durationMin: 90,
  price: 50000, bufferMin: 10, capacity: 8, passEligible: false,
  description: null, imageUrl: null, preparationInfo: null, targetAudience: null, status: "ACTIVE",
};
const notified: VacancyAlertResponse = {
  alertId: 701, classId: 42, slotId: 78, className: bookingClass.name,
  startAt: "2099-01-03T14:00:00", endAt: "2099-01-03T15:30:00",
  status: "NOTIFIED", accessToken: null,
};
const availableSlot: PublicSlotResponse = {
  id: 78, classId: 42, startAt: notified.startAt, endAt: notified.endAt,
  capacity: 8, bookedCount: 6, remainingCapacity: 2,
};

async function mockVacancyFlow(page: Page) {
  const state = {
    alerts: [notified],
    slots: [
      { ...availableSlot, id: 77, startAt: "2099-01-02T10:00:00", endAt: "2099-01-02T11:30:00" },
      availableSlot,
    ],
    classes: [bookingClass],
    slotError: false,
    slotReads: 0,
    registrations: 0,
    cancellations: 0,
    unexpectedWrites: [] as string[],
  };
  const notification: NotificationResponse = {
    id: 901, eventType: "BOOKING_VACANCY_AVAILABLE", aggregateType: "VACANCY_ALERT", aggregateId: 701,
    contextTitle: null, scheduledAt: null, deliveredAt: "2026-09-12T10:00:00", readAt: null, read: false,
  };
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const json = (body: unknown, status = 200) => route.fulfill({
      status, contentType: "application/json", body: JSON.stringify(body),
    });
    if (pathname === "/api/v1/auth/csrf") {
      return route.fulfill({ status: 200, contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=vacancy-link-test; Path=/" }, body: "{}" });
    }
    if (pathname === "/api/v1/me/notifications/901/read") {
      notification.read = true;
      return route.fulfill({ status: 204 });
    }
    if (pathname === "/api/v1/me/slots/78/vacancy-alerts") {
      if (request.method() === "POST") {
        state.registrations += 1;
        const waiting: VacancyAlertResponse = { ...notified, alertId: 702, status: "WAITING" };
        state.alerts = [waiting, ...state.alerts];
        return json(waiting);
      }
      if (request.method() === "DELETE") {
        state.cancellations += 1;
        state.alerts = state.alerts.filter((alert) => alert.status !== "WAITING");
        return route.fulfill({ status: 204 });
      }
    }
    if (request.method() !== "GET") {
      state.unexpectedWrites.push(`${request.method()} ${pathname}`);
      return json({ code: "UNEXPECTED_WRITE" }, 500);
    }
    switch (pathname) {
      case "/api/v1/me": return json({
        id: 501, name: "회원", email: "vacancy@example.com", phone: "01012345678",
        phoneVerified: true, localPasswordEnabled: true,
      });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: notification.read ? 0 : 1 });
      case "/api/v1/me/notifications": return json([notification]);
      case "/api/v1/me/passes/page": return json({ content: [], hasMore: false, nextCursor: null });
      case "/api/v1/me/vacancy-alerts": return json(state.alerts);
      case "/api/v1/classes": return json(state.classes);
      case "/api/v1/policies/current": return json({
        terms: { version: "2026-09", documentPath: "/terms" },
        privacy: { version: "2026-09", documentPath: "/privacy" },
      });
      case "/api/v1/slots/upcoming":
        state.slotReads += 1;
        return state.slotError
          ? json({ code: "INTERNAL_ERROR", message: "일정을 불러오지 못했습니다." }, 503)
          : json(state.slots);
      default: throw new Error(`정의하지 않은 빈자리 알림 테스트 요청: ${pathname}`);
    }
  });
  return state;
}

test("빈자리 알림에서 해당 날짜로 이동하고 재방문 시 최신 잔여 좌석을 확인한다", async ({ page }, testInfo) => {
  const state = await mockVacancyFlow(page);
  await page.goto("/my/notifications");
  await page.getByRole("link", { name: "예약 빈자리 안내", exact: true }).click();
  await expect(page).toHaveURL(/\/my\/vacancy-alerts$/);
  await expect(page.getByText("빈자리 발생", { exact: true })).toBeVisible();
  await expect(page.getByText("대기 중 0건", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: /빈자리 알림 취소/ })).toHaveCount(0);
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 900 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`vacancy-history-${width}.png`), fullPage: true, animations: "disabled" });
  }
  await page.getByRole("link", { name: "가죽 카드지갑 예약 가능 여부 확인" }).click();
  await expect(page).toHaveURL(/\/bookings\/new\?classId=42&slotId=78$/);
  await expect(page.getByLabel("클래스", { exact: true })).toHaveValue("42");
  await expect(page.getByLabel("날짜", { exact: true })).toHaveValue("2099-01-03");
  await expect(page.locator('[data-slot-id="78"]')).toContainText("알림 신청한 일정");
  await expect(page.locator('[data-slot-id="78"]')).toContainText("2명 예약 가능");
  await expect(page.getByLabel("예약 인원", { exact: true })).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath("vacancy-booking-390.png"), fullPage: true, animations: "disabled" });
  await page.getByLabel("날짜", { exact: true }).selectOption("2099-01-02");
  await expect(page.locator('[data-slot-id="77"]')).toBeVisible();
  const readsBeforeReturn = state.slotReads;
  await page.goBack();
  await expect(page.getByText("빈자리 발생", { exact: true })).toBeVisible();
  state.slots[1] = { ...availableSlot, bookedCount: 8, remainingCapacity: 0 };
  await page.getByRole("link", { name: "가죽 카드지갑 예약 가능 여부 확인" }).click();
  await expect.poll(() => state.slotReads).toBeGreaterThan(readsBeforeReturn);
  await expect(page.getByLabel("날짜", { exact: true })).toHaveValue("2099-01-03");
  await expect(page.locator('[data-slot-id="78"]')).toContainText("만석");
  await expect(page.getByRole("button", { name: "빈자리 알림", exact: true })).toBeVisible();
  expect(state.unexpectedWrites).toEqual([]);
});

test("다시 만석이 되면 알림을 재신청하고 취소 후에도 이전 내역을 보존한다", async ({ page }) => {
  const state = await mockVacancyFlow(page);
  state.slots[1] = { ...availableSlot, bookedCount: 8, remainingCapacity: 0 };
  await page.goto("/bookings/new?classId=42&slotId=78");
  await page.getByRole("button", { name: "빈자리 알림", exact: true }).click();
  await expect.poll(() => state.registrations).toBe(1);
  await page.getByRole("button", { name: "빈자리 알림 취소", exact: true }).click();
  await expect.poll(() => state.cancellations).toBe(1);
  await expect(page.getByRole("button", { name: "빈자리 알림", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "알림", exact: true }).click();
  await page.getByRole("dialog", { name: "알림 목록" }).getByRole("button", { name: /예약 빈자리 안내/ }).click();
  await expect(page).toHaveURL(/\/my\/vacancy-alerts$/);
  await expect(page.getByText("빈자리 발생", { exact: true })).toBeVisible();
  await expect(page.getByText("대기 중 0건", { exact: true })).toBeVisible();
  expect(state.unexpectedWrites).toEqual([]);
});

test("일정 조회 실패는 재시도하고 사라진 일정은 다른 일정으로 안내한다", async ({ page }) => {
  const state = await mockVacancyFlow(page);
  state.slotError = true;
  state.slots = state.slots.filter((slot) => slot.id !== 78);
  await page.goto("/bookings/new?classId=42&slotId=78");
  await expect(page.getByRole("button", { name: "다시 시도", exact: true })).toBeVisible();
  await expect(page.getByText(/이 일정은 현재 예약할 수 없습니다/)).toHaveCount(0);
  state.slotError = false;
  await page.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(page.getByText(/이 일정은 현재 예약할 수 없습니다/)).toBeVisible();
  await expect(page.getByLabel("날짜", { exact: true })).toHaveValue("2099-01-02");
  await expect(page.getByLabel("예약 인원", { exact: true })).toHaveCount(0);
  await page.locator('[data-slot-id="77"]').click();
  await expect(page.getByLabel("예약 인원", { exact: true })).toHaveValue("1");
  expect(state.unexpectedWrites).toEqual([]);
});
