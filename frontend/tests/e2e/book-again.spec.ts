import { expect, test, type Page } from "@playwright/test";
import type { BookingDetailResponse, ClassResponse, MyBookingDetail } from "../../src/generated/api/booking";

const bookingClass: ClassResponse = {
  id: 27, name: "가죽 공예", category: "LEATHER", durationMin: 90,
  price: 45000, bufferMin: 30, capacity: 6, passEligible: true,
  description: null, imageUrl: null, preparationInfo: null, targetAudience: null, status: "ACTIVE",
};
const previousBooking = {
  bookingId: 7, classId: 27, className: "가죽 공예", slotId: 70,
  startAt: "2026-01-12T10:00:00", endAt: "2026-01-12T11:30:00",
  participantCount: 3, depositAmount: 30000, balanceAmount: 60000, refund: null,
  cancelPolicy: {
    cancellable: false, refundable: false, deadlineAt: "2026-01-11T10:00:00",
    passCreditRestorable: false, manualCompensationRequired: false, warningCode: null,
  },
};

async function mockBookingFlow(page: Page, member: boolean, currentClasses = [bookingClass]) {
  const writes: string[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({
      status, contentType: "application/json", body: JSON.stringify(body),
    });
    if (route.request().method() !== "GET") {
      writes.push(`${route.request().method()} ${pathname}`);
      return json({ code: "UNEXPECTED_WRITE" }, 500);
    }
    switch (pathname) {
      case "/api/v1/me":
        return member
          ? json({ id: 501, name: "회원", email: "booking@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true })
          : json({ code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/passes/page": return json({ content: [], hasMore: false, nextCursor: null });
      case "/api/v1/policies/current": return json({
        terms: { version: "2026-09", documentPath: "/terms" },
        privacy: { version: "2026-09", documentPath: "/privacy" },
      });
      case "/api/v1/me/bookings/7": return json({
        ...previousBooking, status: "COMPLETED", balanceStatus: "PAID", passBooking: false, receiptUrl: null,
      } satisfies MyBookingDetail);
      case "/api/v1/bookings/7":
        expect(route.request().headers()["x-access-token"]).toBe("guest-booking-code");
        return json({
          ...previousBooking, status: "CANCELED", bookingNumber: "HG-7", guestName: "비회원", guestPhone: "01087654321",
        } satisfies BookingDetailResponse);
      case "/api/v1/me/reviews/bookings/7": return json([]);
      case "/api/v1/me/reviews/classes/7/creation-state": return json({
        sourceId: 7, sourceType: "BOOKING", targetType: "CLASS", status: "AVAILABLE",
      });
      case "/api/v1/classes": return json(currentClasses);
      case "/api/v1/slots/upcoming": return json([{
        id: 71, classId: Number(searchParams.get("classId")), bookedCount: 0,
        capacity: 6, remainingCapacity: 6, startAt: "2099-01-12T14:00:00", endAt: "2099-01-12T15:30:00",
      }]);
      default: throw new Error(`정의하지 않은 예약 테스트 요청: ${pathname}`);
    }
  });
  return writes;
}

for (const member of [true, false]) {
  test(`${member ? "회원 완료" : "비회원 취소"} 예약에서 같은 수업의 새 예약을 시작한다`, async ({ page }, testInfo) => {
    const writes = await mockBookingFlow(page, member);
    await page.setViewportSize(member ? { width: 1280, height: 900 } : { width: 390, height: 844 });
    if (member) {
      await page.goto("/my/bookings/7");
    } else {
      await page.goto("/guest/bookings");
      await page.getByLabel("예약 번호", { exact: true }).fill("7");
      await page.getByLabel("조회 코드", { exact: true }).fill("guest-booking-code");
      await page.getByRole("button", { name: "예약 조회", exact: true }).click();
    }
    const bookAgain = page.getByRole("link", { name: "같은 수업 예약", exact: true });
    await expect(bookAgain).toHaveAttribute("href", "/bookings/new?classId=27");
    await expect(page.getByRole("link", { name: "Google 캘린더에 추가" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "캘린더 파일 받기" })).toHaveCount(0);
    await page.screenshot({ path: testInfo.outputPath("booking-detail.png"), fullPage: true });
    await bookAgain.click();
    await expect(page).toHaveURL(/\/bookings\/new\?classId=27$/);
    await expect(page.getByLabel("클래스", { exact: true })).toHaveValue("27");
    await expect(page.getByLabel("날짜", { exact: true })).toHaveValue("2099-01-12");
    await expect(page.getByLabel("예약 인원", { exact: true })).toHaveCount(0);
    await page.locator('[data-slot-id="71"]').click();
    await expect(page.getByLabel("예약 인원", { exact: true })).toHaveValue("1");
    expect(writes).toEqual([]);
  });
}

test("예약이 중단된 수업으로 진입하면 안내를 보고 다른 수업을 선택한다", async ({ page }, testInfo) => {
  const writes = await mockBookingFlow(page, false, [{ ...bookingClass, id: 28, name: "위빙 공예" }]);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/bookings/new?classId=27");
  const notice = page.getByText("이 수업은 현재 예약할 수 없습니다. 다른 수업을 선택해 주세요.");
  await expect(notice).toBeVisible();
  await expect(page.getByLabel("클래스", { exact: true })).toHaveValue("");
  await page.screenshot({ path: testInfo.outputPath("unavailable-class.png"), fullPage: true });
  await page.getByLabel("클래스", { exact: true }).selectOption("28");
  await expect(notice).toHaveCount(0);
  await expect(page.locator('[data-slot-id="71"]')).toBeVisible();
  expect(writes).toEqual([]);
});
