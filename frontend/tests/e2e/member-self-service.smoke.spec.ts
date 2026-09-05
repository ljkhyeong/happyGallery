import { expect, test } from "@playwright/test";
import {
  createAdminProduct,
  extractFirstNumber,
  fetchClasses,
  fetchMyBookingSlot,
  findAvailableBookingSlot,
  installTossPaymentStub,
  loginCustomer,
  logoutCustomer,
  makeUniqueLabel,
  signupCustomer,
} from "./support";

const EMPTY_CART_VERSION = "0".repeat(64);

test("P8-6 @smoke @payment 회원 가입 후 상품 상세에서 주문하고 내 주문 상세를 확인할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);
  let interruptedConfirm = false;
  await page.route(/\/api\/v1\/payments\/confirm$/, async (route) => {
    if (!interruptedConfirm) {
      interruptedConfirm = true;
      await route.abort("failed");
      return;
    }
    await route.continue();
  });

  const productName = makeUniqueLabel("P8-회원주문");
  const product = await createAdminProduct(request, {
    name: productName,
    price: 33000,
    quantity: 5,
  });
  const customer = await signupCustomer(page, "p8-member-order");

  await page.goto(`/products/${product.id}`);
  await page.getByRole("button", { name: "매장 수령" }).click();
  await expect(page.getByText("준비 완료 알림을 받은 뒤 매장에서 수령합니다.")).toBeVisible();
  const quantityInput = page.getByRole("spinbutton", { name: "수량" });
  await quantityInput.fill("2");
  await expect(quantityInput).toHaveValue("2");
  await page.getByRole("button", { name: "바로 구매하기" }).click();

  await expect(page.getByText("결제 결과를 다시 확인해 주세요")).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await expect.poll(() => page.evaluate(() =>
    sessionStorage.getItem("hg_payment_confirm_request"),
  )).toBeNull();
  await page.getByRole("link", { name: "내 주문 상세 보기" }).click();
  await expect(page).toHaveURL(/\/my\/orders\/\d+$/);
  const orderId = Number(page.url().match(/\/my\/orders\/(\d+)$/)?.[1]);
  await expect(page.getByRole("heading", { name: "주문 상품" })).toBeVisible();
  const itemRow = page.getByRole("row").filter({ hasText: productName });
  await expect(itemRow).toContainText("2");
  await expect(itemRow).toContainText("₩66,000");

  await logoutCustomer(page);
  await page.goto("/my");
  await expect(page.getByText("로그인하고 주문, 예약, 8회권을 한 곳에서 관리하세요")).toBeVisible();

  await loginCustomer(page, customer);
  await page.goto("/my/orders");
  await page.getByLabel("상태", { exact: true }).selectOption("PAID_APPROVAL_PENDING");
  await page.getByLabel("주문 번호·상품명 검색").fill(String(orderId));
  await page.getByText(`주문 #${orderId}`).click();
  await expect(page).toHaveURL(new RegExp(`/my/orders/${orderId}$`));

  await page.getByRole("button", { name: "주문 취소" }).click();
  const refundDialog = page.getByRole("dialog", { name: "주문 취소 및 환불 요청" });
  await expect(refundDialog).toBeVisible();
  await expect(refundDialog.getByText(
    "주문 취소는 바로 반영되며, 실제 환불 완료 시점은 결제사에 따라 달라질 수 있습니다.",
  )).toBeVisible();
  await refundDialog.getByRole("button", { name: "취소하고 환불 요청" }).click();
  await expect(page.locator(".badge-status").filter({ hasText: "고객 취소" }).first())
    .toBeVisible();
});

test("P8-7 @payment 회원은 8회권 구매와 예약 생성 후 내 정보에서 바로 확인할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const classes = await fetchClasses(request);
  expect(classes.length, "P8 member booking flow requires at least one class in the local DB")
    .toBeGreaterThan(0);
  const bookingClass = classes[0]!;

  const slot = await findAvailableBookingSlot(request, bookingClass.id, 6);
  const secondSlot = await findAvailableBookingSlot(
    request,
    bookingClass.id,
    7,
    new Set([slot.id]),
  );
  const slotDate = slot.startAt.slice(0, 10);

  await signupCustomer(page, "p8-member-booking");

  await page.goto("/passes/purchase");
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await expect(page.getByRole("link", { name: "내 8회권 확인하기" })).toBeVisible();
  await page.getByRole("link", { name: "내 8회권 확인하기" }).click();
  await expect(page).toHaveURL(/\/my\/passes$/);
  await expect(page.getByText("전체 8회권")).toBeVisible();
  const passCardText = await page.locator(".my-list-card").first().textContent();
  if (!passCardText) {
    throw new Error("Member pass list text was empty");
  }
  const passId = extractFirstNumber(passCardText, "8회권 #");
  await page.getByLabel("상태", { exact: true }).selectOption("사용 가능");
  await page.getByLabel("8회권 번호 검색").fill(String(passId));
  await expect(page.getByText(`8회권 #${passId}`)).toBeVisible();

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").selectOption(slotDate);
  await page.locator(`[data-slot-id="${slot.id}"]`).click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await expect(page.getByRole("link", { name: "내 예약 상세 보기" })).toBeVisible();
  await page.getByRole("link", { name: "내 예약 상세 보기" }).click();

  await expect(page).toHaveURL(/\/my\/bookings\/\d+$/);
  const bookingId = Number(page.url().match(/\/my\/bookings\/(\d+)$/)?.[1]);
  const booked = await fetchMyBookingSlot(page, bookingId);
  const targetSlot = booked.slotId === slot.id ? secondSlot : slot;
  const targetDate = targetSlot.id === slot.id
    ? slot.startAt.slice(0, 10)
    : secondSlot.startAt.slice(0, 10);
  expect([slot.id, secondSlot.id]).toContain(booked.slotId);

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
    await fetchMyBookingSlot(page, bookingId)
  ).slotId).toBe(targetSlot.id);

  await page.getByRole("button", { name: "예약 취소" }).click();
  const cancelDialog = page.getByRole("dialog", { name: "예약 취소 및 환불 안내" });
  await expect(cancelDialog.getByText("사용한 8회권 1회를 돌려드립니다.")).toBeVisible();
  await cancelDialog.getByRole("button", { name: "예약 취소 및 1회 복원" }).click();
  await expect(page.getByText("취소됨")).toBeVisible();

  await page.goto("/my/bookings");
  await page.getByLabel("상태", { exact: true }).selectOption("취소됨");
  await page.getByLabel("예약 검색").fill(String(bookingId));
  await expect(page.getByText(bookingClass.name)).toBeVisible();
});

