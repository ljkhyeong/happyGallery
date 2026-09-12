import { expect, test, type Page, type Route } from "@playwright/test";
import type { BookingCalendarResponse, RestockDemandPageResponse, SmartStoreInventoryMappingResponse, SmartStoreNoticeResponse } from "../../src/generated/api/adminCatalog";
import type { SmartStoreChannelOrderDetailResponse, SmartStoreChannelOrderResponse } from "../../src/generated/api/adminOrder";
import type {
  GroupInquiryFollowUpPageResponse,
  GroupInquiryPageResponse,
  SmartStoreAccountingReportResponse,
} from "../../src/generated/api/adminOperations";

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

function inquiryPage(content: unknown[], page = 0, totalCount = content.length) {
  return { content, page, size: 50, totalCount, totalPages: Math.ceil(totalCount / 50) };
}

async function prepareAdmin(page: Page) {
  await page.addInitScript(() => sessionStorage.setItem("hg_admin_token", "smartstore-admin-test"));
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/me") return json(route, {}, 401);
    if (pathname === "/api/v1/admin/group-inquiries") {
      return json(route, {
        content: [], hasMore: false, nextCursor: null,
      } satisfies GroupInquiryPageResponse);
    }
    if (pathname === "/api/v1/admin/group-inquiries/follow-ups") {
      return json(route, {
        content: [], hasMore: false, nextCursor: null,
      } satisfies GroupInquiryFollowUpPageResponse);
    }
    if (pathname === "/api/v1/admin/restock-demand") {
      return json(route, {
        content: [], page: 0, size: 20, totalCount: 0, totalPages: 0,
      } satisfies RestockDemandPageResponse);
    }
    if (pathname.includes("smartstore-inquiries") && pathname.endsWith("/page")) {
      return json(route, inquiryPage([]));
    }
    if (pathname === "/api/v1/admin/smartstore-notices") {
      return json(route, { notices: [], page: 1, size: 100, totalElements: 0, totalPages: 0 });
    }
    if (pathname === "/api/v1/admin/products/smartstore-inspections") {
      return json(route, { products: [], page: 1, size: 100, totalElements: 0, totalPages: 0 });
    }
    if (pathname === "/api/v1/admin/smartstore-settlements/accounting") {
      return json(route, {
        from: "2026-08-01", to: "2026-08-30", vatAvailableThrough: "2026-07-31",
        dailySettlements: [], commissionDetails: [], dailyVat: [],
      });
    }
    if (pathname === "/api/v1/admin/orders" || pathname.includes("/page") || pathname.endsWith("/unanswered")) {
      return json(route, { content: [], hasMore: false, nextCursor: null });
    }
    if (pathname === "/api/v1/workshop") return json(route, { name: "해피갤러리", version: 1 });
    return json(route, []);
  });
}

async function prepareSmartStoreOrderEditors(page: Page) {
  await prepareAdmin(page);
  const state = { listUnavailable: false, listReads: 0 };
  const orders = ["po-editor-1", "po-editor-2"].map((id) => ({
    productOrderId: id, orderId: `order-${id}`, originProductNo: 1, itemNo: null,
    productId: 1, productVariantId: null, productName: id, productOption: null,
    productOrderStatus: "PAYED", claimType: null, claimStatus: null,
    initialQuantity: 1, remainQuantity: 1, inventoryAppliedQuantity: 1,
    attentionReason: null, paymentDate: null, lastChangedAt: "2026-09-12T09:00:00",
    pendingReturnQuantity: 0, returnReviewVersion: "R0:0", inventoryResolutionVersion: "v1",
  } satisfies SmartStoreChannelOrderResponse));
  await page.route("**/api/v1/admin/smartstore-orders**", (route) => {
    if (route.request().method() !== "GET") return route.fallback();
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/actions")) return json(route, []);
    const order = orders.find((item) => path.endsWith(`/${item.productOrderId}`));
    if (order) return json(route, {
      order, placeOrderStatus: "OK", deliveryInfo: null, claimDetail: null,
      channelCommission: null, deliveryCompany: null, expectedDeliveryMethod: "DELIVERY",
      expectedSettlementAmount: null, paymentAmount: null, paymentCommission: null,
      saleCommission: null, shippingDueDate: null, trackingNumber: null, unitPrice: null,
    } satisfies SmartStoreChannelOrderDetailResponse);
    state.listReads++;
    return state.listUnavailable
      ? json(route, { code: "SERVICE_UNAVAILABLE" }, 503)
      : json(route, { content: orders, hasMore: false, nextCursor: null });
  });
  await page.route("**/api/v1/admin/order-claims?**", (route) =>
    json(route, { content: [], hasMore: false, nextCursor: null }));
  await page.goto("/admin?view=orders");
  await expect(page.getByLabel("po-editor-1 선택", { exact: true })).toBeVisible();
  return state;
}

for (const mode of ["단건", "일괄"] as const) {
  test(`@admin 스마트스토어 ${mode} 처리창은 새 주문의 입력을 초기화하고 발송 중 변경을 막는다`, async ({ page }) => {
    await page.clock.setFixedTime(new Date("2026-09-12T09:00:00+09:00"));
    await prepareSmartStoreOrderEditors(page);
    const dispatches: Array<{ path: string; body: Record<string, unknown> }> = [];
    let pending: Route | undefined;
    await page.route("**/api/v1/admin/smartstore-orders/**/dispatch", handleDispatch);
    await page.route("**/api/v1/admin/smartstore-orders/dispatch", handleDispatch);
    async function handleDispatch(route: Route) {
      dispatches.push({ path: new URL(route.request().url()).pathname, body: route.request().postDataJSON() });
      if (dispatches.length === 1) return json(route, { code: "SMARTSTORE_OPERATION_NOT_SENT" }, 503);
      pending = route;
    }
    async function open(id: string) {
      if (mode === "단건") {
        await page.getByRole("row").filter({ hasText: id })
          .getByRole("button", { name: "주문 처리", exact: true }).click();
      } else {
        await page.getByLabel(`${id} 선택`, { exact: true }).check();
        await page.getByRole("button", { name: "선택 주문 발송", exact: true }).click();
      }
    }
    await open("po-editor-1");
    const dialog = page.getByRole("dialog");
    const tracking = dialog.getByPlaceholder("운송장 번호", { exact: true });
    const company = dialog.getByPlaceholder(mode === "단건" ? "택배사 코드 (예: CJGLS)" : "택배사 코드", { exact: true });
    const date = mode === "단건" ? dialog.getByLabel("발송일시") : dialog.locator('input[type="datetime-local"]');
    const submit = dialog.getByRole("button", { name: mode === "단건" ? "발송 처리" : "일괄 발송", exact: true });
    const close = dialog.getByRole("button", { name: mode === "단건" ? "닫기" : "취소", exact: true });
    await tracking.fill("1111111111111");
    await company.fill("EPOST");
    await date.fill("2026-09-12T09:30");
    await submit.click();
    await expect(dialog.getByRole("alert")).toBeVisible();
    await expect(tracking).toHaveValue("1111111111111");
    await close.click();
    if (mode === "일괄") await page.getByLabel("po-editor-1 선택", { exact: true }).uncheck();
    await page.clock.setFixedTime(new Date("2026-09-12T11:00:00+09:00"));
    await open("po-editor-2");
    await expect(tracking).toHaveValue("");
    await expect(company).toHaveValue("");
    await expect(dialog.getByRole("alert")).not.toBeVisible();
    await expect(date).toHaveValue(mode === "단건" ? "" : "2026-09-12T11:00");
    await tracking.fill("2222222222222");
    await company.fill("EPOST");
    await date.fill("2026-09-12T11:15");
    await submit.click();
    await expect.poll(() => dispatches.length).toBe(2);
    await expect(tracking).toBeDisabled();
    await expect(date).toBeDisabled();
    await expect(close).toBeDisabled();
    await page.keyboard.press("Escape");
    await expect(dialog).toBeVisible();
    const request = { deliveryCompanyCode: "EPOST", trackingNumber: "2222222222222", dispatchDate: "2026-09-12T11:15" };
    expect(dispatches[1]).toMatchObject(mode === "단건"
      ? { path: "/api/v1/admin/smartstore-orders/po-editor-2/dispatch", body: request }
      : { path: "/api/v1/admin/smartstore-orders/dispatch", body: { orders: [{ productOrderId: "po-editor-2", ...request }] } });
    await json(pending!, { successProductOrderIds: ["po-editor-2"], failures: [] });
    if (mode === "단건") {
      await expect(close).toBeEnabled();
      await close.click();
    }
    await expect(dialog).not.toBeVisible();
    expect(dispatches).toHaveLength(2);
  });
}

