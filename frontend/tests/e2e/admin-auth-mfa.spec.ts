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
        recoveryResetAvailable: false,
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

test("P8-MFA-RECOVERY @admin 복구 코드 세션은 현재 비밀번호 확인 후 MFA를 초기화한다", async ({
  page,
}) => {
  let recoveryRequestCount = 0;

  await page.route("**/api/v1/admin/auth/login", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        status: "MFA_REQUIRED",
        token: null,
        challengeToken: "recovery-challenge",
      }),
    });
  });
  await page.route("**/api/v1/admin/auth/mfa/verify", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        status: "AUTHENTICATED",
        token: "recovery-admin-session",
        challengeToken: null,
      }),
    });
  });
  await page.route("**/api/v1/admin/auth/mfa", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        enabled: true,
        enrollmentPending: false,
        recoveryCodesRemaining: 3,
        recoveryResetAvailable: true,
      }),
    });
  });
  await page.route("**/api/v1/admin/auth/mfa/recovery", async (route) => {
    expect(route.request().headers().authorization)
      .toBe("Bearer recovery-admin-session");
    expect(route.request().postDataJSON()).toEqual({
      currentPassword: "admin1234",
    });
    recoveryRequestCount++;
    await route.fulfill({ status: 204 });
  });

  await page.goto("/admin");
  await page.getByLabel("아이디").fill("admin");
  await page.getByLabel("비밀번호").fill("admin1234");
  await page.getByRole("button", { name: "로그인" }).click();

  await page.getByLabel("인증 코드 또는 복구 코드").fill("aaaa-bbbb-cccc-0001");
  await page.getByRole("button", { name: "확인", exact: true }).click();

  await expect(page.getByRole("button", { name: "복구 진행" })).toBeVisible();
  await page.getByRole("button", { name: "복구 진행" }).click();

  const mfaPanel = page.locator("section").filter({
    has: page.getByRole("heading", { name: "2단계 인증", exact: true }),
  });
  await mfaPanel.getByLabel("현재 비밀번호").fill("admin1234");
  await mfaPanel.getByRole("button", { name: "2단계 인증 초기화" }).click();

  await expect(page.getByRole("heading", { name: "관리자 로그인" })).toBeVisible();
  await expect(page.getByText(
    "2단계 인증이 초기화되었습니다. 다시 로그인해 인증 앱을 등록해 주세요.",
  )).toBeVisible();
  expect(recoveryRequestCount).toBe(1);
});
