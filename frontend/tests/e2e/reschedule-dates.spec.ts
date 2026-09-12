import { expect, test, type Page } from "@playwright/test";
import type { MyBookingDetail, PublicSlotResponse } from "../../src/generated/api/booking";

function slot(id: number, date: string, remainingCapacity: number, hour = "10"): PublicSlotResponse {
  return { id, classId: 27, capacity: 6, bookedCount: 6 - remainingCapacity, remainingCapacity,
    startAt: `${date}T${hour}:00:00`, endAt: `${date}T${hour}:50:00` };
}
const slots = [
  slot(70, "2099-01-12", 6),
  slot(71, "2099-01-13", 2),
  slot(74, "2099-01-15", 4),
  slot(72, "2099-01-14", 3),
  slot(73, "2099-01-14", 4, "14"),
  slot(75, "2099-02-12", 3),
];

async function openBooking(page: Page, member: boolean, options = { upcomingFailure: false, dateFailure: false, noUpcoming: false }) {
  const booking: MyBookingDetail = {
    bookingId: 7, classId: 27, className: "가죽 공예", slotId: 70,
    startAt: slots[0].startAt, endAt: slots[0].endAt, status: "BOOKED",
    participantCount: 3, depositAmount: 30000, balanceAmount: 60000, balanceStatus: "UNPAID",
    passBooking: false, receiptUrl: null, refund: null,
    cancelPolicy: { cancellable: true, refundable: true, deadlineAt: "2099-01-11T00:00:00",
      passCreditRestorable: false, manualCompensationRequired: false, warningCode: null },
  };
  const changes: number[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const { pathname, searchParams } = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({
      status, contentType: "application/json", body: JSON.stringify(body),
    });
    if (pathname === "/api/v1/auth/csrf") return route.fulfill({
      contentType: "application/json", headers: { "Set-Cookie": "XSRF-TOKEN=reschedule-test; Path=/" }, body: "{}",
    });
    if (pathname.endsWith("/reschedule")) {
      expect(route.request().method()).toBe("PATCH");
      if (!member) expect(route.request().headers()["x-access-token"]).toBe("reschedule-code");
      const { newSlotId } = route.request().postDataJSON();
      changes.push(newSlotId);
      const target = slots.find((value) => value.id === newSlotId)!;
      Object.assign(booking, { slotId: target.id, startAt: target.startAt, endAt: target.endAt });
      return json({ ...booking, bookingNumber: "HG-7" });
    }
    switch (pathname) {
      case "/api/v1/me": return member
        ? json({ id: 501, name: "회원", email: "reschedule@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true })
        : json({ code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      case "/api/v1/workshop": return json({
        name: "해피갤러리", addressLine1: "충주시 계명대로 161", addressLine2: "2층", phone: "043-123-4567",
      });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/bookings/7": return json(booking);
      case "/api/v1/bookings/7": return json({
        ...booking, bookingNumber: "HG-7", guestName: "비회원", guestPhone: "01087654321",
      });
      case "/api/v1/me/reviews/bookings/7": return json([]);
      case "/api/v1/me/reviews/classes/7/creation-state": return json({
        sourceId: 7, sourceType: "BOOKING", targetType: "CLASS", status: "NOT_REVIEWABLE",
      });
      case "/api/v1/slots/upcoming":
        expect(searchParams.get("classId")).toBe("27");
        expect(searchParams.get("days")).toBe("14");
        if (options.upcomingFailure) return json({ code: "INTERNAL_ERROR" }, 503);
        return json(options.noUpcoming ? slots.slice(0, 2) : slots.slice(0, 5));
      case "/api/v1/slots":
        if (options.dateFailure && searchParams.get("date") === "2099-01-14") return json({ code: "INTERNAL_ERROR" }, 503);
        return json(slots.filter((value) => value.startAt.startsWith(searchParams.get("date")!)));
      default: throw new Error(`정의하지 않은 예약 변경 테스트 요청: ${pathname}`);
    }
  });
  if (member) {
    await page.goto("/my/bookings/7");
  } else {
    await page.goto("/guest/bookings");
    await page.getByLabel("예약 번호", { exact: true }).fill("7");
    await page.getByLabel("조회 코드", { exact: true }).fill("reschedule-code");
    await page.getByRole("button", { name: "예약 조회", exact: true }).click();
  }
  return { changes, panel: page.locator(".card").filter({ has: page.getByText("예약 변경", { exact: true }) }) };
}

test("회원은 현재 인원이 모두 이동 가능한 날짜만 골라 예약을 변경한다", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1280, height: 900 });
  const { panel, changes } = await openBooking(page, true);
  const calendarLink = page.getByRole("link", { name: "Google 캘린더에 추가" });
  await expect(calendarLink).toHaveAttribute("href", /dates=20990112T010000Z%2F20990112T015000Z/);
  await expect(calendarLink).toHaveAttribute("target", "_blank");
  await expect(calendarLink).toHaveAttribute("rel", "noopener noreferrer");
  await page.context().route("https://calendar.google.com/**", (route) => route.fulfill({
    contentType: "text/html", body: "<title>Google 일정 작성 대역</title>",
  }));
  const popupPromise = page.waitForEvent("popup");
  await calendarLink.click();
  const popup = await popupPromise;
  await expect(popup).toHaveURL(/^https:\/\/calendar\.google\.com\/calendar\/r\/eventedit\?/);
  await popup.close();
  const quickDates = panel.getByLabel("빠른 날짜 선택 (14일 이내)");
  await expect(quickDates.locator("option")).toHaveText(["날짜를 선택하세요", "2099. 01. 14.", "2099. 01. 15."]);
  await quickDates.selectOption("2099-01-14");
  await expect(panel.getByLabel("변경할 날짜")).toHaveValue("2099-01-14");
  await expect(panel.locator("[data-slot-id]")).toHaveCount(2);
  await panel.locator('[data-slot-id="72"]').click();
  await page.screenshot({ path: testInfo.outputPath("desktop.png"), fullPage: true });
  await panel.getByRole("button", { name: "선택한 시간으로 변경" }).click();
  await expect(page.getByText("회원 예약이 변경되었습니다.", { exact: true })).toBeVisible();
  expect(changes).toEqual([72]);
  await expect(calendarLink).toHaveAttribute("href", /dates=20990114T010000Z%2F20990114T015000Z/);
  await expect(panel.locator('[data-slot-id="72"]')).toHaveCount(0);
  await expect(panel.getByRole("button", { name: "선택한 시간으로 변경" })).toBeDisabled();
});

