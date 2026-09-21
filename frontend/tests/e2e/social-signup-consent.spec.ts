import { expect, test } from "@playwright/test";
import { TERMS_POLICY_VERSION, PRIVACY_POLICY_VERSION } from "../../src/features/policy-consent/policyVersions";
import { clearSsrUpstreamFixtures, homeSsrFixtures, replaceSsrUpstreamFixtures } from "./ssr-upstream-fixture";

test.afterEach(async () => { await clearSsrUpstreamFixtures(); });

for (const mobile of [false, true]) {
  test(`@identity 소셜 신규 가입은 인증 반복 없이 동의 후 원래 화면으로 돌아간다 (${mobile ? "모바일" : "데스크톱"})`, async ({ page, context, baseURL }) => {
    if (mobile) await page.setViewportSize({ width: 390, height: 844 });
    await replaceSsrUpstreamFixtures(...homeSsrFixtures({ workshop: { name: "해피갤러리" } }));
    await context.addCookies([{ name: "XSRF-TOKEN", value: "social-consent", url: baseURL! }]);
    let signedUp = false;
    let authorizations = 0;
    let completions = 0;
    await page.route("**/api/v1/**", async (route) => {
      const { pathname } = new URL(route.request().url());
      const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
      if (pathname === "/api/v1/auth/social/authorization/naver") {
        authorizations++;
        return route.fulfill({ status: 302, headers: { location: "/auth/callback?signupAttempt=verified-naver-attempt" } });
      }
      if (pathname === "/api/v1/auth/social/signup-completion") {
        completions++;
        expect(route.request().postDataJSON()).toEqual({
          attemptId: "verified-naver-attempt",
          policyAcceptance: { termsVersion: TERMS_POLICY_VERSION, termsAccepted: true, privacyVersion: PRIVACY_POLICY_VERSION, privacyAccepted: true },
        });
        expect(route.request().headers()["x-xsrf-token"]).toBe("social-consent");
        signedUp = true;
        return json({ id: 91, name: "소셜 신규 회원", email: null, phone: null, phoneVerified: false, localPasswordEnabled: false });
      }
      if (pathname === "/api/v1/me") return signedUp
        ? json({ id: 91, name: "소셜 신규 회원", email: null, phone: null, phoneVerified: false, localPasswordEnabled: false })
        : json({ code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      if (pathname === "/api/v1/policies/current") return json({
        terms: { version: TERMS_POLICY_VERSION, documentPath: `/terms/${TERMS_POLICY_VERSION}` },
        privacy: { version: PRIVACY_POLICY_VERSION, documentPath: `/privacy/${PRIVACY_POLICY_VERSION}` },
      });
      if (pathname === "/api/v1/workshop") return json({ name: "해피갤러리" });
      return json([]);
    });
    await page.goto(`/login?${new URLSearchParams({ redirect: "/?from=social" })}`);
    await page.getByRole("button", { name: "네이버로 로그인", exact: true }).click();
    await expect(page.getByRole("heading", { name: "동의하고 가입 완료" })).toBeVisible();
    const submit = page.getByRole("button", { name: "동의하고 시작하기" });
    await expect(submit).toBeDisabled();
    expect(completions).toBe(0);
    await page.getByRole("checkbox").check();
    await expect(submit).toBeEnabled();
    await page.screenshot({ path: `/tmp/hg-social-consent-${mobile ? "mobile" : "desktop"}.png` });
    await submit.click();
    await expect(page).toHaveURL(/\/\?from=social$/);
    expect(authorizations).toBe(1);
    expect(completions).toBe(1);
    expect(await page.evaluate(() => sessionStorage.getItem("social_login_return_to"))).toBeNull();
  });
}

test("@identity 만료된 소셜 가입은 재로그인을 안내하고 재제출하지 않는다", async ({ page, context, baseURL }) => {
  await context.addCookies([{ name: "XSRF-TOKEN", value: "social-expired", url: baseURL! }]);
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    if (pathname === "/api/v1/auth/social/signup-completion") return json({ code: "SOCIAL_LOGIN_FAILED", message: "다시 로그인해 주세요." }, 401);
    if (pathname === "/api/v1/me") return json({ code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
    if (pathname === "/api/v1/policies/current") return json({
      terms: { version: TERMS_POLICY_VERSION, documentPath: `/terms/${TERMS_POLICY_VERSION}` },
      privacy: { version: PRIVACY_POLICY_VERSION, documentPath: `/privacy/${PRIVACY_POLICY_VERSION}` },
    });
    if (pathname === "/api/v1/workshop") return json({ name: "해피갤러리" });
    return json([]);
  });
  await page.goto("/auth/callback?signupAttempt=expired-attempt");
  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: "동의하고 시작하기" }).click();
  await expect(page.getByRole("link", { name: "다시 로그인하기" })).toBeVisible();
  await expect(page.getByRole("button", { name: "동의하고 시작하기" })).toBeDisabled();
});
