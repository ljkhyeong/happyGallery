import { expect, test } from "@playwright/test";

const initialSummary = { id: 51, source: "WEBSITE", status: "CONSULTING", organization: "일정 변경 기관", headcount: 20,
  preferredSchedule: "10월 오전", location: "기관 강당", classInterest: "레진아트", createdAt: "2026-09-05T10:00:00" };

test("회원은 최신 문의 버전으로 일정과 인원을 수정하고 변경 이력을 확인한 뒤 취소한다 @identity", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "XSRF-TOKEN", value: "inquiry-test", url: baseURL! }]);
  const detail = { summary: { ...initialSummary }, version: 1, changes: [] as { id: number; note: string; createdAt: string }[] };
  let stale = true;
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "inquiry@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
    if (path === "/api/v1/me/group-inquiries") return json({ content: [detail.summary], nextCursor: null, hasMore: false });
    if (path === "/api/v1/me/group-inquiries/51") {
      if (route.request().method() === "PUT") {
        const request = route.request().postDataJSON();
        if (stale) { stale = false; detail.version += 1; return json({ code: "CONFLICT", message: "문의가 변경되었습니다. 최신 내용을 확인해 주세요." }, 409); }
        expect(request).toEqual({ version: detail.version, headcount: 30, preferredSchedule: "11월 오후" });
        detail.version += 1;
        detail.summary.headcount = request.headcount; detail.summary.preferredSchedule = request.preferredSchedule;
        detail.changes.unshift({ id: detail.version, note: "참여 인원: 20명 → 30명 / 희망 일정: 10월 오전 → 11월 오후", createdAt: "2026-09-05T11:00:00" });
      }
      return json(detail);
    }
    if (path === "/api/v1/me/group-inquiries/51/cancel") {
      expect(route.request().postDataJSON()).toEqual({ version: detail.version });
      detail.version += 1; detail.summary.status = "CANCELED";
      detail.changes.unshift({ id: detail.version, note: "회원이 문의를 취소했습니다.", createdAt: "2026-09-05T12:00:00" });
      return json(detail);
    }
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    return json([]);
  });
  await page.goto("/my/group-inquiries");
  await page.getByRole("button", { name: "상세·변경 이력" }).click();
  await page.getByLabel("참여 인원", { exact: true }).fill("30");
  await page.getByLabel("희망 일정", { exact: true }).fill("11월 오후");
  await page.getByRole("button", { name: "변경 저장" }).click();
  await expect(page.getByText("문의가 변경되었거나 수정할 수 없는 상태입니다. 최신 문의를 불러와 확인해 주세요.", { exact: true })).toBeVisible();
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("30");
  await page.getByRole("button", { name: "최신 문의 불러오기" }).click();
  await expect(page.getByLabel("참여 인원", { exact: true })).toHaveValue("20");
  await page.getByLabel("참여 인원", { exact: true }).fill("30");
  await page.getByLabel("희망 일정", { exact: true }).fill("11월 오후");
  await page.getByRole("button", { name: "변경 저장" }).click();
  await expect(page.getByText(/참여 인원: 20명 → 30명/)).toBeVisible();
  await page.getByRole("button", { name: "문의 취소", exact: true }).click();
  await page.getByRole("dialog").getByRole("button", { name: "문의 취소 확인" }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(page.getByText("회원이 문의를 취소했습니다.", { exact: true })).toBeVisible();
  await expect(page.getByRole("button", { name: "변경 저장" })).toHaveCount(0);
});

test("관리자 문의 검색은 접수 번호와 날짜 및 경로를 다음 페이지에도 유지한다 @admin", async ({ page }) => {
  await page.addInitScript(() => sessionStorage.setItem("hg_admin_token", "inquiry-admin"));
  const queries: URLSearchParams[] = [];
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    if (url.pathname === "/api/v1/me") return json({ code: "UNAUTHORIZED", message: "로그인 필요" }, 401);
    if (url.pathname === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (url.pathname === "/api/v1/admin/group-inquiries") {
      queries.push(url.searchParams);
      return json({ content: [initialSummary], nextCursor: url.searchParams.has("cursor") ? null : "next-page", hasMore: !url.searchParams.has("cursor") });
    }
    return json({ code: "SERVICE_UNAVAILABLE", message: "검증 대상 외 항목" }, 503);
  });
  await page.goto("/admin?view=support");
  const section = page.locator("#admin-group-inquiries");
  await section.getByLabel("접수 번호", { exact: true }).fill("51");
  await section.getByLabel("문의 경로", { exact: true }).selectOption("WEBSITE");
  await section.getByLabel("접수 시작일", { exact: true }).fill("2026-09-01");
  await section.getByLabel("접수 종료일", { exact: true }).fill("2026-09-05");
  await section.getByRole("button", { name: "검색", exact: true }).click();
  await expect.poll(() => queries.at(-1)?.get("inquiryId")).toBe("51");
  await section.getByRole("button", { name: "다음", exact: true }).click();
  await expect.poll(() => queries.at(-1)?.get("cursor")).toBe("next-page");
  expect(Object.fromEntries(queries.at(-1)!)).toMatchObject({ inquiryId: "51", source: "WEBSITE", from: "2026-09-01", to: "2026-09-05" });
  await section.getByRole("button", { name: "검색 초기화" }).click();
  await expect(section.getByLabel("접수 번호", { exact: true })).toHaveValue("");
  await expect(section.getByRole("button", { name: "이전", exact: true })).toBeDisabled();
});
