import { expect, test } from "@playwright/test";
import {
  adminCard,
  armNextRefundFailure,
  clearNextRefundFailure,
  completePhoneVerification,
  extractFirstNumber,
  loginAdmin,
  makePhoneNumber,
  makeUniqueLabel,
  plusDays,
  toDateTimeLocalInput,
  waitForFailedRefundByOrderId,
  waitForFailedRefundGone,
  waitForOrder,
  waitForProduct,
} from "./support";
import { extractCodeText, successAlert } from "./ui-support";

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

  await page.goto("/guest");
  await page.getByRole("link", { name: "비회원 주문 조회" }).click();
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
    await page.getByRole("button", { name: "비회원 다중 상품 주문 계속" }).click();
    await completePhoneVerification(page, phone);
    await page.getByLabel("주문자 이름").fill(ordererName);
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
