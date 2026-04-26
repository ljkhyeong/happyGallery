import { expect, test } from "@playwright/test";
import {
  adminCard,
  extractFirstNumber,
  fetchClasses,
  findUniqueSlotWindow,
  formatTimeTokenForUi,
  installTossPaymentStub,
  loginAdmin,
  loginCustomer,
  logoutCustomer,
  makeUniqueLabel,
  signupCustomer,
  toDateInput,
  toDateTimeLocalInput,
  waitForProduct,
  waitForSlot,
} from "./support";

test("P8-6 회원 가입 후 상품 상세에서 주문하고 내 주문 상세를 확인할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

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
  await page.getByRole("button", { name: "BUY NOW" }).click();

  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await page.getByRole("button", { name: "내 주문 상세 보기" }).click();
  await expect(page).toHaveURL(/\/my\/orders\/\d+$/);
  const orderId = Number(page.url().match(/\/my\/orders\/(\d+)$/)?.[1]);
  await expect(page.getByRole("heading", { name: "주문 상품" })).toBeVisible();
  await expect(page.getByRole("cell", { name: String(product.id) })).toBeVisible();
  await expect(page.getByRole("cell", { name: "₩66,000" })).toBeVisible();

  await logoutCustomer(page);
  await page.goto("/my");
  await expect(page.getByText("로그인하고 주문, 예약, 8회권을 한 곳에서 관리하세요")).toBeVisible();

  await loginCustomer(page, customer);
  await page.goto("/my/orders");
  await page.getByLabel("상태").selectOption("승인 대기");
  await page.getByLabel("주문 번호 검색").fill(String(orderId));
  await expect(page.getByText(`주문 #${orderId}`)).toBeVisible();
});

test("P8-7 회원은 8회권 구매와 예약 생성 후 내 정보에서 바로 확인할 수 있다", async ({ page, request }) => {
  await installTossPaymentStub(page);

  const classes = await fetchClasses(request);
  test.skip(classes.length === 0, "P8 member booking flow requires at least one class in the local DB");
  const bookingClass = classes[0]!;

  const firstSlotWindow = await findUniqueSlotWindow(request, bookingClass.id, 6, 15, 5, bookingClass.durationMin);
  const secondSlotWindow = await findUniqueSlotWindow(request, bookingClass.id, 6, 18, 20, bookingClass.durationMin);
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
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await expect(page.getByRole("button", { name: "내 8회권 확인하기" })).toBeVisible();
  await page.getByRole("button", { name: "내 8회권 확인하기" }).click();
  await expect(page).toHaveURL(/\/my\/passes$/);
  await expect(page.getByText("전체 8회권")).toBeVisible();
  const passCardText = await page.locator(".my-list-card").first().textContent();
  if (!passCardText) {
    throw new Error("Member pass list text was empty");
  }
  const passId = extractFirstNumber(passCardText, "8회권 #");
  await page.getByLabel("상태").selectOption("사용 가능");
  await page.getByLabel("8회권 번호 검색").fill(String(passId));
  await expect(page.getByText(`8회권 #${passId}`)).toBeVisible();

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption(String(bookingClass.id));
  await page.getByLabel("날짜").fill(slotDate);
  await page.locator(".list-group-item").filter({ hasText: formatTimeTokenForUi(slot.startAt) }).first().click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect(page.getByRole("heading", { name: "결제 완료" })).toBeVisible();
  await expect(page.getByRole("button", { name: "내 예약 상세 보기" })).toBeVisible();
  await page.getByRole("button", { name: "내 예약 상세 보기" }).click();

  await expect(page).toHaveURL(/\/my\/bookings\/\d+$/);
  const bookingId = Number(page.url().match(/\/my\/bookings\/(\d+)$/)?.[1]);
  await expect(page.getByText(bookingClass.name)).toBeVisible();
  await page.getByLabel("새 슬롯 ID").fill(String(secondSlot.id));
  await page.getByRole("button", { name: "예약 변경" }).click();
  await expect(page.getByText(`현재 슬롯: #${secondSlot.id}`)).toBeVisible();

  await page.getByRole("button", { name: "예약 취소" }).click();
  await page.getByRole("button", { name: "취소 확인" }).click();
  await expect(page.getByText("취소됨")).toBeVisible();

  await page.goto("/my/bookings");
  await page.getByLabel("상태").selectOption("취소됨");
  await page.getByLabel("예약 검색").fill(String(bookingId));
  await expect(page.getByText(bookingClass.name)).toBeVisible();
});
