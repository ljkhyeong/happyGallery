import { expect, test, type Page } from "@playwright/test";
import type { CreateQnaRequest, ProductQnaDetail } from "../../src/generated/api/productQna";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";
import { skipExternalFonts } from "./external-fonts";

test.beforeEach(skipExternalFonts);
test.afterEach(clearSsrUpstreamFixtures);

async function mockQna(page: Page) {
  const product = { id: 42, name: "문의 확인 작품", type: "READY_STOCK", price: 12000, available: true,
    stockQuantity: 3, description: null, category: "공예", imageUrl: null, specification: null,
    careInstructions: null, productionLeadDays: null, optionGroups: [], variants: [] };
  const state = {
    accountId: 501, hold: false, fail: false, release: () => {},
    requests: [] as Array<{ accountId: number; body: CreateQnaRequest }>,
    rows: [] as Array<ProductQnaDetail & { userId: number }>,
  };
  await replaceSsrUpstreamFixtures(ssrApiFixture("/products/42", product));
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({ contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=qna-writing; Path=/" }, body: "{}" });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`, email: "qna@example.com",
        phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/me/products/42/qna": {
        const body = request.postDataJSON() as CreateQnaRequest;
        const accountId = state.accountId;
        state.requests.push({ accountId, body });
        if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
        if (state.fail) return json({ code: "SERVICE_UNAVAILABLE" }, 503);
        const row = { ...body, id: 91, userId: accountId, productId: 42, authorName: `회원${accountId}`,
          createdAt: "2026-09-12T10:00:00", repliedAt: "2026-09-12T11:00:00", replyContent: "첫 번째 답변\n두 번째 답변" };
        state.rows.push(row);
        return json({ id: row.id, productId: 42, title: row.title, secret: row.secret, createdAt: row.createdAt }, 201);
      }
      case "/api/v1/products/42/qna/page": return json({ content: state.rows.map((row) => ({
        id: row.id, title: row.secret ? "비밀 문의입니다." : row.title, secret: row.secret,
        authorName: "회원***", createdAt: row.createdAt, hasReply: true,
      })), hasMore: false, nextCursor: null });
      case "/api/v1/me/products/42/qna/page": return json({ content: state.rows.filter((row) => row.userId === state.accountId)
        .map((row) => ({ id: row.id, title: row.title, secret: row.secret, createdAt: row.createdAt, hasReply: true })), hasMore: false, nextCursor: null });
      case "/api/v1/me/products/42/qna/91": {
        const row = state.rows.find((item) => item.id === 91 && item.userId === state.accountId);
        return row ? json(row) : json({ code: "NOT_FOUND" }, 404);
      }
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/coupons": return json([]);
      case "/api/v1/me/rewards": return json({ availableBalance: 0, reservedBalance: 0, debtBalance: 0, history: [] });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/favorites/PRODUCT/42": return json({ saved: false });
      case "/api/v1/products/42": return route.fallback();
      case "/api/v1/products/42/reviews": return json({ content: [], filteredCount: 0, hasMore: false, nextCursor: null,
        summary: { averageRating: 0, reviewCount: 0, histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 } } });
      case "/api/v1/orders/policy": return json({ shippingFee: 3000, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "주문제작 동의" });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      default: throw new Error(`정의하지 않은 상품 문의 작성 테스트 요청: ${request.method()} ${pathname}`);
    }
  });
  await page.goto("/products/42");
  await page.getByRole("button", { name: "문의 작성", exact: true }).click();
  return state;
}

async function writeQna(page: Page) {
  await page.getByLabel("제목", { exact: true }).fill("각인 문구 문의");
  await page.getByLabel("내용", { exact: true }).fill("안녕하세요.\n두 줄로 각인할 수 있나요?");
  await page.getByLabel("비밀글", { exact: true }).check();
}

test("상품 문의는 작성 취소·이동·새로고침 전에 입력 삭제를 확인한다", async ({ page }, testInfo) => {
  const state = await mockQna(page);
  await page.getByRole("button", { name: "취소", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("button", { name: "문의 작성", exact: true }).click();
  await writeQna(page);
  const form = page.getByLabel("제목", { exact: true }).locator("xpath=ancestor::form");
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 844 });
    await form.screenshot({ path: testInfo.outputPath(`qna-form-${width}.png`), animations: "disabled" });
  }
  await page.getByRole("button", { name: "취소", exact: true }).click();
  const dialog = page.getByRole("dialog", { name: "문의 작성을 그만둘까요?" });
  await expect(dialog).toBeVisible();
  await dialog.screenshot({ path: testInfo.outputPath("qna-discard-mobile.png"), animations: "disabled" });
  await dialog.getByRole("button", { name: "계속 작성", exact: true }).click();
  await expect(page.getByLabel("비밀글", { exact: true })).toBeChecked();
  const reloadDialog = page.waitForEvent("dialog");
  await page.evaluate(() => { window.setTimeout(() => window.location.reload(), 0); });
  const confirmation = await reloadDialog;
  expect(confirmation.type()).toBe("beforeunload");
  await confirmation.dismiss();
  await expect(page.getByLabel("내용", { exact: true })).toHaveValue("안녕하세요.\n두 줄로 각인할 수 있나요?");
  await page.getByRole("button", { name: "취소", exact: true }).click();
  await dialog.getByRole("button", { name: "내용 버리기", exact: true }).click();
  await page.getByRole("button", { name: "문의 작성", exact: true }).click();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("비밀글", { exact: true })).not.toBeChecked();
  await writeQna(page);
  await page.setViewportSize({ width: 1280, height: 844 });
  await page.getByRole("link", { name: "장바구니", exact: true }).click();
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "계속 작성", exact: true }).click();
  await expect(page).toHaveURL(/\/products\/42$/);
  await page.getByRole("link", { name: "장바구니", exact: true }).click();
  await dialog.getByRole("button", { name: "내용 버리고 이동", exact: true }).click();
  await expect(page).toHaveURL(/\/cart$/);
  expect(state.requests).toEqual([]);
});

