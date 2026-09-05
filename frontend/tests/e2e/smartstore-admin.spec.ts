import { expect, test, type Page, type Route } from "@playwright/test";
import type { RestockDemandPageResponse } from "../../src/generated/api/adminCatalog";
import type {
  GroupInquiryFollowUpPageResponse,
  GroupInquiryPageResponse,
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

test("@admin 스마트스토어 확인 주문을 사유와 커서로 조회하고 수동 재고 결정을 저장한다", async ({ page }) => {
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
    requestSummary: "상품 1, 재고 결정 APPLY_REMAINING, 사유: 스마트스토어 옵션과 내부 상품을 확인",
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
  await page.getByLabel("확인 사유 필터").selectOption("MAPPING_REQUIRED");
  await expect.poll(() => reads.at(-1)).toMatchObject({
    attentionOnly: "true",
    attentionReason: "MAPPING_REQUIRED",
    size: "50",
  });
  await page.getByRole("button", { name: "다음", exact: true }).click();
  await expect.poll(() => reads.at(-1)).toMatchObject({ cursor: "next-page" });
  await page.getByRole("button", { name: "상품 연결·재고 결정", exact: true }).click();
  await page.getByLabel("내부 상품").selectOption("1");
  await page.getByLabel("처리 사유").fill("스마트스토어 옵션과 내부 상품을 확인");
  await page.getByRole("button", { name: "상품 연결과 재고 결정 저장", exact: true }).click();

  await expect.poll(() => resolutionBody).toEqual({
    productId: 1,
    productVariantId: null,
    action: "APPLY_REMAINING",
    reason: "스마트스토어 옵션과 내부 상품을 확인",
    resolutionVersion: "resolution-1",
  });
  await page.getByRole("button", { name: "주문 처리", exact: true }).click();
  await expect(page.getByRole("region", { name: "주문 처리 이력" })).toContainText("수동 재고 결정");
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

test("@admin 스마트스토어 원상품 변경과 해제는 기존 매핑 확인과 최신 개정을 요구한다", async ({ page }) => {
  await prepareAdmin(page);
  let releaseMapping: (() => void) | undefined;
  const mappingGate = new Promise<void>((resolve) => {
    releaseMapping = resolve;
  });
  let savedBody: Record<string, unknown> | undefined;
  let deleteParams: Record<string, string> | undefined;

  await page.route("**/api/v1/admin/products", (route) => json(route, [{
    id: 1,
    name: "연동 작품",
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
    different: false,
    previewVersion: "preview-1",
  }));
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
