import { expect, test, type Page } from "@playwright/test";

async function mockAccount(page: Page, signedIn = false) {
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/auth/csrf") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        headers: { "set-cookie": "XSRF-TOKEN=account-guidance; Path=/; SameSite=Lax" },
        body: JSON.stringify({ cookieName: "XSRF-TOKEN", headerName: "X-XSRF-TOKEN" }),
      });
      return;
    }
    let status = 200;
    let body: unknown = [];
    if (pathname === "/api/v1/me") {
      if (route.request().method() === "DELETE") {
        status = 422;
        body = { code: "ACCOUNT_WITHDRAWAL_BLOCKED", message: "사용 가능한 이용권이 있습니다.\n처리 중인 환불이 있습니다." };
      } else if (signedIn) {
        body = { id: 101, email: "guidance@example.com", name: "안내 확인 회원", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true };
      } else {
        status = 401;
        body = { code: "UNAUTHORIZED", message: "로그인이 필요합니다." };
      }
    } else if (pathname === "/api/v1/policies/current") {
      body = {
        terms: { version: "2026-07", documentPath: "/terms/2026-07" },
        privacy: { version: "2026-07", documentPath: "/privacy/2026-07" },
      };
    } else if (pathname === "/api/v1/workshop") {
      body = { name: "해피갤러리" };
    } else if (pathname === "/api/v1/me/cart") {
      body = { cartVersion: "0".repeat(64), items: [], totalAmount: 0 };
    } else if (pathname === "/api/v1/me/notifications/unread-count") {
      body = { count: 0 };
    } else if (pathname === "/api/v1/me/social-accounts") {
      body = { linkedProviders: [] };
    } else if (pathname.endsWith("/page")) {
      body = { content: [], hasMore: false, nextCursor: null };
    }
    await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
  });
}

for (const [path, label] of [["/signup", "비밀번호"], ["/forgot-password", "새 비밀번호"]]) {
  test(`@identity ${path}에서 한글 비밀번호의 길이 초과를 즉시 안내한다`, async ({ page }) => {
    await mockAccount(page);
    await page.goto(path);
    const password = page.getByLabel(label, { exact: true });
    const feedback = page.getByRole("alert").filter({ hasText: "비밀번호가 너무 깁니다" });
    await password.fill("가".repeat(25));
    await expect(feedback).toBeVisible();
    await password.fill("가".repeat(24));
    await expect(feedback).toHaveCount(0);
    await password.fill("😀".repeat(19));
    await expect(feedback).toBeVisible();
    await password.fill("😀".repeat(18));
    await expect(feedback).toHaveCount(0);
  });
}

test("@identity 탈퇴 제한 사유만 목록으로 표시하고 로그인 상태를 유지한다", async ({ page }) => {
  await mockAccount(page, true);
  await page.goto("/my");
  await page.getByRole("button", { name: "탈퇴", exact: true }).click();
  const modal = page.getByRole("dialog", { name: "회원 탈퇴" });
  await modal.getByRole("textbox").fill("탈퇴");
  await modal.getByRole("button", { name: "회원 탈퇴", exact: true }).click();
  const reasons = modal.getByRole("alert").getByRole("listitem");
  await expect(reasons).toHaveText(["사용 가능한 이용권이 있습니다.", "처리 중인 환불이 있습니다."]);
  await expect(modal.getByRole("button", { name: "회원 탈퇴", exact: true })).toBeEnabled();
  await modal.getByRole("button", { name: "취소", exact: true }).click();
  await expect(page.getByText("guidance@example.com").first()).toBeVisible();
  await expect(page).toHaveURL(/\/my$/);
});