test("@admin 스마트스토어 목록 재조회 실패가 열린 처리창의 초안을 지우지 않는다", async ({ page }) => {
  await page.clock.install();
  const state = await prepareSmartStoreOrderEditors(page);
  await page.getByRole("row").filter({ hasText: "po-editor-1" })
    .getByRole("button", { name: "주문 처리", exact: true }).click();
  const dialog = page.getByRole("dialog");
  const tracking = dialog.getByPlaceholder("운송장 번호", { exact: true });
  await tracking.fill("1234567890123");
  state.listUnavailable = true;
  await page.clock.fastForward(31_000);
  await expect.poll(() => state.listReads).toBeGreaterThan(1);
  await page.clock.runFor(2_000);
  const panel = page.locator(".admin-workspace-panel").filter({
    has: page.getByRole("heading", { name: "스마트스토어 채널 주문", exact: true }),
  });
  await expect(panel.getByRole("alert")).toBeVisible();
  await expect(dialog).toBeVisible();
  await expect(tracking).toHaveValue("1234567890123");
  await dialog.getByRole("button", { name: "닫기", exact: true }).click();
  state.listUnavailable = false;
  await panel.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(panel.getByRole("alert")).not.toBeVisible();
  await expect(page.getByLabel("po-editor-1 선택", { exact: true })).toBeVisible();
});

test("@admin 스마트스토어 일괄 발주 확인 중에는 대상 주문을 바꾸지 않는다", async ({ page }) => {
  await prepareSmartStoreOrderEditors(page);
  let pending: Route | undefined;
  await page.route("**/api/v1/admin/smartstore-orders/confirm", (route) => { pending = route; });
  await page.getByLabel("po-editor-1 선택", { exact: true }).check();
  await page.getByRole("button", { name: "선택 주문 발주 확인", exact: true }).click();
  await expect.poll(() => pending !== undefined).toBe(true);
  await expect(page.getByLabel("po-editor-2 선택", { exact: true })).toBeDisabled();
  await expect(page.getByRole("checkbox", { name: "확인이 필요한 주문만 보기" })).toBeDisabled();
  await expect(page.getByRole("button", { name: "선택 주문 발송", exact: true })).toBeDisabled();
  expect(pending!.request().postDataJSON()).toEqual({ productOrderIds: ["po-editor-1"] });
  await json(pending!, { successProductOrderIds: ["po-editor-1"], failures: [] });
  await expect(page.getByLabel("po-editor-2 선택", { exact: true })).toBeEnabled();
  await expect(page.getByLabel("po-editor-1 선택", { exact: true })).not.toBeChecked();
});

