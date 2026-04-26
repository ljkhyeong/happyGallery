import { expect, test } from "@playwright/test";
import {
  adminCard,
  completeGuestAuthGate,
  completeLockedPhoneVerification,
  completePhoneVerification,
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
  waitForProduct,
  waitForSlot,
} from "./support";

test("P8-8 회원은 같은 번호의 비회원 주문과 예약을 claim 할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const productName = makeUniqueLabel("P8-claim-order");
  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 guest claim flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const slotWindow = await findUniqueSlotWindow(request, bookingClass.id, 7, 13, 5, bookingClass.durationMin);
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
  await page.getByRole("button", { name: "비회원 다중 상품 주문 계속" }).click();
  await completePhoneVerification(page, guestPhone);
  await page.getByLabel("주문자 이름").fill(guestName);
  await page.getByLabel("상품").selectOption(String(product.id));
  await page.getByLabel("수량").fill("1");
  await page.getByRole("button", { name: "추가" }).click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("button", { name: "비회원 주문 확인하기" }).click();
  const guestOrderState = await readRouterState<{ orderId: number; token: string }>(page);
  const orderId = guestOrderState?.orderId;
  if (!orderId) {
    throw new Error("Guest order id should be kept in router state");
  }

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(slotDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(slot.startAt) }).first().click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await completeGuestAuthGate(page, guestPhone, guestName);
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("button", { name: "비회원 예약 확인하기" }).click();
  const guestBookingState = await readRouterState<{ bookingId: number; token: string }>(page);
  const bookingId = guestBookingState?.bookingId;
  if (!bookingId) {
    throw new Error("Guest booking id should be kept in router state");
  }

  await signupCustomer(page, "p8-member-claim", { phone: memberPhone });
  await page.goto("/my");
  await expect(page.getByText("휴대폰 재확인 필요")).toBeVisible();

  await page.getByRole("button", { name: /가져오기/ }).first().click();
  const claimDialog = page.getByRole("dialog").filter({ hasText: "비회원 이력 가져오기" }).first();
  await expect(claimDialog).toBeVisible();
  await completeLockedPhoneVerification(claimDialog, page, guestPhone, "인증하고 불러오기");

  await expect(claimDialog.getByText("확인 완료")).toBeVisible();
  await expect(claimDialog.getByText(`주문 #${orderId}`)).toBeVisible();
  await expect(claimDialog.getByText(`${bookingClass.name} #${bookingId}`)).toBeVisible();
  await claimDialog.getByRole("button", { name: "선택한 이력 가져오기" }).click();

  await expect(page.getByText("휴대폰 인증 완료")).toBeVisible();
  await expect(page.getByText(`주문 #${orderId}`)).toBeVisible();
  await expect(page.getByText(bookingClass.name)).toBeVisible();
});

test("P8-9 비회원 주문 결제 후 조회 화면에서 회원 전환 안내를 볼 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const productName = makeUniqueLabel("P8-claim-signup");
  const guestPhone = makePhoneNumber(makeUniqueLabel("p8-claim-signup"));
  const guestName = makeUniqueLabel("P8 전환");

  await loginAdmin(page);

  const createCard = adminCard(page, "상품 등록");
  await createCard.getByLabel("상품명").fill(productName);
  await createCard.getByLabel("유형").selectOption("READY_STOCK");
  await createCard.getByLabel("가격 (원)").fill("29000");
  await createCard.getByLabel("수량").fill("5");
  await createCard.getByRole("button", { name: "상품 등록" }).click();
  const product = await waitForProduct(request, productName);

  await page.goto("/orders/new");
  await page.getByRole("button", { name: "비회원 다중 상품 주문 계속" }).click();
  await completePhoneVerification(page, guestPhone);
  await page.getByLabel("주문자 이름").fill(guestName);
  await page.getByLabel("상품").selectOption(String(product.id));
  await page.getByLabel("수량").fill("1");
  await page.getByRole("button", { name: "추가" }).click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();

  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("button", { name: "비회원 주문 확인하기" }).click();

  await expect(page.getByRole("heading", { name: "비회원 주문 조회" })).toBeVisible();
  await expect(page.locator(".card").filter({ hasText: /주문 #\d+/ }).last()).toBeVisible();
  await expect(page.getByRole("button", { name: "회원가입" })).toBeVisible();
  await expect(page.getByText("비회원 주문은 토큰으로 조회하고")).toBeVisible();
});
