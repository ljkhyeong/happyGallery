import { expect, test } from "@playwright/test";
import {
  adminCard,
  completeGuestAuthGate,
  completeLockedPhoneVerification,
  completePhoneVerification,
  extractFirstNumber,
  fetchClasses,
  findUniqueSlotWindow,
  formatTimeTokenForUi,
  loginAdmin,
  makePhoneNumber,
  makeUniqueLabel,
  signupCustomer,
  toDateInput,
  toDateTimeLocalInput,
  waitForBookingByPhone,
  waitForProduct,
  waitForSlot,
} from "./support";
import { navLoginLink, navLogoutButton, successAlert } from "./ui-support";

test("P8-8 회원은 같은 번호의 비회원 주문, 예약, 8회권을 claim 할 수 있다", async ({ page, request }) => {
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

  await page.getByRole("button", { name: /가져오기/ }).first().click();
  const claimDialog = page.getByRole("dialog").filter({ hasText: "비회원 이력 가져오기" }).first();
  await expect(claimDialog).toBeVisible();
  await completeLockedPhoneVerification(claimDialog, page, guestPhone, "인증하고 불러오기");

  await expect(claimDialog.getByText("확인 완료")).toBeVisible();
  await expect(claimDialog.getByText(`주문 #${orderId}`)).toBeVisible();
  await expect(claimDialog.getByText(`${bookingClass.name} #${booking.bookingId}`)).toBeVisible();
  await expect(claimDialog.getByText(`8회권 #${passId}`)).toBeVisible();

  await claimDialog.locator(`#claim-pass-${passId}`).uncheck();
  await claimDialog.getByRole("button", { name: "선택한 이력 가져오기" }).click();

  await expect(page.getByText("휴대폰 인증 완료")).toBeVisible();
  await expect(page.getByText(`주문 #${orderId}`)).toBeVisible();
  await expect(page.getByText(bookingClass.name)).toBeVisible();
  await expect(page.getByText(/잔여\s+\d+\/8회/)).toBeVisible();
});

test("P8-9 비회원 성공 화면에서 회원가입 후 claim 모달을 바로 열 수 있다", async ({ page, request }) => {
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
  await page.getByRole("button", { name: "주문하기", exact: true }).click();

  await expect(successAlert(page, "주문이 완료되었습니다!")).toBeVisible();
  await page.getByRole("link", { name: "회원가입하고 내 정보로 가져오기" }).click();

  await expect(page).toHaveURL(/\/signup\?/);
  await expect(page.getByLabel("전화번호")).toHaveValue(guestPhone);
  await expect(page.getByLabel("이름")).toHaveValue(guestName);
  await expect(page.getByText("같은 휴대폰 번호로 가입하면")).toBeVisible();

  const signupSeed = makeUniqueLabel("p8-success-claim");
  await page.getByLabel("이메일").fill(`${signupSeed}@example.com`);
  await page.getByLabel("비밀번호").fill("password123");
  await page.getByRole("button", { name: "회원가입" }).click();

  await expect(page).toHaveURL(/\/my$/);
  await expect(navLogoutButton(page)).toBeVisible();
  const claimDialog = page.getByRole("dialog").filter({ hasText: "비회원 이력 가져오기" }).first();
  await expect(claimDialog).toBeVisible();
  await expect(claimDialog.getByText("휴대폰 재인증")).toBeVisible();

  await claimDialog.getByRole("button", { name: "닫기" }).click();
  await navLogoutButton(page).click();
  await expect(navLoginLink(page)).toBeVisible();

  await page.goto("/login?redirect=/my");
  await page.getByLabel("이메일").fill(`${signupSeed}@example.com`);
  await page.getByLabel("비밀번호").fill("password123");
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page).toHaveURL(/\/my$/);
  await expect(navLogoutButton(page)).toBeVisible();
});
