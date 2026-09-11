import { expect, test, type Page } from "@playwright/test";
import type { CreateInquiryRequest, InquiryResponse } from "../../src/generated/api/customerStore";

async function mockInquiries(page: Page) {
  const state = {
    accountId: 501, fail: false, hold: false, release: () => {},
    requests: [] as CreateInquiryRequest[],
    rows: new Map<number, InquiryResponse[]>([[501, []], [502, []]]),
  };
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({
      status, contentType: "application/json", body: JSON.stringify(body),
    });
    const rows = state.rows.get(state.accountId)!;
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({
        contentType: "application/json", headers: { "Set-Cookie": "XSRF-TOKEN=inquiry-test; Path=/" }, body: "{}",
      });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`,
        email: "inquiry@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/inquiries/page": return json({ content: rows, nextCursor: null, hasMore: false });
      case "/api/v1/me/inquiries": {
        const request = route.request().postDataJSON() as CreateInquiryRequest;
        state.requests.push(request);
        if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
        if (state.fail) return json({ code: "SERVICE_UNAVAILABLE" }, 503);
        const row: InquiryResponse = { ...request, id: state.requests.length,
          createdAt: "2026-09-12T10:00:00", hasReply: false, repliedAt: null, replyContent: null };
        rows.unshift(row);
        return json(row, 201);
      }
      default: throw new Error(`정의하지 않은 문의 테스트 요청: ${pathname}`);
    }
  });
  await page.goto("/my/inquiries");
  await page.getByRole("link", { name: "문의 작성", exact: true }).click();
  await expect(page.getByLabel("제목", { exact: true })).toBeVisible();
  return state;
}

async function writeInquiry(page: Page) {
  await page.getByLabel("제목", { exact: true }).fill("수업 준비물 문의");
  await page.getByLabel("내용", { exact: true }).fill("안녕하세요.\n앞치마를 가져가야 하나요?");
}

test("문의 입력이 있을 때만 이동을 확인하고 계속 작성하거나 버리고 나갈 수 있다", async ({ page }, testInfo) => {
  const state = await mockInquiries(page);
  await page.getByRole("button", { name: "취소", exact: true }).click();
  await expect(page).toHaveURL(/\/my\/inquiries$/);
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("link", { name: "문의 작성", exact: true }).click();
  await writeInquiry(page);
  await page.getByRole("link", { name: "내 문의 목록", exact: false }).click();
  const dialog = page.getByRole("dialog", { name: "문의 작성을 그만둘까요?" });
  await expect(dialog).toBeVisible();
  for (const [name, width] of [["desktop", 1280], ["mobile", 390]] as const) {
    await page.setViewportSize({ width, height: 844 });
    await page.screenshot({ path: testInfo.outputPath(`inquiry-leave-${name}.png`), fullPage: true, animations: "disabled" });
  }
  await dialog.getByRole("button", { name: "계속 작성" }).click();
  await expect(page.getByLabel("내용", { exact: true })).toHaveValue("안녕하세요.\n앞치마를 가져가야 하나요?");
  await page.evaluate(() => window.history.back());
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: "나가기", exact: true }).click();
  await expect(page).toHaveURL(/\/my\/inquiries$/);
  await page.getByRole("link", { name: "문의 작성", exact: true }).click();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("");
  expect(state.requests).toEqual([]);
});

test("새로고침 취소 시 입력을 유지하고 내용을 비우면 확인 없이 새로고침한다", async ({ page }) => {
  await mockInquiries(page);
  await writeInquiry(page);
  const dialogPromise = page.waitForEvent("dialog");
  await page.evaluate(() => { window.setTimeout(() => window.location.reload(), 0); });
  const dialog = await dialogPromise;
  expect(dialog.type()).toBe("beforeunload");
  await dialog.dismiss();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("수업 준비물 문의");
  await page.getByLabel("제목", { exact: true }).fill("");
  await page.getByLabel("내용", { exact: true }).fill("");
  const unexpectedDialogs: string[] = [];
  page.on("dialog", async (next) => { unexpectedDialogs.push(next.type()); await next.dismiss(); });
  await page.reload();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("");
  expect(unexpectedDialogs).toEqual([]);
});

test("등록 중 입력·중복 제출·이동을 막고 실패 뒤 재제출 성공 시 목록으로 이동한다", async ({ page }) => {
  const state = await mockInquiries(page);
  state.hold = true;
  state.fail = true;
  await writeInquiry(page);
  await page.locator("form").evaluate((form: HTMLFormElement) => { form.requestSubmit(); form.requestSubmit(); });
  await expect.poll(() => state.requests.length).toBe(1);
  await expect(page.getByLabel("제목", { exact: true })).toBeDisabled();
  await expect(page.getByLabel("내용", { exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "등록 중..." })).toBeDisabled();
  await page.getByRole("link", { name: "내 문의 목록", exact: false }).click();
  const pendingDialog = page.getByRole("dialog", { name: "문의 등록 중" });
  await expect(pendingDialog.getByRole("button", { name: "나가기" })).toBeDisabled();
  state.hold = false;
  state.release();
  await page.getByRole("dialog").getByRole("button", { name: "계속 작성" }).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("수업 준비물 문의");
  state.fail = false;
  state.hold = true;
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(2);
  await page.getByRole("link", { name: "내 문의 목록", exact: false }).click();
  await expect(pendingDialog).toBeVisible();
  state.release();
  await expect(page).toHaveURL(/\/my\/inquiries$/);
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByText("수업 준비물 문의", { exact: true })).toBeVisible();
  expect(state.requests[1]).toEqual(state.requests[0]);
  expect(state.rows.get(501)).toHaveLength(1);
});

test("계정 전환 시 문의 입력·이탈 확인창을 비우고 이전 등록 결과로 이동하지 않는다", async ({ page }) => {
  const state = await mockInquiries(page);
  await writeInquiry(page);
  state.hold = true;
  await page.getByRole("button", { name: "등록", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  await page.getByRole("link", { name: "내 문의 목록", exact: false }).click();
  await expect(page.getByRole("dialog")).toBeVisible();
  state.accountId = 502;
  await page.evaluate(() => {
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  const response = page.waitForResponse((res) => res.url().endsWith("/me/inquiries") && res.request().method() === "POST");
  state.release();
  await response;
  await expect(page.getByLabel("제목", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("내용", { exact: true })).toHaveValue("");
  await expect(page.getByText("문의가 등록되었습니다.", { exact: true })).toHaveCount(0);
  await page.getByRole("button", { name: "취소", exact: true }).click();
  await expect(page).toHaveURL(/\/my\/inquiries$/);
  await expect(page.getByText("등록된 문의가 없습니다.")).toBeVisible();
});

test("문의와 답변의 줄바꿈을 표시하고 긴 문장이 모바일 화면 밖으로 넘치지 않는다", async ({ page }, testInfo) => {
  const state = await mockInquiries(page);
  const content = "준비물 문의입니다.\n앞치마가 필요한가요?\n" + "a".repeat(180);
  const reply = "앞치마는 공방에서 제공합니다.\n편한 복장으로 방문해 주세요.";
  state.rows.get(501)!.push({ id: 7, title: "준비물 안내", content, hasReply: true,
    createdAt: "2026-09-12T10:00:00", repliedAt: "2026-09-12T11:00:00", replyContent: reply });
  await page.goto("/my/inquiries");
  for (const [name, width] of [["desktop", 1280], ["mobile", 390]] as const) {
    await page.setViewportSize({ width, height: 844 });
    await expect(page.getByText(content, { exact: true })).toHaveCSS("white-space", "pre-wrap");
    await expect(page.getByText(reply, { exact: true })).toHaveCSS("white-space", "pre-wrap");
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(width);
    await page.screenshot({ path: testInfo.outputPath(`inquiry-list-${name}.png`), fullPage: true, animations: "disabled" });
  }
});
