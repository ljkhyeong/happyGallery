import { expect, test, type Page } from "@playwright/test";
import {
  adminCard,
  armNextRefundFailure,
  clearNextRefundFailure,
  completePhoneVerification,
  extractFirstNumber,
  fetchClasses,
  loginAdmin,
  makePhoneNumber,
  makeUniqueLabel,
  formatTimeTokenForUi,
  plusDays,
  toDateInput,
  toDateTimeLocalInput,
  waitForBookingByPhone,
  waitForFailedRefundByOrderId,
  waitForFailedRefundGone,
  waitForOrder,
  waitForProduct,
  waitForSlot,
} from "./support";

test.describe.configure({ mode: "serial" });

function successAlert(page: Page, text: string) {
  return page.locator(".alert.alert-success").filter({ hasText: text }).first();
}

async function extractCodeText(page: Page): Promise<string> {
  const code = await page.locator(".card code").first().textContent();
  if (!code) {
    throw new Error("Could not find code value in success card");
  }
  return code.trim();
}

test("P8-1 상품 등록 후 관리자 목록에서 확인할 수 있다", async ({ page, request }) => {
  const productName = makeUniqueLabel("P8-상품");

  await loginAdmin(page);

  const createCard = adminCard(page, "상품 등록");
  await createCard.getByLabel("상품명").fill(productName);
  await createCard.getByLabel("유형").selectOption("READY_STOCK");
  await createCard.getByLabel("가격 (원)").fill("19000");
  await createCard.getByLabel("수량").fill("4");
  await createCard.getByRole("button", { name: "상품 등록" }).click();

  const product = await waitForProduct(request, productName);
  expect(product.available).toBe(true);

  const listCard = adminCard(page, "상품 목록");
  await expect(listCard.locator("tbody tr").filter({ hasText: productName })).toContainText("판매 가능");
});

test("P8-2 슬롯 생성 후 예약 생성, 변경, 취소를 완주할 수 있다", async ({ page, request }) => {
  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 booking flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const firstWindow = plusDays(4, 10, 7, bookingClass.durationMin);
  const secondWindow = plusDays(4, 14, 37, bookingClass.durationMin);
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
  await completePhoneVerification(page, phone);

  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(bookingDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(firstSlot.startAt) }).first().click();

  await page.getByLabel("이름").fill(guestName);
  await page.getByLabel("예약금 (원)").fill("30000");
  await page.getByRole("button", { name: /예약하기/ }).click();

  await expect(successAlert(page, "예약이 완료되었습니다!")).toBeVisible();
  const bookingToken = await extractCodeText(page);
  const booking = await waitForBookingByPhone(request, bookingDate, phone);

  await page.goto("/bookings/manage");
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

  const slotWindow = plusDays(5, 11, 11, bookingClass.durationMin);
  const slotDate = toDateInput(slotWindow.start);
  const phone = makePhoneNumber(makeUniqueLabel("p8-pass"));
  const guestName = makeUniqueLabel("P8 수강생");

  await loginAdmin(page);
  const slotCreateCard = adminCard(page, "슬롯 생성");
  await slotCreateCard.getByLabel("클래스").selectOption(String(bookingClass.id));
  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(slotWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(slotWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();

  const slot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(slotWindow.start));

  await page.goto("/passes/purchase");
  await completePhoneVerification(page, phone);
  await page.getByLabel("이름").fill(guestName);
  await page.getByLabel("결제 금액 (원)").fill("120000");
  await page.getByRole("button", { name: /8회권 구매/ }).click();

  await expect(successAlert(page, "8회권 구매가 완료되었습니다!")).toBeVisible();
  const passCardText = await page.locator(".card").last().textContent();
  if (!passCardText) {
    throw new Error("Pass success card text was empty");
  }
  const passId = extractFirstNumber(passCardText, "8회권 ID:");

  await page.goto("/bookings/new");
  await completePhoneVerification(page, phone);
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(slotDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(slot.startAt) }).first().click();
  await page.getByLabel("이름").fill(guestName);
  await page.getByLabel("8회권 사용").check();
  await page.getByLabel("8회권 ID").fill(String(passId));
  await page.getByRole("button", { name: "예약하기" }).click();

  await expect(successAlert(page, "예약이 완료되었습니다!")).toBeVisible();
  const booking = await waitForBookingByPhone(request, slotDate, phone);
  expect(booking.passBooking).toBe(true);
});

