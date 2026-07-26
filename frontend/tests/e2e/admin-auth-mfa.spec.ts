import { expect, test } from "@playwright/test";

test("P8-MFA @admin 운영 MFA 미등록 관리자는 등록과 복구 코드 확인만 수행한다", async ({ page }) => {
  await page.route("**/api/v1/admin/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        status: "AUTHENTICATED",
        token: "limited-admin-session",
        challengeToken: null,
      }),
    });
  });
  await page.route("**/api/v1/admin/auth/mfa", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        enabled: false,
        enrollmentPending: false,
        recoveryCodesRemaining: 0,
      }),
    });
  });
  await page.route("**/api/v1/admin/auth/mfa/enrollment", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        secret: "JBSWY3DPEHPK3PXP",
        provisioningUri: "otpauth://totp/happyGallery:admin?secret=JBSWY3DPEHPK3PXP",
      }),
    });
  });
  await page.route("**/api/v1/admin/auth/mfa/enrollment/confirm", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        recoveryCodes: [
          "aaaa-bbbb-cccc-0001",
          "aaaa-bbbb-cccc-0002",
        ],
      }),
    });
  });

  await page.goto("/admin");
  await page.getByLabel("아이디").fill("admin");
  await page.getByLabel("비밀번호").fill("admin1234");
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page.getByRole("heading", { name: "관리자 2단계 인증 등록" })).toBeVisible();
  await expect(page.getByRole("tab", { name: "오늘 할 일" })).toHaveCount(0);
  await page.getByRole("button", { name: "2단계 인증 설정" }).click();
  await expect(page.getByText("JBSWY3DPEHPK3PXP")).toBeVisible();

  await page.getByLabel("인증 앱의 6자리 코드").fill("123456");
  await page.getByRole("button", { name: "등록 확인" }).click();

  await expect(page.getByText("aaaa-bbbb-cccc-0001")).toBeVisible();
  await expect(page.getByRole("button", { name: "보관 완료, 다시 로그인" })).toBeVisible();
});
