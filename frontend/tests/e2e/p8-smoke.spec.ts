import { expect, test, type Page } from "@playwright/test";
import {
  adminCard,
  armNextRefundFailure,
  clearNextRefundFailure,
  completeGuestAuthGate,
  completeLockedPhoneVerification,
  completePhoneVerification,
  extractFirstNumber,
  fetchClasses,
  loginCustomer,
  loginAdmin,
  logoutCustomer,
  makePhoneNumber,
  makeUniqueLabel,
  formatTimeTokenForUi,
  plusDays,
  signupCustomer,
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
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(bookingDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(firstSlot.startAt) }).first().click();
  await page.getByLabel("예약금 (원)").fill("30000");
  await page.getByRole("button", { name: /예약하기/ }).click();
  await completeGuestAuthGate(page, phone, guestName);

  await expect(successAlert(page, "예약이 완료되었습니다!")).toBeVisible();
  const bookingToken = await extractCodeText(page);
  const booking = await waitForBookingByPhone(request, bookingDate, phone);

  await page.goto("/guest/bookings");
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

  await page.goto(`/products/${product.id}`);
  await page.getByRole("spinbutton", { name: "수량" }).fill("2");
  await page.getByRole("button", { name: "비회원 주문하기" }).click();

  await expect(page).toHaveURL(new RegExp(`/orders/new\\?productId=${product.id}&qty=2$`));
  await expect(page.getByText("상품 상세에서 선택한 상품과 수량을 미리 담아두었습니다.")).toBeVisible();

  await completePhoneVerification(page, phone);
  await page.getByLabel("주문자 이름").fill(ordererName);
  const prefilledItem = page.locator(".list-group-item").filter({ hasText: productName }).first();
  await expect(prefilledItem).toBeVisible();
  await expect(prefilledItem).toContainText("x2");
  await page.getByRole("button", { name: "주문하기", exact: true }).click();

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

  await page.goto("/guest/orders");
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

    await waitForOrder(request, orderId, "REJECTED");
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

test("P8-6 회원 가입 후 상품 상세에서 주문하고 내 주문 상세를 확인할 수 있다", async ({ page, request }) => {
  const productName = makeUniqueLabel("P8-회원주문");

  await loginAdmin(page);

  const createCard = adminCard(page, "상품 등록");
  await createCard.getByLabel("상품명").fill(productName);
  await createCard.getByLabel("유형").selectOption("READY_STOCK");
  await createCard.getByLabel("가격 (원)").fill("33000");
  await createCard.getByLabel("수량").fill("5");
  await createCard.getByRole("button", { name: "상품 등록" }).click();

  const product = await waitForProduct(request, productName);
  const customer = await signupCustomer(page, "p8-member-order");

  await page.goto(`/products/${product.id}`);
  await page.getByRole("spinbutton", { name: "수량" }).fill("2");
  await page.getByRole("button", { name: "구매하기" }).click();

  await expect(page).toHaveURL(/\/my\/orders\/\d+$/);
  await expect(page.getByRole("heading", { name: "주문 상품" })).toBeVisible();
  await expect(page.getByRole("cell", { name: String(product.id) })).toBeVisible();
  await expect(page.getByRole("cell", { name: "₩66,000" })).toBeVisible();

  await logoutCustomer(page);
  await page.goto("/my");
  await expect(page.getByText("로그인이 필요합니다")).toBeVisible();

  await loginCustomer(page, customer);
  await page.goto("/my");
  await expect(page.getByText("내 주문")).toBeVisible();
});