test("@admin 스마트스토어 회계 CSV는 수식 형태의 상품명을 보호하고 금액과 본문을 보존한다", async ({ page }) => {
  await prepareAdmin(page);
  const names = [
    ["=1+1", '"\t=1+1"'],
    ["+1+1", '"\t+1+1"'],
    ["-1+1", '"\t-1+1"'],
    ["@SUM(1,1)", '"\t@SUM(1,1)"'],
    ["  =1+1", '"\t  =1+1"'],
    ["\t=1+1", '"\t\t=1+1"'],
    ["\r=1+1", '"\t\r=1+1"'],
    ["\n=1+1", '"\t\n=1+1"'],
    ["＝1+1", '"\t＝1+1"'],
    ["＋1+1", '"\t＋1+1"'],
    ["－1+1", '"\t－1+1"'],
    ["＠SUM(1,1)", '"\t＠SUM(1,1)"'],
    ['가죽, "공예"\n수업', '"가죽, ""공예""\n수업"'],
  ];
  await page.route("**/api/v1/admin/smartstore-settlements/accounting?**", (route) => json(route, {
    from: "2026-08-01", to: "2026-08-31", vatAvailableThrough: "2026-08-31",
    dailySettlements: [], dailyVat: [],
    commissionDetails: names.map(([productName], index) => ({
      orderNo: `ORDER-${index}`, productOrderId: `PRODUCT-ORDER-${index}`, productName,
      merchantId: "happy-gallery", merchantName: "해피갤러리", productId: null,
      productOrderType: "NORMAL", payMeansType: null, settleType: "CANCEL",
      settleBasisDate: "2026-08-10", settleCompleteDate: null, settleExpectDate: null,
      taxReturnDate: null, commissionType: "SALE", commissionBasisAmount: -30000,
      commissionAmount: -900, maximumSellingInterlockCommissionAmount: null,
    })),
  } satisfies SmartStoreAccountingReportResponse));
  await page.route("**/api/v1/admin/smartstore-orders?**", (route) =>
    json(route, { content: [], hasMore: false, nextCursor: null }));
  await page.route("**/api/v1/admin/order-claims?**", (route) =>
    json(route, { content: [], hasMore: false, nextCursor: null }));
  await page.goto("/admin?view=orders");
  const downloadPromise = page.waitForEvent("download");
  await page.getByRole("button", { name: "CSV 다운로드", exact: true }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe("smartstore-accounting-2026-08-01-2026-08-31.csv");
  const stream = await download.createReadStream();
  const chunks: Buffer[] = [];
  for await (const chunk of stream!) chunks.push(Buffer.from(chunk));
  const csv = Buffer.concat(chunks).toString("utf8");
  expect(csv).toMatch(/^\uFEFF/);
  for (const [, escaped] of names) expect(csv).toContain(escaped);
  expect(csv).toContain('"CANCEL","2026-08-10","SALE","-30000","-900"\r\n');
  expect(csv).toContain('"[일별 부가세]"\r\n');
});

function smartStoreNotice(id: number): SmartStoreNoticeResponse {
  return {
    sellerNoticeId: id, postCategoryType: "ORDINARY", title: `공지 ${id}`,
    detailContents: `공지 ${id} 본문`, importantNotice: false,
    importantNoticeStartDate: null, importantNoticeEndDate: null, wholeNotice: false,
    displayStartDate: null, displayEndDate: null, popup: false,
    popupStartDate: null, popupEndDate: null,
  };
}

for (const scenario of [
  {
    timezoneId: "America/Los_Angeles", now: "2028-02-29T15:30:00Z", today: "2028-03-01",
    weekFrom: "2028-02-24", monthFrom: "2028-03-01", monthTo: "2028-03-31",
    previousFrom: "2028-02-01", previousTo: "2028-02-29", dispatchDate: "2028-03-01T00:30",
  },
  {
    timezoneId: "Pacific/Kiritimati", now: "2027-12-31T14:30:00Z", today: "2027-12-31",
    weekFrom: "2027-12-25", monthFrom: "2027-12-01", monthTo: "2027-12-31",
    previousFrom: "2027-11-01", previousTo: "2027-11-30", dispatchDate: "2027-12-31T23:30",
  },
]) {
  test.describe(`한국 시간 ${scenario.timezoneId}`, () => {
    test.use({ timezoneId: scenario.timezoneId });

    test("@admin 공지의 전시·중요·팝업 시각을 한국 시간으로 조회하고 저장한다", async ({ page }) => {
      await prepareAdmin(page);
      let saved: Record<string, unknown> | undefined;
      const notice = {
        ...smartStoreNotice(1), importantNotice: true, popup: true,
        displayStartDate: "2030-01-01T00:00:00Z", displayEndDate: "2030-01-01T01:00:00Z",
        importantNoticeStartDate: "2030-01-01T09:00:00+09:00", importantNoticeEndDate: "2030-01-01T10:00:00+09:00",
        popupStartDate: "2030-01-01T00:00:00Z", popupEndDate: "2030-01-01T01:00:00Z",
      } satisfies SmartStoreNoticeResponse;
      await page.route("**/api/v1/admin/smartstore-notices**", (route) => {
        if (route.request().method() === "PUT") {
          saved = route.request().postDataJSON();
          return json(route, { sellerNoticeId: 1 });
        }
        return json(route, new URL(route.request().url()).pathname.endsWith("/1") ? notice : {
          notices: [notice], page: 1, size: 100, totalElements: 1, totalPages: 1,
        });
      });
      await page.goto("/admin?view=support");
      await page.getByRole("row").filter({ hasText: "공지 1" })
        .getByRole("button", { name: "수정", exact: true }).click();
      const dialog = page.getByRole("dialog");
      const dates = dialog.locator('input[type="datetime-local"]');
      await expect(dates).toHaveCount(6);
      for (let index = 0; index < 6; index++) {
        await expect(dates.nth(index)).toHaveValue(index % 2 ? "2030-01-01T10:00" : "2030-01-01T09:00");
        await dates.nth(index).fill(index % 2 ? "2030-01-01T10:30" : "2030-01-01T09:30");
      }
      await dialog.getByRole("button", { name: "저장", exact: true }).click();
      await expect(dialog).not.toBeVisible();
      expect(saved).toMatchObject({
        displayStartDate: "2030-01-01T00:30:00.000Z", displayEndDate: "2030-01-01T01:30:00.000Z",
        importantNoticeStartDate: "2030-01-01T00:30:00.000Z", importantNoticeEndDate: "2030-01-01T01:30:00.000Z",
        popupStartDate: "2030-01-01T00:30:00.000Z", popupEndDate: "2030-01-01T01:30:00.000Z",
      });
    });

    test("@admin 정산 기간·발송 시각·예약 캘린더는 한국의 월말과 오늘을 사용한다", async ({ page }) => {
      await prepareAdmin(page);
      await page.clock.setFixedTime(new Date(scenario.now));
      let accountingRange: Record<string, string> | undefined;
      let synchronized: Record<string, unknown> | undefined;
      let dispatched: Record<string, unknown> | undefined;
      let calendarRange: Record<string, string> | undefined;
      await page.route("**/api/v1/admin/smartstore-settlements/accounting?**", (route) => {
        accountingRange = Object.fromEntries(new URL(route.request().url()).searchParams);
        return json(route, { ...accountingRange, vatAvailableThrough: scenario.previousTo,
          dailySettlements: [], commissionDetails: [], dailyVat: [] });
      });
      await page.route("**/api/v1/admin/smartstore-settlements/synchronize", (route) => {
        synchronized = route.request().postDataJSON();
        return json(route, { successCount: 0, issueCount: 0 });
      });
      const order = {
        productOrderId: "po-time", orderId: "order-time", originProductNo: 1,
        itemNo: null, productId: 1, productVariantId: null, productName: "발송 시각 확인 상품",
        productOption: null, productOrderStatus: "PAYED", claimType: null, claimStatus: null,
        initialQuantity: 1, remainQuantity: 1, inventoryAppliedQuantity: 1,
        attentionReason: null, paymentDate: null, lastChangedAt: "2027-12-01T10:00:00",
        pendingReturnQuantity: 0, returnReviewVersion: "R0:0", inventoryResolutionVersion: "v1",
      } satisfies SmartStoreChannelOrderResponse;
      await page.route("**/api/v1/admin/smartstore-orders?**", (route) =>
        json(route, { content: [order], hasMore: false, nextCursor: null }));
      await page.route("**/api/v1/admin/smartstore-orders/dispatch", (route) => {
        dispatched = route.request().postDataJSON();
        return json(route, { successProductOrderIds: ["po-time"], failures: [] });
      });
      await page.route("**/api/v1/admin/order-claims?**", (route) =>
        json(route, { content: [], hasMore: false, nextCursor: null }));
      await page.route("**/api/v1/admin/slots/calendar?**", (route) => {
        calendarRange = Object.fromEntries(new URL(route.request().url()).searchParams);
        return json(route, {
          settings: { openTime: "10:00", closeTime: "19:00", slotIntervalMin: 30,
            blockPublicHolidays: true, version: 1 },
          days: [{ date: scenario.today, effectiveAvailability: "OPEN", overrideMode: "DEFAULT",
            publicHoliday: false, timeBlocks: [] }],
        } satisfies BookingCalendarResponse);
      });
      await page.goto("/admin?view=orders");
      await expect.poll(() => accountingRange).toEqual({ from: scenario.previousFrom, to: scenario.previousTo });
      const settlement = page.locator(".admin-workspace-panel").filter({
        has: page.getByRole("heading", { name: "스마트스토어 정산 불일치", exact: true }),
      });
      await expect(settlement.locator('input[type="date"]').nth(0)).toHaveValue(scenario.weekFrom);
      await expect(settlement.locator('input[type="date"]').nth(1)).toHaveValue(scenario.today);
      await settlement.getByRole("button", { name: "선택 기간 조회", exact: true }).click();
      await expect.poll(() => synchronized).toEqual({ from: scenario.weekFrom, to: scenario.today });

      await page.getByLabel("po-time 선택", { exact: true }).check();
      await page.getByRole("button", { name: "선택 주문 발송", exact: true }).click();
      const dialog = page.getByRole("dialog");
      await expect(dialog.locator('input[type="datetime-local"]')).toHaveValue(scenario.dispatchDate);
      await dialog.getByPlaceholder("택배사 코드").fill("EPOST");
      await dialog.getByPlaceholder("운송장 번호").fill("1234567890123");
      await dialog.getByRole("button", { name: "일괄 발송", exact: true }).click();
      await expect(dialog).not.toBeVisible();
      expect(dispatched).toMatchObject({ orders: [{ productOrderId: "po-time", dispatchDate: scenario.dispatchDate }] });

      await page.goto("/admin?view=classes");
      await expect.poll(() => calendarRange).toEqual({ dateFrom: scenario.monthFrom, dateTo: scenario.monthTo });
      const calendar = page.locator(".admin-workspace-panel").filter({
        has: page.getByRole("heading", { name: "예약 캘린더", exact: true }),
      });
      await expect(calendar.getByRole("heading", { name: scenario.today, exact: true })).toBeVisible();
      await calendar.getByRole("button", { name: "이전 달", exact: true }).click();
      await expect.poll(() => calendarRange).toEqual({ dateFrom: scenario.previousFrom, dateTo: scenario.previousTo });
      await calendar.getByRole("button", { name: "다음 달", exact: true }).click();
      await expect(calendar.getByRole("heading", {
        name: scenario.timezoneId === "America/Los_Angeles" ? "2028년 3월" : "2027년 12월", exact: true,
      })).toBeVisible();
    });
  });
}

test("@admin 스마트스토어 공지 조회 실패 시 이전 초안을 저장하지 않고 다시 조회한다", async ({ page }) => {
  await prepareAdmin(page);
  let unavailable = true;
  const updates: Array<{ path: string; body: Record<string, unknown> }> = [];
  await page.route("**/api/v1/admin/smartstore-notices**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (route.request().method() === "PUT") {
      updates.push({ path, body: route.request().postDataJSON() });
      return json(route, { sellerNoticeId: 2 });
    }
    if (path.endsWith("/1")) return json(route, smartStoreNotice(1));
    if (path.endsWith("/2")) {
      return unavailable
        ? json(route, { code: "SERVICE_UNAVAILABLE" }, 503)
        : json(route, smartStoreNotice(2));
    }
    return json(route, {
      notices: [smartStoreNotice(1), smartStoreNotice(2)],
      page: 1, size: 100, totalElements: 2, totalPages: 1,
    });
  });
  await page.goto("/admin?view=support");
  await page.getByRole("row").filter({ hasText: "공지 1" })
    .getByRole("button", { name: "수정", exact: true }).click();
  const dialog = page.getByRole("dialog");
  const body = dialog.getByPlaceholder("공지 내용");
  await expect(body).toHaveValue("공지 1 본문");
  await body.fill("공지 1의 저장하지 않은 초안");
  await dialog.getByRole("button", { name: "취소", exact: true }).click();
  await page.getByRole("row").filter({ hasText: "공지 2" })
    .getByRole("button", { name: "수정", exact: true }).click();

  await expect(dialog.getByRole("alert")).toBeVisible();
  await expect(dialog.getByRole("button", { name: "저장", exact: true })).toBeDisabled();
  await expect(body).toBeDisabled();
  await expect(body).not.toHaveValue("공지 1의 저장하지 않은 초안");
  expect(updates).toHaveLength(0);

  unavailable = false;
  await dialog.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(body).toHaveValue("공지 2 본문");
  await body.fill("공지 2 수정 본문");
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  await expect(dialog).not.toBeVisible();
  expect(updates).toHaveLength(1);
  expect(updates[0]).toMatchObject({
    path: "/api/v1/admin/smartstore-notices/2",
    body: { title: "공지 2", detailContents: "공지 2 수정 본문" },
  });
});

