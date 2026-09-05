import { expect, test, type Locator } from "@playwright/test";
import { clearSsrUpstreamFixtures, replaceSsrUpstreamFixtures, ssrApiFixture } from "./ssr-upstream-fixture";

const details = { organization: "단체 수업 기관", contactName: "김담당", phone: "01012345678", email: null,
  headcount: 20, preferredSchedule: "10월 평일 오전", location: "기관 강당", classInterest: "레진아트", message: null };
const summary = { id: 51, source: "WEBSITE", status: "RECEIVED", organization: details.organization, headcount: 20,
  preferredSchedule: details.preferredSchedule, location: details.location, classInterest: details.classInterest, createdAt: "2026-09-05T10:00:00" };

async function fillInquiry(form: Locator) {
  await form.getByLabel("기관·모임명", { exact: true }).fill(details.organization);
  await form.getByLabel("담당자 이름", { exact: true }).fill(details.contactName);
  await form.getByLabel("담당자 휴대폰", { exact: true }).fill(details.phone);
  await form.getByLabel("희망 일정", { exact: true }).fill(details.preferredSchedule);
  await form.getByLabel("수업 장소", { exact: true }).fill(details.location);
  await form.getByLabel("관심 수업", { exact: true }).fill(details.classInterest);
  await form.getByLabel("참여 인원", { exact: true }).fill("20");
}

test.afterEach(clearSsrUpstreamFixtures);

for (const member of [false, true]) {
  test(`${member ? "회원" : "비회원"}이 단체 수업 문의를 접수한다`, async ({ page, context, baseURL }) => {
    await context.addCookies([{ name: "XSRF-TOKEN", value: "group-csrf", url: baseURL! }]);
    await replaceSsrUpstreamFixtures(ssrApiFixture("/workshop", { name: "해피갤러리" }));
    let submitted = false;
    await page.route("**/api/v1/**", async (route) => {
      const path = new URL(route.request().url()).pathname;
      const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
      if (path === "/api/v1/workshop") return route.fallback();
      if (path === "/api/v1/me") return member
        ? json({ id: 501, name: "회원", email: "group@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true })
        : json({ code: "UNAUTHORIZED", message: "로그인 필요" }, 401);
      if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
      if (path.endsWith("/group-inquiries")) {
        if (route.request().method() === "POST") {
          expect(path).toBe(member ? "/api/v1/me/group-inquiries" : "/api/v1/group-inquiries");
          expect(route.request().postDataJSON()).toMatchObject({ ...details, email: member ? "group@example.com" : null });
          submitted = true;
          return json({ id: 51, status: "RECEIVED" }, 201);
        }
        return json({ content: submitted ? [summary] : [], hasMore: false, nextCursor: null });
      }
      if (path.endsWith("/page")) return json({ content: [], hasMore: false, nextCursor: null });
      return json([]);
    });
    await page.goto("/group-classes");
    const form = page.locator("#group-inquiry-form");
    await fillInquiry(form);
    await form.getByRole("button", { name: "단체 수업 문의 접수", exact: true }).click();
    await expect(form.getByText("문의가 접수되었습니다.", { exact: true })).toBeVisible();
    if (member) {
      await form.getByRole("link", { name: "내 문의 상태 확인" }).click();
      await expect(page.locator("#my-group-inquiries").getByText(details.organization, { exact: true })).toBeVisible();
      await expect(page.locator("#my-group-inquiries").getByText("접수", { exact: true })).toBeVisible();
    }
  });
}

test("관리자가 외부 문의를 등록하고 상담 상태와 메모를 저장한다", async ({ page }) => {
  await page.addInitScript(() => sessionStorage.setItem("hg_admin_token", "group-admin"));
  let created = false;
  let version = 0;
  let status = "RECEIVED";
  let nextContactOn: string | null = null;
  const activities: { id: number; adminId: number; fromStatus: string; toStatus: string; note: string; createdAt: string }[] = [];
  const detail = () => ({ summary: { ...summary, source: "EXTERNAL", status }, details, version, nextContactOn, activities });
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown, responseStatus = 200) => route.fulfill({ status: responseStatus, contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/me") return json({ code: "UNAUTHORIZED", message: "로그인 필요" }, 401);
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (path === "/api/v1/admin/group-inquiries") {
      if (route.request().method() === "POST") {
        expect(route.request().postDataJSON()).toEqual(details);
        created = true;
        return json(detail(), 201);
      }
      return json({ content: created ? [detail().summary] : [], hasMore: false, nextCursor: null });
    }
    if (path === "/api/v1/admin/group-inquiries/follow-ups") return json({
      content: nextContactOn ? [{ id: 51, organization: details.organization, status, nextContactOn }] : [], nextCursor: null, hasMore: false,
    });
    if (path === "/api/v1/admin/group-inquiries/51/next-contact") {
      const request = route.request().postDataJSON();
      expect(request.version).toBe(version);
      version += 1; nextContactOn = request.nextContactOn;
      return json(detail());
    }
    if (path === "/api/v1/admin/group-inquiries/51") {
      if (route.request().method() === "PUT") {
        const request = route.request().postDataJSON();
        expect(request.version).toBe(version);
        expect(request.note).toBe("10월 오전 일정 협의");
        const previous = status;
        version += 1; status = request.status;
        activities.unshift({ id: version, adminId: 99, fromStatus: previous, toStatus: status, note: request.note, createdAt: "2026-09-05T10:01:00" });
      }
      return json(detail());
    }
    return json({ code: "SERVICE_UNAVAILABLE", message: "이 테스트의 대상이 아닌 운영 항목입니다." }, 503);
  });
  await page.goto("/admin?view=support");
  const section = page.locator("#admin-group-inquiries");
  await section.getByRole("button", { name: "외부 문의 등록", exact: true }).click();
  await fillInquiry(section);
  await section.getByRole("button", { name: "외부 문의 저장", exact: true }).click();
  await expect(section.getByText(/김담당 · 01012345678/)).toBeVisible();
  await section.getByLabel("상담 상태", { exact: true }).selectOption("CONSULTING");
  await section.getByLabel("상담 메모", { exact: true }).fill("10월 오전 일정 협의");
  await section.getByRole("button", { name: "상담 저장", exact: true }).click();
  await expect(section.getByText("10월 오전 일정 협의", { exact: true })).toBeVisible();
  await expect(section.getByLabel("상담 메모", { exact: true })).toHaveValue("");
  await expect(section.getByLabel("상담 상태", { exact: true })).toHaveValue("CONSULTING");
  await section.getByLabel("다음 연락일", { exact: true }).fill("2000-01-01");
  await section.getByRole("button", { name: "연락일 저장", exact: true }).click();
  await expect.poll(() => nextContactOn).toBe("2000-01-01");
  await page.goto("/admin?view=today");
  const followUps = page.locator("#group-inquiry-follow-ups");
  await expect(followUps.getByText("연락 예정일 2000-01-01", { exact: true })).toBeVisible();
  await followUps.getByRole("button", { name: "상담 열기", exact: true }).click();
  await followUps.getByLabel("다음 연락일", { exact: true }).fill("");
  await followUps.getByRole("button", { name: "연락일 저장", exact: true }).click();
  await expect(followUps.getByText("오늘까지 연락할 단체 문의가 없습니다.", { exact: true })).toBeVisible();

});