test("비회원은 날짜·시간 조회 실패를 재시도하고 직접 입력한 날짜도 조회한다", async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  const options = { upcomingFailure: true, dateFailure: true, noUpcoming: false };
  const { panel, changes } = await openBooking(page, false, options);
  const calendarLink = page.getByRole("link", { name: "Google 캘린더에 추가" });
  await expect(calendarLink).toHaveAttribute("href", /dates=20990112T010000Z%2F20990112T015000Z/);
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "캘린더 파일 받기" }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe("해피갤러리-가죽 공예.ics");
  const stream = await download.createReadStream();
  const chunks: Buffer[] = [];
  for await (const chunk of stream!) chunks.push(Buffer.from(chunk));
  expect(Buffer.concat(chunks).toString("utf8")).toContain("DTSTART:20990112T010000Z");
  expect(await calendarLink.getAttribute("href")).not.toContain("reschedule-code");
  await expect(page.getByText("예약을 변경·취소하면 저장한 일정도 직접 수정해 주세요.")).toBeVisible();
  await expect(panel.getByRole("alert")).toBeVisible();
  await panel.getByLabel("변경할 날짜").fill("2099-02-12");
  await expect(panel.locator('[data-slot-id="75"]')).toBeVisible();
  options.upcomingFailure = false;
  await panel.getByRole("button", { name: "다시 시도" }).click();
  await expect(panel.getByRole("alert")).toHaveCount(0);
  await panel.getByLabel("빠른 날짜 선택 (14일 이내)").selectOption("2099-01-14");
  await expect(panel.getByRole("alert")).toBeVisible();
  await expect(panel.getByLabel("변경할 날짜")).toHaveValue("2099-01-14");
  await page.screenshot({ path: testInfo.outputPath("mobile-retry.png"), fullPage: true });
  options.dateFailure = false;
  await panel.getByRole("button", { name: "다시 시도" }).click();
  await expect(panel.locator('[data-slot-id="72"]')).toBeVisible();
  await panel.locator('[data-slot-id="72"]').click();
  await panel.getByRole("button", { name: "선택한 시간으로 변경" }).click();
  await expect(page.getByText("예약이 변경되었습니다.", { exact: true })).toBeVisible();
  expect(changes).toEqual([72]);
  await expect(calendarLink).toHaveAttribute("href", /dates=20990114T010000Z%2F20990114T015000Z/);
});

test("14일 내 후보가 없어도 날짜를 직접 입력해 변경 가능한 시간을 찾는다", async ({ page }) => {
  const { panel } = await openBooking(page, true, { upcomingFailure: false, dateFailure: false, noUpcoming: true });
  const quickDates = panel.getByLabel("빠른 날짜 선택 (14일 이내)");
  await expect(quickDates).toBeDisabled();
  await expect(quickDates.locator("option:checked")).toHaveText("14일 내 변경 가능한 날짜가 없습니다");
  await panel.getByLabel("변경할 날짜").fill("2099-02-12");
  await expect(panel.locator('[data-slot-id="75"]')).toBeVisible();
  await panel.locator('[data-slot-id="75"]').click();
  await expect(panel.getByRole("button", { name: "선택한 시간으로 변경" })).toBeEnabled();
});