test("@admin 스마트스토어 공지 저장 실패는 초안을 보존하고 새 공지에는 오류를 옮기지 않는다", async ({ page }) => {
  await prepareAdmin(page);
  let created: Record<string, unknown> | undefined;
  await page.route("**/api/v1/admin/smartstore-notices**", async (route) => {
    const request = route.request();
    if (request.method() === "PUT") return json(route, { code: "CONFLICT" }, 409);
    if (request.method() === "POST") {
      created = request.postDataJSON();
      return json(route, { sellerNoticeId: 3 });
    }
    if (new URL(request.url()).pathname.endsWith("/1")) return json(route, smartStoreNotice(1));
    return json(route, { notices: [smartStoreNotice(1)], page: 1, size: 100, totalElements: 1, totalPages: 1 });
  });
  await page.goto("/admin?view=support");
  await page.getByRole("row").filter({ hasText: "공지 1" })
    .getByRole("button", { name: "수정", exact: true }).click();
  const dialog = page.getByRole("dialog");
  const body = dialog.getByPlaceholder("공지 내용");
  await expect(body).toHaveValue("공지 1 본문");
  await body.fill("실패해도 보존할 초안");
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  await expect(dialog.getByRole("alert")).toBeVisible();
  await expect(body).toHaveValue("실패해도 보존할 초안");
  await dialog.getByRole("button", { name: "취소", exact: true }).click();

  await page.getByRole("button", { name: "공지 등록", exact: true }).click();
  await expect(dialog.getByRole("alert")).not.toBeVisible();
  await expect(body).toHaveValue("");
  await dialog.getByPlaceholder("공지 제목").fill("새 공지");
  await body.fill("새 공지 본문");
  await dialog.getByRole("button", { name: "저장", exact: true }).click();
  await expect(dialog).not.toBeVisible();
  expect(created).toMatchObject({ title: "새 공지", detailContents: "새 공지 본문" });
});

for (const inquiry of [
  {
    tab: "주문·배송 문의",
    path: "/api/v1/admin/smartstore-inquiries/customers/page",
    answerPath: "/api/v1/admin/smartstore-inquiries/customers/789/answer/456",
    item: (answer: string) => ({
      inquiryNo: 789, answerContentId: 456, category: "DELIVERY", title: "배송 문의",
      inquiryContent: "언제 출고되나요?", answerContent: answer, answered: true,
      orderId: "order-1", channelProductId: "123", productOrderIds: "po-1",
      productName: "각인 지갑", productOrderOption: "브라운", maskedCustomerId: "cust***",
      customerName: "홍*동", createdAt: "2026-08-30T10:00:00", answeredAt: "2026-08-30T11:00:00",
    }),
  },
  {
    tab: "상품 문의",
    path: "/api/v1/admin/smartstore-inquiries/page",
    answerPath: "/api/v1/admin/smartstore-inquiries/123/answer",
    item: (answer: string) => ({
      questionId: 123, channelProductId: 456, productName: "각인 지갑",
      maskedWriterId: "cust***", question: "언제 출고되나요?", answer, answered: true,
      createdAt: "2026-08-30T10:00:00",
    }),
  },
]) {
test(`@admin 스마트스토어 ${inquiry.tab} 답변은 기존 내용을 수정하고 실패 시 초안을 보존한다`, async ({ page }) => {
  await prepareAdmin(page);
  let answer = "오늘 출고 예정입니다.";
  let attempts = 0;
  await page.route("**/api/v1/admin/smartstore-inquiries**", async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname !== inquiry.path && url.pathname !== inquiry.answerPath) return route.fallback();
    if (route.request().method() === "PUT") {
      expect(url.pathname).toBe(inquiry.answerPath);
      expect(route.request().postDataJSON()).toEqual({ content: "내일 출고 예정입니다." });
      attempts += 1;
      if (attempts === 1) return json(route, { code: "CONFLICT" }, 409);
      answer = "내일 출고 예정입니다.";
      return route.fulfill({ status: 204 });
    }
    if (url.searchParams.get("unansweredOnly") === "true") return json(route, inquiryPage([]));
    return json(route, inquiryPage([inquiry.item(answer)]));
  });
  await page.goto("/admin?view=support");
  await page.getByRole("button", { name: inquiry.tab, exact: true }).click();
  await page.getByRole("checkbox", { name: "미답변 문의만 보기", exact: true }).uncheck();
  await page.getByRole("button", { name: "답변 수정", exact: true }).click();
  const draft = page.getByPlaceholder("스마트스토어에 등록할 답변");
  await expect(draft).toHaveValue("오늘 출고 예정입니다.");
  await draft.fill("저장하지 않을 초안");
  await page.getByRole("button", { name: "취소", exact: true }).click();
  expect(attempts).toBe(0);
  await page.getByRole("button", { name: "답변 수정", exact: true }).click();
  await expect(draft).toHaveValue("오늘 출고 예정입니다.");
  await draft.fill("내일 출고 예정입니다.");
  await page.getByRole("button", { name: "수정 저장", exact: true }).click();
  const inquiryPanel = page.locator(".admin-workspace-panel").filter({
    has: page.getByRole("heading", { name: "스마트스토어 문의", exact: true }),
  });
  await expect(inquiryPanel.getByRole("alert")).toContainText("요청을 완료할 수 없습니다. 안내된 조건을 확인하거나 잠시 후 다시 시도해 주세요.");
  await expect(draft).toHaveValue("내일 출고 예정입니다.");
  await page.getByRole("button", { name: "수정 저장", exact: true }).click();
  await expect(draft).not.toBeVisible();
  await expect(page.getByText("내일 출고 예정입니다.", { exact: false })).toBeVisible();
  expect(attempts).toBe(2);
});
}

