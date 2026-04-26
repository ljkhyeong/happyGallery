import { expect, test } from "@playwright/test";
import {
  adminCard,
  completeGuestAuthGate,
  extractFirstNumber,
  fetchClasses,
  findUniqueSlotWindow,
  formatTimeTokenForUi,
  installTossPaymentStub,
  loginAdmin,
  makePhoneNumber,
  makeUniqueLabel,
  readRouterState,
  signupCustomer,
  toDateInput,
  toDateTimeLocalInput,
  waitForSlot,
} from "./support";

test("P8-2 슬롯 생성 후 예약 생성, 변경, 취소를 완주할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

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
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await completeGuestAuthGate(page, phone, guestName);

  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("button", { name: "비회원 예약 확인하기" }).click();
  const guestBookingState = await readRouterState<{ bookingId: number; token: string }>(page);
  if (!guestBookingState?.bookingId) {
    throw new Error("Guest booking id should be kept in router state");
  }
  expect(guestBookingState?.token, "Guest booking token should be kept in router state").toBeTruthy();

  await expect(page.getByText(bookingClass.name)).toBeVisible();
  await page.getByLabel("새 슬롯 ID").fill(String(secondSlot.id));
  await page.getByRole("button", { name: "예약 변경" }).click();
  await expect(page.getByText(`현재 슬롯: #${secondSlot.id}`)).toBeVisible();

  await page.getByRole("button", { name: "예약 취소" }).click();
  await page.getByRole("button", { name: "취소 확인" }).click();
  await expect(page.getByText("취소됨")).toBeVisible();
});

test("P8-3 회원은 8회권 구매 후 8회권으로 예약할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 pass flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const slotWindow = await findUniqueSlotWindow(request, bookingClass.id, 5, 11, 11, bookingClass.durationMin);

  await loginAdmin(page);
  const slotCreateCard = adminCard(page, "슬롯 생성");
  await slotCreateCard.getByLabel("클래스").selectOption(String(bookingClass.id));
  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(slotWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(slotWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();

  const slot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(slotWindow.start));
  const slotDate = toDateInput(new Date(slot.startAt));

  await signupCustomer(page, "p8-pass-member");

  await page.goto("/passes/purchase");
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("button", { name: "내 8회권 확인하기" }).click();
  await expect(page).toHaveURL(/\/my\/passes$/);
  const passCardText = await page.locator(".my-list-card").first().textContent();
  if (!passCardText) {
    throw new Error("Member pass list text was empty");
  }
  const passId = extractFirstNumber(passCardText, "8회권 #");

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(slotDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(slot.startAt) }).first().click();
  await page.getByLabel("8회권 사용").check();
  await page.getByLabel("8회권 ID").fill(String(passId));
  await page.getByRole("button", { name: "8회권으로 예약하기" }).click();

  await expect(page).toHaveURL(/\/my\/bookings\/\d+$/);
  await expect(page.getByText("8회권 사용")).toBeVisible();
});