test("P8-4 주문 생성 후 관리자 승인, 픽업 준비, 픽업 완료까지 진행할 수 있다", async ({ page, request }) => {
  const productName = makeUniqueLabel("P8-주문상품");
  const phone = makePhoneNumber(makeUniqueLabel("p8-order"));
  const ordererName = makeUniqueLabel("P8 주문자");

  await loginAdmin(page);

  const createCard = adminCard(page, "상품 등록");
  await createCard.getByLabel("상품명").fill(productName);
  await createCard.getByLabel("유형").selectOption("READY_STOCK");
  await createCard.getByLabel("가격 (원)").fill("25000");
  await createCard.getByLabel("수량").fill("5");
  await createCard.getByRole("button", { name: "상품 등록" }).click();

  const product = await waitForProduct(request, productName);

  await page.goto("/orders/new");
  await completePhoneVerification(page, phone);
  await page.getByLabel("주문자 이름").fill(ordererName);
  await page.getByLabel("상품").selectOption(String(product.id));
  await page.getByLabel("수량").fill("1");
  await page.getByRole("button", { name: "추가" }).click();
  await page.getByRole("button", { name: "주문하기" }).click();

  await expect(successAlert(page, "주문이 완료되었습니다!")).toBeVisible();
  const orderCardText = await page.locator(".card").last().textContent();
  if (!orderCardText) {
    throw new Error("Order success card text was empty");
  }
  const orderId = extractFirstNumber(orderCardText, "주문 #");
  const orderToken = await extractCodeText(page);
  const approvalPendingOrder = await waitForOrder(request, orderId, "PAID_APPROVAL_PENDING");

  await page.goto("/admin");
  const orderCard = adminCard(page, "주문 목록");
  await orderCard.getByLabel("상태").selectOption("PAID_APPROVAL_PENDING");
  let row = orderCard.locator("tbody tr").filter({ hasText: approvalPendingOrder.orderNumber }).first();
  await expect(row).toBeVisible();
  await row.getByRole("button", { name: "승인" }).click();

  await waitForOrder(request, orderId, "APPROVED_FULFILLMENT_PENDING");
  await orderCard.getByLabel("상태").selectOption("APPROVED_FULFILLMENT_PENDING");
  row = orderCard.locator("tbody tr").filter({ hasText: approvalPendingOrder.orderNumber }).first();
  await expect(row).toBeVisible();
  await row.locator('input[type="datetime-local"]').fill(toDateTimeLocalInput(plusDays(7, 18, 0, 30).start));
  await row.getByRole("button", { name: "픽업 준비" }).click();

  await waitForOrder(request, orderId, "PICKUP_READY");
  await orderCard.getByLabel("상태").selectOption("PICKUP_READY");
  row = orderCard.locator("tbody tr").filter({ hasText: approvalPendingOrder.orderNumber }).first();
  await expect(row).toBeVisible();
  await row.getByRole("button", { name: "픽업 완료" }).click();

  await waitForOrder(request, orderId, "PICKED_UP");

  await page.goto("/orders/detail");
  await page.getByLabel("주문 ID").fill(String(orderId));
  await page.getByLabel("인증 토큰").fill(orderToken);
  await page.getByRole("button", { name: "조회" }).click();

  await expect(page.locator(".badge-status").filter({ hasText: "수령 완료" }).first()).toBeVisible();
  await expect(page.getByText("이행 정보")).toBeVisible();
});

test("P8-5 환불 실패 주문을 관리자 화면에서 재시도해 복구할 수 있다", async ({ page, request }) => {
  const productName = makeUniqueLabel("P8-환불상품");
  const phone = makePhoneNumber(makeUniqueLabel("p8-refund"));
  const ordererName = makeUniqueLabel("P8 환불 주문자");
  const failureReason = makeUniqueLabel("P8-환불실패");

  await clearNextRefundFailure(request);

  try {
    await loginAdmin(page);

    const createCard = adminCard(page, "상품 등록");
    await createCard.getByLabel("상품명").fill(productName);
    await createCard.getByLabel("유형").selectOption("READY_STOCK");
    await createCard.getByLabel("가격 (원)").fill("27000");
    await createCard.getByLabel("수량").fill("3");
    await createCard.getByRole("button", { name: "상품 등록" }).click();

    const product = await waitForProduct(request, productName);

    await page.goto("/orders/new");
    await completePhoneVerification(page, phone);
    await page.getByLabel("주문자 이름").fill(ordererName);
    await page.getByLabel("상품").selectOption(String(product.id));
    await page.getByLabel("수량").fill("1");
    await page.getByRole("button", { name: "추가" }).click();
    await page.getByRole("button", { name: "주문하기" }).click();

    await expect(successAlert(page, "주문이 완료되었습니다!")).toBeVisible();
    const orderCardText = await page.locator(".card").last().textContent();
    if (!orderCardText) {
      throw new Error("Order success card text was empty");
    }
    const orderId = extractFirstNumber(orderCardText, "주문 #");
    const approvalPendingOrder = await waitForOrder(request, orderId, "PAID_APPROVAL_PENDING");

    await armNextRefundFailure(request, failureReason);

    await page.goto("/admin");
    const orderCard = adminCard(page, "주문 목록");
    await orderCard.getByLabel("상태").selectOption("PAID_APPROVAL_PENDING");
    const orderRow = orderCard.locator("tbody tr").filter({ hasText: approvalPendingOrder.orderNumber }).first();
    await expect(orderRow).toBeVisible();
    await orderRow.getByRole("button", { name: "거절" }).click();

    await waitForOrder(request, orderId, "REJECTED_REFUNDED");
    const failedRefund = await waitForFailedRefundByOrderId(request, orderId);
    expect(failedRefund.failReason).toContain(failureReason);

    await page.reload();
    const refundCard = adminCard(page, "환불 실패 목록");
    const refundRow = refundCard.locator("tbody tr").filter({ hasText: failureReason }).first();
    await expect(refundRow).toBeVisible();
    await refundRow.getByRole("button", { name: "재시도" }).click();

    await waitForFailedRefundGone(request, failedRefund.refundId);
    await expect(refundCard.locator("tbody tr").filter({ hasText: failureReason })).toHaveCount(0);
  } finally {
    await clearNextRefundFailure(request);
  }
});