test("@admin 문의 조회 실패 중에도 탭과 조회 조건을 사용하고 재시도할 수 있다", async ({ page }) => {
  await prepareAdmin(page);
  let unavailable = true;
  await page.route("**/api/v1/admin/smartstore-inquiries/page?**", (route) =>
    unavailable ? json(route, { code: "CONFLICT" }, 409) : json(route, inquiryPage([])));
  await page.goto("/admin?view=support");
  const panel = page.locator(".admin-workspace-panel").filter({
    has: page.getByRole("heading", { name: "스마트스토어 문의", exact: true }),
  });
  await expect(panel.getByRole("alert")).toBeVisible();
  await expect(panel.getByRole("checkbox", { name: "미답변 문의만 보기" })).toBeVisible();
  await expect(panel.getByText("답변을 기다리는 스마트스토어 문의가 없습니다.")).not.toBeVisible();
  await panel.getByRole("button", { name: "주문·배송 문의", exact: true }).click();
  await expect(panel.getByText("답변을 기다리는 스마트스토어 문의가 없습니다.")).toBeVisible();
  await panel.getByRole("button", { name: "상품 문의", exact: true }).click();
  await expect(panel.getByRole("alert")).toBeVisible();
  unavailable = false;
  await panel.getByRole("button", { name: "다시 시도", exact: true }).click();
  await expect(panel.getByRole("alert")).not.toBeVisible();
  await expect(panel.getByText("답변을 기다리는 스마트스토어 문의가 없습니다.")).toBeVisible();
});

test("@admin 반품 택배사 계약은 펼칠 때 조회하고 주문 수거 코드와 구분한다", async ({ page }) => {
  await prepareAdmin(page);
  let reads = 0;
  await page.route("**/api/v1/admin/smartstore-orders/return-delivery-companies", async (route) => {
    reads += 1;
    return json(route, [{ id: 1001, name: "CJ대한통운", priorityType: "PRIMARY" }]);
  });
  await page.goto("/admin?view=orders");
  const open = page.getByRole("button", { name: "등록된 반품 택배사 계약 조회", exact: true });
  await expect(open).toBeVisible();
  expect(reads).toBe(0);
  await open.click();
  await expect(page.getByRole("row").filter({ hasText: "1001" })).toContainText("CJ대한통운");
  await expect(page.getByText("계약번호는 발송·수거용 택배사 코드가 아니므로", { exact: false })).toBeVisible();
  expect(reads).toBe(1);
});

test("@admin 스마트스토어 확인 주문을 사유와 커서로 조회하고 재고 반영 방법을 지정한다", async ({ page }) => {
  await prepareAdmin(page);
  const reads: Array<Record<string, string>> = [];
  let resolutionBody: Record<string, unknown> | undefined;
  const order = {
    productOrderId: "po-manual-1",
    orderId: "order-manual-1",
    originProductNo: 123,
    itemNo: null,
    productId: null,
    productVariantId: null,
    productName: "매핑 확인 지갑",
    productOption: "색상: 브라운",
    productOrderStatus: "PAYED",
    claimType: null,
    claimStatus: null,
    initialQuantity: 2,
    remainQuantity: 2,
    inventoryAppliedQuantity: 0,
    attentionReason: "MAPPING_REQUIRED",
    paymentDate: "2026-09-02T10:00:00",
    lastChangedAt: "2026-09-02T10:01:00",
    pendingReturnQuantity: 0,
    returnReviewVersion: "R0:0",
    inventoryResolutionVersion: "resolution-1",
  };
  await page.route("**/api/v1/admin/smartstore-orders?**", async (route) => {
    const url = new URL(route.request().url());
    reads.push(Object.fromEntries(url.searchParams));
    await json(route, {
      content: [order],
      nextCursor: url.searchParams.has("cursor") ? null : "next-page",
      hasMore: !url.searchParams.has("cursor"),
    });
  });
  await page.route("**/api/v1/admin/products", (route) => json(route, [{
    id: 1,
    name: "내부 카드지갑",
    type: "READY_STOCK",
    price: 35000,
    quantity: 5,
    status: "ACTIVE",
    available: true,
    category: null,
    imageUrl: null,
    description: null,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
    variants: [],
    optionGroups: [],
  }]));
  await page.route("**/api/v1/admin/smartstore-orders/po-manual-1/inventory-resolution", async (route) => {
    resolutionBody = route.request().postDataJSON() as Record<string, unknown>;
    await json(route, { ...order, productId: 1, inventoryAppliedQuantity: 2, attentionReason: null });
  });
  await page.route("**/api/v1/admin/smartstore-orders/po-manual-1/actions", (route) => json(route, [{
    id: 71,
    action: "INVENTORY_RESOLVED",
    status: "SUCCEEDED",
    requestSummary: "상품 1, 재고 반영 방법 APPLY_REMAINING, 사유: 스마트스토어 옵션과 내부 상품을 확인",
    resultCode: null,
    resultMessage: null,
    changedByAdminId: 1,
    changedBy: "운영 관리자",
    requestedAt: "2026-09-02T10:10:00",
    completedAt: "2026-09-02T10:10:00",
  }]));
  await page.route("**/api/v1/admin/smartstore-orders/po-manual-1", (route) => json(route, {
    order,
    deliveryInfo: null,
    placeOrderStatus: "OK",
    shippingDueDate: null,
    expectedDeliveryMethod: "DELIVERY",
    deliveryCompany: null,
    trackingNumber: null,
    unitPrice: 35000,
    paymentAmount: 70000,
    paymentCommission: null,
    saleCommission: null,
    channelCommission: null,
    expectedSettlementAmount: 68000,
    claimDetail: null,
  }));

  await page.goto("/admin?view=orders");
  await page.getByRole("checkbox", { name: "확인이 필요한 주문만 보기" }).check();
  await page.getByLabel("확인 필요 사유 필터").selectOption("MAPPING_REQUIRED");
  await expect.poll(() => reads.at(-1)).toMatchObject({
    attentionOnly: "true",
    attentionReason: "MAPPING_REQUIRED",
    size: "50",
  });
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect.poll(() => reads.at(-1)).toMatchObject({ cursor: "next-page" });
  await page.getByRole("button", { name: "상품 연결·재고 반영 방법", exact: true }).click();
  await page.getByLabel("해피갤러리 상품").selectOption("1");
  await page.getByLabel("처리 사유").fill("스마트스토어 옵션과 내부 상품을 확인");
  await page.getByRole("button", { name: "상품 연결과 재고 반영 방법 저장", exact: true }).click();

  await expect.poll(() => resolutionBody).toEqual({
    productId: 1,
    productVariantId: null,
    action: "APPLY_REMAINING",
    reason: "스마트스토어 옵션과 내부 상품을 확인",
    resolutionVersion: "resolution-1",
  });
  await page.getByRole("button", { name: "주문 처리", exact: true }).click();
  await expect(page.getByRole("region", { name: "주문 처리 이력" })).toContainText("재고 반영 방법 지정");
  await expect(page.getByRole("region", { name: "주문 처리 이력" })).toContainText("운영 관리자");
});