test("P8-10 @payment 취소 마감 후 8회권 미복구와 예약금 환불 불가를 구분해 안내한다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const bookingId = 990001;
  let canceled = false;
  let passBooking = true;
  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "pass-cancel-warning-token",
    url: baseURL,
  }]);
  await page.setViewportSize({ width: 390, height: 844 });

  await page.route(/\/api\/v1\/me\/cart$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 }),
    });
  });

  await page.route(/\/api\/v1\/me\/notifications\/unread-count$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0 }),
    });
  });

  await page.route(/\/api\/v1\/slots(?:\?.*)?$/, async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "[]" });
  });

  await page.route(/\/api\/v1\/me$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        id: 101,
        email: "pass-warning@example.com",
        name: "8회권 회원",
        phone: "01012345678",
        phoneVerified: true,
      }),
    });
  });

  await page.route(new RegExp(`/api/v1/me/bookings/${bookingId}$`), async (route) => {
    if (route.request().method() === "DELETE") {
      canceled = true;
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          bookingId,
          status: "CANCELED",
          participantCount: 1,
          refundable: false,
          refundAmount: 0,
          refund: null,
          manualCompensationRequired: false,
        }),
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        bookingId,
        classId: 1,
        slotId: 88,
        status: canceled ? "CANCELED" : "BOOKED",
        className: passBooking ? "8회권 취소 경고 클래스" : "당일 예약금 취소 경고 클래스",
        startAt: "2026-07-12T18:00:00",
        endAt: "2026-07-12T19:00:00",
        participantCount: 1,
        depositAmount: passBooking ? 0 : 15000,
        balanceAmount: 0,
        balanceStatus: "UNPAID",
        passBooking,
        cancelPolicy: {
          cancellable: true,
          refundable: false,
          deadlineAt: "2026-07-12T00:00:00",
          passCreditRestorable: false,
          manualCompensationRequired: false,
          warningCode: passBooking
            ? "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE"
            : null,
        },
        refund: null,
      }),
    });
  });

  await page.goto(`/my/bookings/${bookingId}`);
  await expect(page.getByText("8회권 취소 경고 클래스")).toBeVisible();
  await expect(page.getByText("1명", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "예약 취소" }).click();

  await expect(page.getByText(
    "취소 마감이 지나 사용한 8회권 1회는 돌려드리지 않습니다.",
  )).toBeVisible();
  await expect(page.getByText("D-1(전날 00:00) 이후에는 예약금 환불이 불가합니다.")).toHaveCount(0);

  await page.getByRole("button", { name: "1회 차감 유지하고 취소" }).click();
  await expect(page.getByText(
    "예약이 취소되었습니다. 사용한 8회권 1회는 돌려드리지 않습니다.",
  )).toBeVisible();

  passBooking = false;
  canceled = false;
  await page.goto(`/my/bookings/${bookingId}`);
  await expect(page.getByText("당일 예약금 취소 경고 클래스")).toBeVisible();

  await page.getByRole("button", { name: "예약 취소" }).click();

  const depositCancelDialog = page.getByRole("dialog", { name: "예약 취소 및 환불 안내" });
  await expect(depositCancelDialog.getByText("환불·이용 횟수 복원 마감")).toBeVisible();
  await expect(depositCancelDialog.getByText("₩15,000", { exact: true })).toBeVisible();
  await expect(depositCancelDialog.getByText(
    "취소 마감이 지나 예약금 ₩15,000은 환불되지 않습니다. 예약만 취소됩니다.",
  )).toBeVisible();
  await expect(depositCancelDialog.getByRole("button", { name: "환불 없이 예약 취소" }))
    .toBeVisible();
});
