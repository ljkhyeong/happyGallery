import { expect, test, type Page } from "@playwright/test";
import type { MyBookingDetailResponse, OrderDetailResponse } from "../../src/shared/types";

const order = {
  orderId: 9090, orderNumber: "HG-NAV-9090", status: "DELIVERED",
  totalAmount: 12000, pgPaidAmount: 12000, productAmount: 12000,
  shippingFee: 0, couponDiscountAmount: 0, rewardUsedAmount: 0, rewardEarnBase: 12000,
  paidAt: "2026-08-01T12:00:00", approvalDeadlineAt: null, receiptUrl: null,
  fulfillment: null, issuedCouponId: null, couponStatus: null, refund: null, items: [],
} satisfies OrderDetailResponse;

const booking = {
  bookingId: 9090, classId: 1, slotId: 88, status: "CANCELED", className: "이력 검색 클래스",
  startAt: "2026-08-01T10:00:00", endAt: "2026-08-01T11:00:00", participantCount: 1,
  depositAmount: 10000, balanceAmount: 0, balanceStatus: "UNPAID", passBooking: false,
  receiptUrl: null, refund: null,
  cancelPolicy: {
    cancellable: false, refundable: false, deadlineAt: "2026-07-31T00:00:00",
    passCreditRestorable: false, manualCompensationRequired: false, warningCode: null,
  },
} satisfies MyBookingDetailResponse;

const scenarios = [
  {
    path: "orders", title: "주문", label: "주문 번호·상품명 검색", sort: "AMOUNT_DESC",
    link: /주문 #9090/, detailText: order.orderNumber, detail: order,
    summary: { ...order, createdAt: "2026-08-01T12:00:00" },
  },
  {
    path: "bookings", title: "예약", label: "예약 검색", sort: "LATEST",
    link: /이력 검색 클래스/, detailText: "예약 #9090", detail: booking, summary: booking,
  },
];

async function installMemberApi(page: Page, scenario: typeof scenarios[number]) {
  const state = { detailAvailable: true, detailRequests: 0, listRequests: [] as URL[] };
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({
      status, contentType: "application/json", body: JSON.stringify(body),
    });
    if (url.pathname === "/api/v1/me") {
      return json({ id: 501, name: "회원", email: "navigation@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    }
    if (url.pathname === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (url.pathname === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
    if (url.pathname === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (url.pathname === `/api/v1/me/${scenario.path}/page`) {
      state.listRequests.push(url);
      return json({ content: [scenario.summary], hasMore: false, nextCursor: null });
    }
    if (url.pathname === `/api/v1/me/${scenario.path}/9090`) {
      state.detailRequests += 1;
      return state.detailAvailable
        ? json(scenario.detail)
        : json({ code: "SERVICE_UNAVAILABLE", message: "일시적인 조회 실패" }, 503);
    }
    if (url.pathname.endsWith("/creation-state")) return json({ status: "NOT_REVIEWABLE" });
    return json([]);
  });
  return state;
}

for (const scenario of scenarios) {
  test(`${scenario.title} 상세를 새로고침한 뒤 목록으로 돌아와도 검색어·상태·정렬을 유지한다 @identity`, async ({ page }) => {
    const api = await installMemberApi(page, scenario);
    await page.goto(`/my/${scenario.path}`);
    await page.getByLabel(scenario.label, { exact: true }).fill("9090");
    await page.getByLabel("정렬", { exact: true }).selectOption(scenario.sort);
    await page.getByLabel("상태", { exact: true }).selectOption(scenario.detail.status);
    await expect.poll(() => api.listRequests.at(-1)?.searchParams.get("keyword")).toBe("9090");
    const listUrl = page.url();

    await page.getByRole("link", { name: scenario.link }).click();
    await expect(page.getByText(scenario.detailText, { exact: true })).toBeVisible();
    await page.reload();
    await expect(page.getByText(scenario.detailText, { exact: true })).toBeVisible();
    await page.getByRole("link", { name: `내 ${scenario.title}`, exact: false }).click();

    await expect(page).toHaveURL(listUrl);
    await expect(page.getByLabel(scenario.label, { exact: true })).toHaveValue("9090");
    await expect(page.getByLabel("상태", { exact: true })).toHaveValue(scenario.detail.status);
    await expect(page.getByLabel("정렬", { exact: true })).toHaveValue(scenario.sort);
    await expect.poll(() => api.listRequests.at(-1)?.searchParams.get("status")).toBe(scenario.detail.status);
    expect(api.listRequests.at(-1)?.searchParams.get("sort")).toBe(scenario.sort);
  });

  test(`${scenario.title} 상세 조회 실패 후 새로고침 없이 재시도하고 기본 목록으로 돌아간다 @identity`, async ({ page }) => {
    const api = await installMemberApi(page, scenario);
    api.detailAvailable = false;
    await page.goto(`/my/${scenario.path}/9090`);
    const retry = page.getByRole("button", { name: "다시 시도", exact: true });
    await expect(retry).toBeVisible();
    const backLink = page.getByRole("link", { name: `내 ${scenario.title}`, exact: false });
    await expect(backLink).toHaveAttribute("href", `/my/${scenario.path}`);
    const failedRequests = api.detailRequests;

    api.detailAvailable = true;
    await retry.click();
    await expect(page.getByText(scenario.detailText, { exact: true })).toBeVisible();
    await expect(retry).toHaveCount(0);
    expect(api.detailRequests).toBe(failedRequests + 1);
    await backLink.click();
    await expect(page).toHaveURL(new RegExp(`/my/${scenario.path}$`));
    await expect(page.getByLabel(scenario.label, { exact: true })).toHaveValue("");
  });
}