test("@admin 스마트스토어 요청의 반영 여부를 확인하고 미반영 주문 처리 화면으로 이동한다", async ({ page }) => {
  await prepareAdmin(page);
  let reconciled = false;
  let reconciliationBody: Record<string, unknown> | undefined;
  const productOrderId = "po-reconcile-1";
  const history = {
    id: 72,
    productOrderId,
    action: "ORDER_CONFIRMED",
    status: "RESULT_UNKNOWN",
    requestSummary: null,
    resultCode: "RESULT_UNKNOWN",
    resultMessage: "네이버 응답에서 처리 결과를 확인할 수 없습니다.",
    changedByAdminId: 7,
    changedBy: "주문 관리자",
    requestedAt: "2026-09-02T10:00:00",
    completedAt: "2026-09-02T10:00:30",
    reconciliationOutcome: null,
    reconciliationNote: null,
    reconciledByAdminId: null,
    reconciledBy: null,
    reconciledAt: null,
  };
  const order = {
    productOrderId,
    orderId: "order-reconcile-1",
    originProductNo: 123,
    itemNo: null,
    productId: 1,
    productVariantId: null,
    productName: "대사 확인 지갑",
    productOption: null,
    productOrderStatus: "PAYED",
    claimType: null,
    claimStatus: null,
    initialQuantity: 1,
    remainQuantity: 1,
    inventoryAppliedQuantity: 1,
    attentionReason: null,
    paymentDate: "2026-09-02T09:50:00",
    lastChangedAt: "2026-09-02T09:55:00",
    pendingReturnQuantity: 0,
    returnReviewVersion: "R0:0",
    inventoryResolutionVersion: "resolution-1",
  };
  await page.route("**/api/v1/admin/smartstore-orders/actions/unresolved?**", (route) =>
    json(route, {
      content: reconciled ? [] : [history],
      nextCursor: null,
      hasMore: false,
    }));
  await page.route(`**/api/v1/admin/smartstore-orders/${productOrderId}/current-status`, (route) =>
    json(route, {
      productOrderId,
      productOrderStatus: "PAYED",
      placeOrderStatus: "NOT_YET",
      claimType: null,
      claimStatus: null,
      remainQuantity: 1,
      shippingDueDate: "2026-09-05T18:00:00",
      expectedDeliveryMethod: "DELIVERY",
      deliveryCompany: null,
      trackingNumber: null,
      claimDetail: null,
    }));
  await page.route("**/api/v1/admin/smartstore-orders/actions/72/reconciliation", async (route) => {
    reconciliationBody = route.request().postDataJSON() as Record<string, unknown>;
    reconciled = true;
    await json(route, {
      ...history,
      reconciliationOutcome: "NOT_APPLIED",
      reconciliationNote: "네이버 발주 상태가 미처리임을 확인",
      reconciledByAdminId: 19,
      reconciledBy: "대사 관리자",
      reconciledAt: "2026-09-02T10:10:00",
    });
  });
  await page.route(`**/api/v1/admin/smartstore-orders/${productOrderId}/actions`, (route) =>
    json(route, [{
      ...history,
      reconciliationOutcome: "NOT_APPLIED",
      reconciliationNote: "네이버 발주 상태가 미처리임을 확인",
      reconciledByAdminId: 19,
      reconciledBy: "대사 관리자",
      reconciledAt: "2026-09-02T10:10:00",
    }]));
  await page.route(`**/api/v1/admin/smartstore-orders/${productOrderId}`, (route) => json(route, {
    order,
    deliveryInfo: null,
    placeOrderStatus: "NOT_YET",
    shippingDueDate: "2026-09-05T18:00:00",
    expectedDeliveryMethod: "DELIVERY",
    deliveryCompany: null,
    trackingNumber: null,
    unitPrice: 35000,
    paymentAmount: 35000,
    paymentCommission: null,
    saleCommission: null,
    channelCommission: null,
    expectedSettlementAmount: 34000,
    claimDetail: null,
  }));
  await page.route(
    (url) => url.pathname === "/api/v1/admin/smartstore-orders",
    (route) => json(route, {
      content: [order],
      nextCursor: null,
      hasMore: false,
    }),
  );

  await page.goto("/admin");
  const panel = page.locator(".admin-workspace-panel").filter({
    has: page.getByRole("heading", { name: "스마트스토어 주문 처리 결과 확인", exact: true }),
  });
  await panel.getByRole("button", { name: "네이버 상태 확인", exact: true }).click();
  await expect(page.getByText("발주: NOT_YET", { exact: true })).toBeVisible();
  await page.getByLabel("반영 여부 확인 결과").selectOption("NOT_APPLIED");
  await page.getByLabel("확인 근거").fill("네이버 발주 상태가 미처리임을 확인");
  await page.getByRole("button", { name: "미반영 저장 후 주문 화면 열기", exact: true }).click();

  await expect.poll(() => reconciliationBody).toEqual({
    outcome: "NOT_APPLIED",
    note: "네이버 발주 상태가 미처리임을 확인",
  });
  await expect(page).toHaveURL(`/admin?view=orders&smartstoreOrderId=${productOrderId}`);
  const orderDialog = page.getByRole("dialog");
  await expect(orderDialog).toContainText("스마트스토어 주문 처리");
  await expect(orderDialog).toContainText(productOrderId);
  await orderDialog.getByRole("button", { name: "닫기", exact: true }).click();
  await page.getByRole("button", { name: "오늘 할 일", exact: true }).click();
  await expect(panel.getByText("처리 결과를 확인할 스마트스토어 주문 요청이 없습니다.", {
    exact: true,
  })).toBeVisible();
});