test("상품 문의 등록 중에는 입력·중복 제출을 막고 실패 후 재등록과 줄바꿈을 유지한다", async ({ page }, testInfo) => {
  const state = await mockQna(page);
  await writeQna(page);
  state.hold = true;
  state.fail = true;
  const form = page.getByLabel("제목", { exact: true }).locator("xpath=ancestor::form");
  await form.evaluate((element: HTMLFormElement) => { element.requestSubmit(); element.requestSubmit(); });
  await expect.poll(() => state.requests.length).toBe(1);
  for (const label of ["제목", "내용", "비밀글"]) await expect(page.getByLabel(label, { exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "취소", exact: true })).toBeDisabled();
  await page.getByRole("link", { name: "장바구니", exact: true }).click();
  const pendingDialog = page.getByRole("dialog", { name: "상품 문의 등록 중" });
  await expect(pendingDialog.getByRole("button", { name: "내용 버리고 이동", exact: true })).toBeDisabled();
  state.release();
  const failedDialog = page.getByRole("dialog", { name: "문의 작성을 그만둘까요?" });
  await failedDialog.getByRole("button", { name: "계속 작성", exact: true }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByLabel("내용", { exact: true })).toHaveValue("안녕하세요.\n두 줄로 각인할 수 있나요?");
  await expect(page.getByLabel("비밀글", { exact: true })).toBeChecked();
  state.fail = false;
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(2);
  await page.getByRole("link", { name: "장바구니", exact: true }).click();
  await expect(pendingDialog).toBeVisible();
  state.release();
  await expect(page).toHaveURL(/\/cart$/);
  await page.goBack();
  await page.getByRole("button", { name: "내 문의 보기", exact: true }).click();
  const content = page.getByText("안녕하세요. 두 줄로 각인할 수 있나요?", { exact: true });
  const reply = page.getByText("첫 번째 답변 두 번째 답변", { exact: true });
  await expect(content).toHaveCSS("white-space", "pre-wrap");
  await expect(reply).toHaveCSS("white-space", "pre-wrap");
  await content.locator("xpath=ancestor::div[contains(@class, 'card')][1]").screenshot({
    path: testInfo.outputPath("qna-content-lines.png"), animations: "disabled",
  });
  expect(state.requests[1]).toEqual(state.requests[0]);
  expect(state.rows).toHaveLength(1);
});

test("상품 문의 등록 클릭 직후 계정이 바뀌면 이전 입력을 전송하지 않는다", async ({ page }) => {
  const state = await mockQna(page);
  await writeQna(page);
  state.accountId = 502;
  await page.getByLabel("제목", { exact: true }).locator("xpath=ancestor::form").evaluate((form: HTMLFormElement) => {
    form.requestSubmit();
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "문의 작성", exact: true }).click();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("비밀글", { exact: true })).not.toBeChecked();
  expect(state.requests).toEqual([]);
});

test("계정 전환은 작성 내용·이탈 확인을 지우고 이전 등록 응답은 새 문의를 초기화하지 않는다", async ({ page }) => {
  const state = await mockQna(page);
  await writeQna(page);
  state.hold = true;
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  await page.getByRole("link", { name: "장바구니", exact: true }).click();
  await expect(page.getByRole("dialog", { name: "상품 문의 등록 중" })).toBeVisible();
  state.accountId = 502;
  await page.evaluate(() => {
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("button", { name: "문의 작성", exact: true }).click();
  await page.getByLabel("제목", { exact: true }).fill("새 계정 문의");
  const completed = page.waitForResponse((response) => response.request().method() === "POST" && response.url().endsWith("/qna"));
  state.release();
  await (await completed).finished();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("새 계정 문의");
  await expect(page).toHaveURL(/\/products\/42$/);
  await expect(page.getByText("상품 문의를 등록했습니다.", { exact: true })).toHaveCount(0);
});
