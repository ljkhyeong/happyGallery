import { expect, test, type Page } from "@playwright/test";
import type { MyGroupInquiryResponse } from "../../src/generated/api/customerStore";

function inquiry(id: number, organization: string): MyGroupInquiryResponse {
  return {
    version: 1, changes: [],
    summary: { id, organization, source: "WEBSITE", status: "CONSULTING", headcount: 20,
      preferredSchedule: "10월 오전", location: "기관 강당", classInterest: "가죽공예", createdAt: "2026-09-12T10:00:00" },
  };
}

async function mockInquiries(page: Page) {
  const state = {
    accountId: 501, failRead: false, failWrite: false, hold: false, release: () => {}, readCount: 0,
    requests: [] as Array<{ id: number; action: string; version: number; headcount?: number; preferredSchedule?: string }>,
    accounts: new Map([
      [501, new Map([[51, inquiry(51, "첫 번째 기관")], [52, inquiry(52, "두 번째 기관")]])],
      [502, new Map([[151, inquiry(151, "새 회원 기관")]])],
    ]),
  };
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    const details = state.accounts.get(state.accountId)!;
    const match = pathname.match(/^\/api\/v1\/me\/group-inquiries\/(\d+)(\/cancel)?$/);
    if (match) {
      const id = Number(match[1]);
      const detail = details.get(id);
      if (!detail) return json({ code: "NOT_FOUND" }, 404);
      if (request.method() === "GET") {
        state.readCount += 1;
        return state.failRead ? json({ code: "SERVICE_UNAVAILABLE" }, 503) : json(detail);
      }
      const body = request.postDataJSON();
      const action = match[2] ? "cancel" : "update";
      state.requests.push({ id, action, ...body });
      if (state.failWrite) {
        if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
        return json({ code: "SERVICE_UNAVAILABLE" }, 503);
      }
      if (body.version !== detail.version) return json({ code: "CONFLICT" }, 409);
      detail.version += 1;
      if (action === "cancel") detail.summary.status = "CANCELED";
      else {
        detail.summary.headcount = body.headcount;
        detail.summary.preferredSchedule = body.preferredSchedule;
      }
      detail.changes.unshift({ id: detail.version, note: action === "cancel" ? "문의 취소 완료" : "일정과 인원 변경 완료",
        createdAt: "2026-09-12T11:00:00" });
      const response = structuredClone(detail);
      if (state.hold) await new Promise<void>((resolve) => { state.release = resolve; });
      return json(response);
    }
    switch (pathname) {
      case "/api/v1/auth/csrf": return route.fulfill({ contentType: "application/json",
        headers: { "Set-Cookie": "XSRF-TOKEN=inquiry-draft; Path=/" }, body: "{}" });
      case "/api/v1/me": return json({ id: state.accountId, name: `회원${state.accountId}`,
        email: "group@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/group-inquiries": return json({ content: Array.from(details.values()).map((detail) => detail.summary),
        hasMore: false, nextCursor: null });
      default: throw new Error(`정의하지 않은 단체 문의 테스트 요청: ${pathname}`);
    }
  });
  await page.goto("/my/group-inquiries?inquiryId=51");
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("20");
  return state;
}

async function editInquiry(page: Page) {
  await page.getByLabel("참여 인원", { exact: true }).fill("30");
  await page.getByLabel("희망 일정", { exact: true }).fill("11월 오후");
}

async function reconnect(page: Page) {
  await page.clock.fastForward(31_000);
  await page.evaluate(() => window.dispatchEvent(new Event("offline")));
  await page.evaluate(() => window.dispatchEvent(new Event("online")));
}

test("재조회로 문의가 바뀌어도 입력을 보존하고 최신 내용 조회가 성공할 때만 교체한다", async ({ page }, testInfo) => {
  const state = await mockInquiries(page);
  await page.clock.install();
  await editInquiry(page);
  const detail = state.accounts.get(501)!.get(51)!;
  detail.version = 2;
  detail.summary.headcount = 25;
  detail.summary.preferredSchedule = "10월 오후";
  await reconnect(page);
  await expect.poll(() => state.readCount).toBe(2);
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("30");
  await expect(page.getByLabel("희망 일정", { exact: true })).toHaveValue("11월 오후");
  await expect(page.getByRole("button", { name: "변경 저장", exact: true })).toBeDisabled();
  for (const width of [1280, 390]) {
    await page.setViewportSize({ width, height: 844 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`inquiry-conflict-${width}.png`), fullPage: true, animations: "disabled" });
  }
  state.failRead = true;
  await page.getByRole("button", { name: "최신 내용 불러오기", exact: true }).click();
  await expect(page.getByRole("button", { name: "문의 조회 다시 시도", exact: true })).toBeEnabled();
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("30");
  state.failRead = false;
  await page.getByRole("button", { name: "최신 내용 불러오기", exact: true }).click();
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("25");
  await expect(page.getByLabel("희망 일정", { exact: true })).toHaveValue("10월 오후");
  await editInquiry(page);
  await page.getByRole("button", { name: "변경 저장", exact: true }).click();
  await expect(page.getByText("일정과 인원 변경 완료", { exact: true })).toBeVisible();
  expect(state.requests).toEqual([{ id: 51, action: "update", version: 2, headcount: 30, preferredSchedule: "11월 오후" }]);
});

