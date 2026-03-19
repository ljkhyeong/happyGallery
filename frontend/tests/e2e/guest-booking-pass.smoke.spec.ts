import { expect, test } from "@playwright/test";
import {
  adminCard,
  completeGuestAuthGate,
  extractFirstNumber,
  fetchClasses,
  findUniqueSlotWindow,
  formatTimeTokenForUi,
  loginAdmin,
  makePhoneNumber,
  makeUniqueLabel,
  toDateInput,
  toDateTimeLocalInput,
  waitForBookingByPhone,
  waitForSlot,
} from "./support";
import { extractCodeText, successAlert } from "./ui-support";

test("P8-2 슬롯 생성 후 예약 생성, 변경, 취소를 완주할 수 있다", async ({ page, request }) => {
  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 booking flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const firstWindow = await findUniqueSlotWindow(request, bookingClass.id, 4, 10, 7, bookingClass.durationMin);
  const secondWindow = await findUniqueSlotWindow(request, bookingClass.id, 4, 14, 37, bookingClass.durationMin);
  const bookingDate = toDateInput(firstWindow.start);
  const phone = makePhoneNumber(makeUniqueLabel("p8-booking"));
  const guestName = makeUniqueLabel("P8 예약자");

  await loginAdmin(page);

  const slotCreateCard = adminCard(page, "슬롯 생성");
  await slotCreateCard.getByLabel("클래스").selectOption(String(bookingClass.id));
  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(firstWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(firstWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();
  const firstSlot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(firstWindow.start));

  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(secondWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(secondWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();
  const secondSlot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(secondWindow.start));

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(bookingDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(firstSlot.startAt) }).first().click();
  await page.getByLabel("예약금 (원)").fill("30000");
  await page.getByRole("button", { name: /예약하기/ }).click();
  await completeGuestAuthGate(page, phone, guestName);

  await expect(successAlert(page, "예약이 완료되었습니다!")).toBeVisible();
  const bookingToken = await extractCodeText(page);
  const booking = await waitForBookingByPhone(request, bookingDate, phone);

  await page.goto("/guest");
  await page.getByRole("link", { name: "비회원 예약 조회" }).click();
  await page.getByLabel("예약 번호").fill(String(booking.bookingId));
  await page.getByLabel("인증 토큰").fill(bookingToken);
  await page.getByRole("button", { name: "예약 조회" }).click();

  await expect(page.getByText(booking.bookingNumber)).toBeVisible();
  await page.getByLabel("새 슬롯 ID").fill(String(secondSlot.id));
  await page.getByRole("button", { name: "예약 변경" }).click();
  await expect(page.getByText(`현재 슬롯: #${secondSlot.id}`)).toBeVisible();

  await page.getByRole("button", { name: "예약 취소" }).click();
  await page.getByRole("button", { name: "취소 확인" }).click();
  await expect(page.getByText("취소됨")).toBeVisible();
});

test("P8-3 8회권 구매 후 8회권으로 예약할 수 있다", async ({ page, request }) => {
  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 pass flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const slotWindow = await findUniqueSlotWindow(request, bookingClass.id, 5, 11, 11, bookingClass.durationMin);
  const phone = makePhoneNumber(makeUniqueLabel("p8-pass"));
  const guestName = makeUniqueLabel("P8 수강생");

  await loginAdmin(page);
  const slotCreateCard = adminCard(page, "슬롯 생성");
  await slotCreateCard.getByLabel("클래스").selectOption(String(bookingClass.id));
  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(slotWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(slotWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();

  const slot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(slotWindow.start));
  const slotDate = toDateInput(new Date(slot.startAt));

  await page.goto("/passes/purchase");
  await page.getByLabel("결제 금액 (원)").fill("120000");
  await page.getByRole("button", { name: /8회권 구매/ }).click();
  await completeGuestAuthGate(page, phone, guestName);

  await expect(page.getByText("8회권 구매가 완료되었습니다!")).toBeVisible();
  const passCardText = await page.locator(".card").last().textContent();
  if (!passCardText) {
    throw new Error("Pass success card text was empty");
  }
  const passId = extractFirstNumber(passCardText, "8회권 ID:");

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(slotDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(slot.startAt) }).first().click();
  await page.getByLabel("8회권 사용").check();
  await page.getByLabel("8회권 ID").fill(String(passId));
  await page.getByRole("button", { name: "예약하기" }).click();
  await completeGuestAuthGate(page, phone, guestName);

  await expect(successAlert(page, "예약이 완료되었습니다!")).toBeVisible();
  const booking = await waitForBookingByPhone(request, slotDate, phone);
  expect(booking.passBooking).toBe(true);
});