test("@admin 스마트스토어 문의는 기간과 페이지를 선택하고 탭별 조회 조건을 유지한다", async ({ page }) => {
  await prepareAdmin(page);
  const reads: Array<Record<string, string>> = [];
  await page.route("**/api/v1/admin/smartstore-inquiries/**", (route) => {
    const url = new URL(route.request().url());
    if (!url.pathname.endsWith("/page")) return route.fallback();
    const type = url.pathname.includes("/customers/") ? "customer" : "product";
    reads.push({ type, ...Object.fromEntries(url.searchParams) });
    const pageNumber = Number(url.searchParams.get("page"));
    return json(route, inquiryPage(type === "customer" ? [] : [{
      questionId: 100 + pageNumber, channelProductId: 456, productName: "기간 검색 작품",
      maskedWriterId: "cust***", question: `상품 문의 ${pageNumber + 1}`, answer: null,
      answered: false, createdAt: "2026-03-01T10:00:00",
    }], pageNumber, type === "customer" ? 0 : 101));
  });
  await page.goto("/admin?view=support");
  const panel = page.locator(".admin-workspace-panel").filter({
    has: page.getByRole("heading", { name: "스마트스토어 문의", exact: true }),
  });
  await panel.getByLabel("문의 시작일", { exact: true }).fill("2026-01-01");
  await panel.getByLabel("문의 종료일", { exact: true }).fill("2026-03-31");
  await panel.getByRole("button", { name: "문의 조회", exact: true }).click();
  await expect.poll(() => reads.at(-1)).toMatchObject({
    type: "product", from: "2026-01-01", to: "2026-03-31", page: "0", size: "50", unansweredOnly: "true",
  });
  await panel.getByRole("button", { name: "다음 페이지", exact: true }).click();
  await expect(panel.getByText("상품 문의 2", { exact: true })).toBeVisible();
  await panel.getByRole("button", { name: "주문·배송 문의", exact: true }).click();
  await panel.getByLabel("문의 시작일", { exact: true }).fill("2026-02-01");
  await panel.getByLabel("문의 종료일", { exact: true }).fill("2026-02-10");
  await panel.getByRole("button", { name: "문의 조회", exact: true }).click();
  await panel.getByRole("checkbox", { name: "미답변 문의만 보기" }).uncheck();
  await expect.poll(() => reads.at(-1)).toMatchObject({
    type: "customer", from: "2026-02-01", to: "2026-02-10", page: "0", unansweredOnly: "false",
  });
  await panel.getByRole("button", { name: "상품 문의", exact: true }).click();
  await expect(panel.getByLabel("문의 시작일", { exact: true })).toHaveValue("2026-01-01");
  await expect(panel.getByRole("checkbox", { name: "미답변 문의만 보기" })).toBeChecked();
  await expect(panel.getByText("상품 문의 2", { exact: true })).toBeVisible();
  await panel.getByRole("button", { name: "이전 페이지", exact: true }).click();
  await expect(panel.getByText("상품 문의 1", { exact: true })).toBeVisible();
  await panel.getByRole("button", { name: "주문·배송 문의", exact: true }).click();
  await expect(panel.getByLabel("문의 시작일", { exact: true })).toHaveValue("2026-02-01");
  await expect(panel.getByRole("checkbox", { name: "미답변 문의만 보기" })).not.toBeChecked();
  await expect(panel.getByRole("button", { name: "다음 페이지", exact: true })).toBeDisabled();
});

async function prepareSmartStoreInventoryEditor(page: Page) {
  await prepareAdmin(page);
  await page.route("**/api/v1/admin/products", (route) => json(route, [1, 2].map((id) => ({
    id,
    name: id === 1 ? "연동 작품" : "다른 작품",
    type: "READY_STOCK",
    price: 35000,
    quantity: 5,
    status: "ACTIVE",
    available: true,
    category: null,
    imageUrl: null,
    description: null,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
    variants: [],
    optionGroups: [],
  }))));
  await page.route("**/api/v1/admin/products/smartstore-catalog?**", (route) => json(route, {
    products: [
      {
        originProductNo: 123,
        channelProductNo: 1001,
        name: "기존 원상품",
        imageUrl: null,
        salePrice: 35000,
        stockQuantity: 5,
        status: "SALE",
      },
      {
        originProductNo: 456,
        channelProductNo: 1002,
        name: "새 원상품",
        imageUrl: null,
        salePrice: 36000,
        stockQuantity: 4,
        status: "SALE",
      },
    ],
    page: 1,
    size: 100,
    totalElements: 2,
    totalPages: 1,
  }));
  await page.route("**/api/v1/admin/products/smartstore-catalog/*", (route) => {
    const originProductNo = Number(new URL(route.request().url()).pathname.split("/").at(-1));
    return json(route, { originProductNo, salePrice: 35000, status: "SALE", options: [] });
  });
  await page.route("**/api/v1/admin/products/1/smartstore-product-preview", (route) => json(route, {
    productId: 1,
    originProductNo: 123,
    localSalePrice: 35000,
    channelSalePrice: 35000,
    localStatus: "SALE",
    channelStatus: "SALE",
    options: [],
    different: true,
    previewVersion: "preview-1",
  }));
  const mapping: SmartStoreInventoryMappingResponse = {
    productId: 1, mappingVersion: 17, originProductNo: 123, enabled: true,
    variants: [], syncStatus: "FAILED", attemptCount: 10,
    lastError: "재고 반영 요청 실패", syncedAt: null,
  };
  await page.route("**/api/v1/admin/products/*/smartstore-inventory**", (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path.endsWith("/history")) return json(route, []);
    if (path.includes("/products/2/")) return json(route, {}, 404);
    return json(route, mapping);
  });
  return mapping;
}

for (const action of ["연동 저장", "재시도", "차이 반영", "연동 해제 실행"] as const) {
  test(`@admin 스마트스토어 재고 연동창은 ${action} 요청 중 편집과 다른 요청을 막는다`, async ({ page }) => {
    const mapping = await prepareSmartStoreInventoryEditor(page);
    let pending: Route | undefined;
    const requests: Array<{ path: string; method: string; body: unknown }> = [];
    await page.route("**/api/v1/admin/products/1/**", (route) => {
      if (route.request().method() === "GET") return route.fallback();
      pending = route;
      requests.push({
        path: new URL(route.request().url()).pathname,
        method: route.request().method(),
        body: route.request().postDataJSON(),
      });
    });
    await page.goto("/admin?view=products");
    await page.getByRole("row").filter({ hasText: "연동 작품" })
      .getByRole("button", { name: "스마트스토어", exact: true }).click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByRole("button", { name: "차이 반영", exact: true })).toBeVisible();
    await dialog.getByText("새 원상품", { exact: true }).click();
    await dialog.getByRole("checkbox", {
      name: "기존 원상품 123의 판매 중지·재고 확인을 완료했습니다.",
    }).check();
    if (action === "연동 해제 실행") {
      await dialog.getByRole("button", { name: "연동 해제", exact: true }).click();
      await dialog.getByRole("checkbox", {
        name: "기존 원상품 123의 판매 중지·재고 확인을 완료했습니다.",
      }).last().check();
    }
    await dialog.getByRole("button", { name: action, exact: true }).click();
    await expect.poll(() => requests.length).toBe(1);
    await expect(dialog.getByPlaceholder("상품명·원상품 번호·채널상품 번호 검색")).toBeDisabled();
    await expect(dialog.getByRole("checkbox", { name: "재고 변경 시 스마트스토어에 자동 반영" })).toBeDisabled();
    await expect(dialog.getByRole("button", { name: "Close", exact: true })).toBeHidden();
    for (const button of await dialog.getByRole("button").all()) await expect(button).toBeDisabled();
    await page.keyboard.press("Escape");
    await expect(dialog).toBeVisible();
    await dialog.getByText("기존 원상품", { exact: true }).click({ force: true });
    await expect(dialog.getByRole("button").filter({ hasText: "새 원상품" })).toHaveClass(/table-primary/);
    await expect.poll(() => requests.length).toBe(1);
    const suffix = action === "재시도" ? "smartstore-inventory/retry"
      : action === "차이 반영" ? "smartstore-product-sync" : "smartstore-inventory";
    expect(requests[0].path).toBe(`/api/v1/admin/products/1/${suffix}`);
    if (action === "연동 저장") expect(requests[0].body).toMatchObject({
      originProductNo: 456, expectedMappingVersion: mapping.mappingVersion, previousOriginConfirmed: true,
    });
    await json(pending!, { code: "SERVICE_UNAVAILABLE" }, 503);
    await expect(dialog.getByPlaceholder("상품명·원상품 번호·채널상품 번호 검색")).toBeEnabled();
    await expect(dialog.getByRole("button").filter({ hasText: "새 원상품" })).toHaveClass(/table-primary/);
    await dialog.getByRole("button", { name: "Close", exact: true }).click();
    await expect(dialog).toBeHidden();
  });
}