test("저장 전 다른 문의 열기와 새로고침을 확인하고 선택한 문의를 URL에 보존한다", async ({ page }, testInfo) => {
  await mockInquiries(page);
  await editInquiry(page);
  const selectOther = page.locator(".my-list-card").filter({ hasText: "두 번째 기관" }).getByRole("button", { name: "상세·변경 이력" });
  await selectOther.click();
  const leave = page.getByRole("dialog", { name: "문의 수정을 그만하시겠어요?" });
  await expect(leave).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({ path: testInfo.outputPath("inquiry-leave-mobile.png"), fullPage: true, animations: "disabled" });
  await leave.getByRole("button", { name: "계속 수정" }).click();
  await expect(page).toHaveURL(/inquiryId=51$/);
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("30");
  const dismissedReload = page.waitForEvent("dialog").then(async (dialog) => {
    expect(dialog.type()).toBe("beforeunload");
    await dialog.dismiss();
  });
  await page.evaluate(() => { setTimeout(() => location.reload(), 0); });
  await dismissedReload;
  await expect(page.getByLabel("희망 일정", { exact: true })).toHaveValue("11월 오후");
  await selectOther.click();
  await leave.getByRole("button", { name: "변경 버리고 이동" }).click();
  await expect(page).toHaveURL(/inquiryId=52$/);
  await expect(page.getByRole("heading", { name: "문의 상세 · 접수 번호 52" })).toBeVisible();
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("20");
  await page.reload();
  await expect(page.getByRole("heading", { name: "문의 상세 · 접수 번호 52" })).toBeVisible();
  await expect(page.getByRole("dialog")).toHaveCount(0);
});

test("저장·취소 실패를 같은 내용으로 재시도하고 처리 중 새로고침과 이동을 막는다", async ({ page }) => {
  const state = await mockInquiries(page);
  await editInquiry(page);
  state.failWrite = true;
  state.hold = true;
  await page.getByRole("button", { name: "변경 저장", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  await expect(page.getByRole("button", { name: "문의 새로고침" })).toBeDisabled();
  await expect(page.getByLabel("참여 인원", { exact: true })).toBeDisabled();
  await page.locator(".my-list-card").filter({ hasText: "두 번째 기관" }).getByRole("button").click();
  const leave = page.getByRole("dialog", { name: "문의 수정을 그만하시겠어요?" });
  await expect(leave.getByRole("button", { name: "변경 버리고 이동" })).toBeDisabled();
  await leave.getByRole("button", { name: "계속 수정" }).click();
  state.hold = false;
  state.release();
  await expect(page.getByRole("button", { name: "변경 저장 다시 시도" })).toBeVisible();
  state.failWrite = false;
  await page.getByRole("button", { name: "변경 저장 다시 시도" }).click();
  await expect(page.getByText("일정과 인원 변경 완료", { exact: true })).toBeVisible();
  state.failWrite = true;
  await page.getByRole("button", { name: "문의 취소", exact: true }).click();
  const cancel = page.getByRole("dialog", { name: "단체 수업 문의 취소" });
  await cancel.getByRole("button", { name: "문의 취소 확인" }).click();
  await expect(cancel.getByRole("button", { name: "문의 취소 다시 시도" })).toBeVisible();
  state.failWrite = false;
  await cancel.getByRole("button", { name: "문의 취소 다시 시도" }).click();
  await expect(cancel).toHaveCount(0);
  await expect(page.getByText("문의 취소 완료", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "변경 저장", exact: true })).toHaveCount(0);
  expect(state.requests.map(({ action, version }) => [action, version])).toEqual([["update", 1], ["update", 1], ["cancel", 2], ["cancel", 2]]);
});

test("늦게 도착한 저장 응답이 더 최신인 문의 상태를 덮어쓰지 않는다", async ({ page }) => {
  const state = await mockInquiries(page);
  await page.clock.install();
  await page.clock.fastForward(31_000);
  await editInquiry(page);
  state.hold = true;
  await page.getByRole("button", { name: "변경 저장", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  const detail = state.accounts.get(501)!.get(51)!;
  detail.version = 3;
  detail.summary.status = "CONFIRMED";
  detail.summary.headcount = 40;
  detail.summary.preferredSchedule = "12월 확정 일정";
  await page.evaluate(() => window.dispatchEvent(new Event("offline")));
  await page.evaluate(() => window.dispatchEvent(new Event("online")));
  await expect.poll(() => state.readCount).toBe(2);
  await expect(page.locator(".card.mt-4").getByText("확정", { exact: true })).toBeVisible();
  const completed = page.waitForResponse((response) => response.request().method() === "PUT");
  state.release();
  await completed;
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("40");
  await expect(page.getByLabel("희망 일정", { exact: true })).toHaveValue("12월 확정 일정");
  await expect(page.getByRole("button", { name: "변경 저장", exact: true })).toHaveCount(0);
});

test("계정 전환 뒤 이전 문의 저장 응답이 새 회원 입력을 바꾸지 않는다", async ({ page }) => {
  const state = await mockInquiries(page);
  await editInquiry(page);
  state.hold = true;
  await page.getByRole("button", { name: "변경 저장", exact: true }).click();
  await expect.poll(() => state.requests.length).toBe(1);
  state.accountId = 502;
  await page.evaluate(() => {
    const value = JSON.stringify({ epoch: crypto.randomUUID(), customerId: 502 });
    localStorage.setItem("hg_customer_session_boundary", value);
    window.dispatchEvent(new StorageEvent("storage", { key: "hg_customer_session_boundary", newValue: value }));
  });
  await expect(page.getByRole("link", { name: "회원502", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "상세·변경 이력", exact: true }).click();
  await expect(page).toHaveURL(/inquiryId=151$/);
  await page.getByLabel("희망 일정", { exact: true }).fill("새 회원이 입력한 일정");
  const completed = page.waitForResponse((response) => response.request().method() === "PUT");
  state.release();
  await completed;
  await expect(page.getByLabel("희망 일정", { exact: true })).toHaveValue("새 회원이 입력한 일정");
  await expect(page.getByText("희망 일정과 참여 인원을 저장했습니다.", { exact: true })).toHaveCount(0);
});