test("P8-7 회원은 8회권 구매와 예약 생성 후 내 정보에서 바로 확인할 수 있다", async ({ page, request }) => {
  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 member booking flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const firstSlotWindow = plusDays(6, 15, 5, bookingClass.durationMin);
  const secondSlotWindow = plusDays(6, 18, 20, bookingClass.durationMin);
  const slotDate = toDateInput(firstSlotWindow.start);

  await loginAdmin(page);
  const slotCreateCard = adminCard(page, "슬롯 생성");
  await slotCreateCard.getByLabel("클래스").selectOption(String(bookingClass.id));
  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(firstSlotWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(firstSlotWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();
  const slot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(firstSlotWindow.start));

  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(secondSlotWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(secondSlotWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();
  const secondSlot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(secondSlotWindow.start));

  await signupCustomer(page, "p8-member-booking");

  await page.goto("/passes/purchase");
  await page.getByLabel("결제 금액 (원)").fill("120000");
  await page.getByRole("button", { name: /8회권 구매/ }).click();
  await expect(page.getByRole("button", { name: "내 8회권 확인하기" })).toBeVisible();
  await page.getByRole("button", { name: "내 8회권 확인하기" }).click();
  await expect(page.getByText("내 8회권")).toBeVisible();
  await expect(page.getByText("잔여")).toBeVisible();

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(slotDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(slot.startAt) }).first().click();
  await page.getByLabel("예약금 (원)").fill("30000");
  await page.getByRole("button", { name: /예약하기/ }).click();
  await expect(page.getByRole("button", { name: "내 예약 상세 보기" })).toBeVisible();
  await page.getByRole("button", { name: "내 예약 상세 보기" }).click();

  await expect(page).toHaveURL(/\/my\/bookings\/\d+$/);
  await expect(page.getByText(bookingClass.name)).toBeVisible();
  await page.getByLabel("새 슬롯 ID").fill(String(secondSlot.id));
  await page.getByRole("button", { name: "예약 변경" }).click();
  await expect(page.getByText(`현재 슬롯: #${secondSlot.id}`)).toBeVisible();

  await page.getByRole("button", { name: "예약 취소" }).click();
  await page.getByRole("button", { name: "취소 확인" }).click();
  await expect(page.getByText("취소됨")).toBeVisible();
});

test("P8-8 회원은 같은 번호의 비회원 주문, 예약, 8회권을 claim 할 수 있다", async ({ page, request }) => {
  const productName = makeUniqueLabel("P8-claim-order");
  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 guest claim flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const slotWindow = plusDays(7, 13, 5, bookingClass.durationMin);
  const slotDate = toDateInput(slotWindow.start);
  const guestPhone = makePhoneNumber(makeUniqueLabel("p8-claim"));
  const guestName = makeUniqueLabel("P8 클레임");
  const memberPhone = `${guestPhone.slice(0, 3)}-${guestPhone.slice(3, 7)}-${guestPhone.slice(7)}`;

  await loginAdmin(page);

  const createCard = adminCard(page, "상품 등록");
  await createCard.getByLabel("상품명").fill(productName);
  await createCard.getByLabel("유형").selectOption("READY_STOCK");
  await createCard.getByLabel("가격 (원)").fill("31000");
  await createCard.getByLabel("수량").fill("5");
  await createCard.getByRole("button", { name: "상품 등록" }).click();
  const product = await waitForProduct(request, productName);

  const slotCreateCard = adminCard(page, "슬롯 생성");
  await slotCreateCard.getByLabel("클래스").selectOption(String(bookingClass.id));
  await slotCreateCard.getByLabel("시작 시각").fill(toDateTimeLocalInput(slotWindow.start));
  await slotCreateCard.getByLabel("종료 시각").fill(toDateTimeLocalInput(slotWindow.end));
  await slotCreateCard.getByRole("button", { name: "슬롯 생성" }).click();
  const slot = await waitForSlot(request, bookingClass.id, toDateTimeLocalInput(slotWindow.start));

  await page.goto("/orders/new");
  await completePhoneVerification(page, guestPhone);
  await page.getByLabel("주문자 이름").fill(guestName);
  await page.getByLabel("상품").selectOption(String(product.id));
  await page.getByLabel("수량").fill("1");
  await page.getByRole("button", { name: "추가" }).click();
  await page.getByRole("button", { name: "주문하기", exact: true }).click();
  await expect(successAlert(page, "주문이 완료되었습니다!")).toBeVisible();
  const orderCardText = await page.locator(".card").last().textContent();
  if (!orderCardText) {
    throw new Error("Order success card text was empty");
  }
  const orderId = extractFirstNumber(orderCardText, "주문 #");

  await page.goto("/passes/purchase");
  await page.getByLabel("결제 금액 (원)").fill("120000");
  await page.getByRole("button", { name: /8회권 구매/ }).click();
  await completeGuestAuthGate(page, guestPhone, guestName);
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
  await page.getByRole("button", { name: /예약하기/ }).click();
  await completeGuestAuthGate(page, guestPhone, guestName);
  await expect(successAlert(page, "예약이 완료되었습니다!")).toBeVisible();
  const booking = await waitForBookingByPhone(request, slotDate, guestPhone);

  await signupCustomer(page, "p8-member-claim", { phone: memberPhone });
  await page.goto("/my");
  await expect(page.getByText("휴대폰 재확인 필요")).toBeVisible();

  await page.getByRole("button", { name: "이력 가져오기" }).click();
  const claimDialog = page.getByRole("dialog").filter({ hasText: "비회원 이력 가져오기" }).first();
  await expect(claimDialog).toBeVisible();
  await completeLockedPhoneVerification(claimDialog, "인증하고 불러오기");

  await expect(claimDialog.getByText("확인 완료")).toBeVisible();
  await expect(claimDialog.getByText(`주문 #${orderId}`)).toBeVisible();
  await expect(claimDialog.getByText(`${bookingClass.name} #${booking.bookingId}`)).toBeVisible();
  await expect(claimDialog.getByText(`8회권 #${passId}`)).toBeVisible();

  await claimDialog.locator(`#claim-pass-${passId}`).uncheck();
  await claimDialog.getByRole("button", { name: "선택한 이력 가져오기" }).click();

  await expect(page.getByText("휴대폰 인증 완료")).toBeVisible();
  await expect(page.getByText(String(orderId))).toBeVisible();
  await expect(page.getByText(bookingClass.name)).toBeVisible();
  await expect(page.getByText(/잔여\s+\d+\/8회/)).toBeVisible();
});