test("@admin 스마트스토어 재고 재시도는 작성 중인 설정을 유지하고 다시 열면 초기화한다", async ({ page }) => {
  const mapping = await prepareSmartStoreInventoryEditor(page);
  await page.route("**/api/v1/admin/products/1/smartstore-inventory/retry", (route) =>
    json(route, { ...mapping, syncStatus: "PENDING", attemptCount: 0, lastError: null }));
  await page.goto("/admin?view=products");
  const open = (name: string) => page.getByRole("row").filter({ hasText: name })
    .getByRole("button", { name: "스마트스토어", exact: true }).click();
  await open("연동 작품");
  const dialog = page.getByRole("dialog");
  await expect(dialog.getByText("확인 필요", { exact: true })).toBeVisible();
  await dialog.getByText("새 원상품", { exact: true }).click();
  const confirmed = dialog.getByRole("checkbox", {
    name: "기존 원상품 123의 판매 중지·재고 확인을 완료했습니다.",
  });
  await confirmed.check();
  await dialog.getByRole("checkbox", { name: "재고 변경 시 스마트스토어에 자동 반영" }).uncheck();
  await dialog.getByRole("button", { name: "재시도", exact: true }).click();
  await expect(dialog.getByText("반영 대기", { exact: true })).toBeVisible();
  await expect(dialog.getByRole("button").filter({ hasText: "새 원상품" })).toHaveClass(/table-primary/);
  await expect(dialog.getByRole("checkbox", { name: "재고 변경 시 스마트스토어에 자동 반영" })).not.toBeChecked();
  await expect(confirmed).toBeChecked();
  await dialog.getByRole("button", { name: "Close", exact: true }).click();
  await open("다른 작품");
  await expect(dialog.getByRole("button").filter({ hasText: "새 원상품" })).not.toHaveClass(/table-primary/);
  await expect(dialog.getByRole("button", { name: "연동 저장", exact: true })).toBeDisabled();
  await dialog.getByRole("button", { name: "Close", exact: true }).click();
  await open("연동 작품");
  await expect(dialog.getByRole("button").filter({ hasText: "기존 원상품" })).toHaveClass(/table-primary/);
  await expect(dialog.getByRole("checkbox", { name: "재고 변경 시 스마트스토어에 자동 반영" })).toBeChecked();
  await expect(confirmed).toBeHidden();
});

test("@admin 스마트스토어 원상품 변경과 해제는 기존 매핑 확인과 최신 개정을 요구한다", async ({ page }) => {
  await prepareSmartStoreInventoryEditor(page);
  let releaseMapping: (() => void) | undefined;
  const mappingGate = new Promise<void>((resolve) => {
    releaseMapping = resolve;
  });
  let savedBody: Record<string, unknown> | undefined;
  let deleteParams: Record<string, string> | undefined;
  await page.route("**/api/v1/admin/products/1/smartstore-inventory**", async (route) => {
    if (new URL(route.request().url()).pathname.endsWith("/history")) {
      return json(route, [{
        id: 21,
        action: "ORIGIN_CHANGED",
        previousOriginProductNo: 111,
        nextOriginProductNo: 123,
        previousEnabled: true,
        nextEnabled: true,
        previousOptionMappings: null,
        nextOptionMappings: null,
        previousMappingVersion: 16,
        nextMappingVersion: 17,
        previousOriginConfirmed: true,
        changedByAdminId: 1,
        changedBy: "운영자",
        changedAt: "2026-09-02T14:30:00",
      }]);
    }
    if (route.request().method() === "DELETE") {
      deleteParams = Object.fromEntries(new URL(route.request().url()).searchParams);
      return route.fulfill({ status: 204 });
    }
    if (route.request().method() === "PUT") {
      savedBody = route.request().postDataJSON();
      return json(route, {
        productId: 1,
        mappingVersion: 18,
        originProductNo: 456,
        enabled: true,
        variants: [],
        syncStatus: "PENDING",
        attemptCount: 0,
        lastError: null,
        syncedAt: null,
      });
    }
    await mappingGate;
    return json(route, {
      productId: 1,
      mappingVersion: 17,
      originProductNo: 123,
      enabled: true,
      variants: [],
      syncStatus: "SYNCED",
      attemptCount: 0,
      lastError: null,
      syncedAt: "2026-09-01T09:00:00+09:00",
    });
  });

  await page.goto("/admin?view=products");
  await page.getByRole("row").filter({ hasText: "연동 작품" })
    .getByRole("button", { name: "스마트스토어", exact: true }).click();
  await page.getByText("새 원상품", { exact: true }).click();
  await expect(page.getByRole("button", { name: "연동 저장", exact: true })).toBeDisabled();

  releaseMapping?.();
  await expect(page.getByText("현재 상태:", { exact: false })).toBeVisible();
  await expect(page.getByText("최근 변경 이력", { exact: true })).toBeVisible();
  await expect(page.getByText("운영자 #1", { exact: true })).toBeVisible();
  await page.getByText("새 원상품", { exact: true }).click();
  await expect(page.getByText("기존 원상품 123에서 새 원상품 456(으)로 변경합니다.", { exact: false })).toBeVisible();
  await page.getByRole("checkbox", {
    name: "기존 원상품 123의 판매 중지·재고 확인을 완료했습니다.",
  }).check();
  await page.getByRole("button", { name: "연동 저장", exact: true }).click();

  await expect.poll(() => savedBody).toEqual({
    originProductNo: 456,
    enabled: true,
    variants: [],
    expectedMappingVersion: 17,
    previousOriginConfirmed: true,
  });

  await page.getByRole("button", { name: "연동 해제", exact: true }).click();
  await expect(page.getByText("기존 원상품 456의 연결과 보존된 과거 옵션 연결을 모두 삭제합니다.", {
    exact: false,
  })).toBeVisible();
  const unlink = page.getByRole("button", { name: "연동 해제 실행", exact: true });
  await expect(unlink).toBeDisabled();
  await page.getByRole("checkbox", {
    name: "기존 원상품 456의 판매 중지·재고 확인을 완료했습니다.",
  }).check();
  await unlink.click();

  await expect.poll(() => deleteParams).toEqual({
    expectedMappingVersion: "18",
    previousOriginConfirmed: "true",
  });
});
