import { expect, test } from "@playwright/test";
import {
  adminCard,
  completeGuestAuthGate,
  createAdminSlot,
  extractFirstNumber,
  fetchClasses,
  fetchGuestBookingSlot,
  findUniqueSlotStart,
  installTossPaymentStub,
  loginAdmin,
  makePhoneNumber,
  makeUniqueLabel,
  openAdminView,
  readRouterState,
  signupCustomer,
  toDateInput,
} from "./support";

test("P8-2 @smoke @payment 슬롯 생성 후 예약 생성, 변경, 취소를 완주할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const classes = await fetchClasses(request);
  expect(classes.length, "P8 booking flow requires at least one class in the local DB")
    .toBeGreaterThan(0);
  const bookingClass = classes[0]!;

  const firstSlotStart = await findUniqueSlotStart(request, bookingClass.id, 4, 10, 7);
  const secondSlotStart = await findUniqueSlotStart(request, bookingClass.id, 5, 14, 37);
  const bookingDate = toDateInput(firstSlotStart);
  const phone = makePhoneNumber(makeUniqueLabel("p8-booking"));
  const guestName = makeUniqueLabel("P8 예약자");

  const firstSlot = await createAdminSlot(request, {
    classId: bookingClass.id,
    startAt: firstSlotStart,
  });
  const secondSlot = await createAdminSlot(request, {
    classId: bookingClass.id,
    startAt: secondSlotStart,
  });

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").selectOption(bookingDate);
  await page.locator(`[data-slot-id="${firstSlot.id}"]`).click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await completeGuestAuthGate(page, phone, guestName);

  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("link", { name: "비회원 예약 확인하기" }).click();
  const guestBookingState = await readRouterState<{ bookingId: number; token: string }>(page);
  if (!guestBookingState?.bookingId) {
    throw new Error("Guest booking id should be kept in router state");
  }
  expect(guestBookingState?.token, "Guest booking token should be kept in router state").toBeTruthy();

  const booked = await fetchGuestBookingSlot(
    request,
    guestBookingState.bookingId,
    guestBookingState.token,
  );
  const targetSlot = booked.slotId === firstSlot.id ? secondSlot : firstSlot;
  const targetDate = targetSlot.id === firstSlot.id
    ? toDateInput(firstSlotStart)
    : toDateInput(secondSlotStart);
  expect([firstSlot.id, secondSlot.id]).toContain(booked.slotId);

  await expect(page.getByText(bookingClass.name)).toBeVisible();
  await expect(page.getByText("1명", { exact: true })).toBeVisible();
  const rescheduleCard = page.locator(".card").filter({ hasText: "예약 변경" }).last();
  const dateInput = rescheduleCard.getByLabel("변경할 날짜");
  await dateInput.fill(targetDate);
  await expect(dateInput).toHaveValue(targetDate);
  const targetButton = rescheduleCard.locator(`[data-slot-id="${targetSlot.id}"]`);
  await expect(targetButton).toBeVisible();
  await targetButton.click();
  const submitButton = rescheduleCard.getByRole("button", { name: "선택한 시간으로 변경" });
  await expect(submitButton).toBeEnabled();
  await submitButton.click();
  await expect.poll(async () => (
    await fetchGuestBookingSlot(request, guestBookingState.bookingId, guestBookingState.token)
  ).slotId).toBe(targetSlot.id);

  await page.getByRole("button", { name: "예약 취소" }).click();
  await page.getByRole("button", { name: "취소 확인" }).click();
  await expect(page.getByText("취소됨")).toBeVisible();

  await loginAdmin(page);
  await openAdminView(page, "현황·검색");
  const searchCard = adminCard(page, "주문·예약 검색");
  await searchCard.getByRole("button", { name: "예약", exact: true }).click();
  await searchCard.getByLabel("식별자 또는 고객명").fill(guestName);
  await searchCard.getByRole("button", { name: "검색", exact: true }).click();
  const searchRow = searchCard.locator("tbody tr").filter({ hasText: guestName }).first();
  await expect(searchRow).toBeVisible();
  await searchRow.getByRole("link", { name: "운영" }).click();

  await expect(page).toHaveURL(
    new RegExp(`view=bookings.*bookingId=${guestBookingState.bookingId}`),
  );
  const bookingCard = adminCard(page, "예약 목록");
  await expect(
    bookingCard.locator(`#admin-booking-${guestBookingState.bookingId}`),
  ).toContainText(guestName);
});

test("P8-3 @smoke @payment 회원은 8회권 구매 후 8회권으로 예약할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const classes = await fetchClasses(request);
  expect(classes.length, "P8 pass flow requires at least one class in the local DB")
    .toBeGreaterThan(0);
  const bookingClass = classes.find((candidate) => candidate.passEligible);
  expect(bookingClass, "P8 pass flow requires a pass-eligible class in the local DB")
    .toBeDefined();

  const slotStart = await findUniqueSlotStart(request, bookingClass!.id, 5, 11, 11);
  const slot = await createAdminSlot(request, {
    classId: bookingClass!.id,
    startAt: slotStart,
  });
  const slotDate = toDateInput(new Date(slot.startAt));

  await signupCustomer(page, "p8-pass-member");

  await page.goto("/passes/purchase");
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("link", { name: "내 8회권 확인하기" }).click();
  await expect(page).toHaveURL(/\/my\/passes$/);
  const passCardText = await page.locator(".my-list-card").first().textContent();
  if (!passCardText) {
    throw new Error("Member pass list text was empty");
  }
  const passId = extractFirstNumber(passCardText, "8회권 #");

  const passCard = page.locator(".my-list-card").filter({ hasText: `8회권 #${passId}` }).first();
  await passCard.getByRole("link", { name: "이 8회권으로 예약" }).click();
  await expect(page).toHaveURL(new RegExp(`/bookings/new\\?passId=${passId}$`));
  await page.getByLabel("클래스").selectOption(String(bookingClass!.id));
  await page.getByLabel("날짜").selectOption(slotDate);
  await page.locator(`[data-slot-id="${slot.id}"]`).click();
  await expect(page.getByLabel("8회권 사용")).toBeChecked();
  await expect(page.getByLabel("사용할 8회권")).toHaveValue(String(passId));
  await page.getByRole("button", { name: "8회권으로 예약하기" }).click();

  await expect(page).toHaveURL(/\/my\/bookings\/\d+$/);
  await expect(page.getByText("8회권 사용")).toBeVisible();
});
