import { expect, test } from "@playwright/test";
import { TERMS_POLICY_VERSION, PRIVACY_POLICY_VERSION } from "../../src/features/policy-consent/policyVersions";
import { clearSsrUpstreamFixtures, homeSsrFixtures, replaceSsrUpstreamFixtures } from "./ssr-upstream-fixture";
import { fulfillJson, installCsrfCookie } from "./mock-support";

test.afterEach(async () => { await clearSsrUpstreamFixtures(); });

for (const provider of ["naver", "kakao", "google"] as const) {
  for (const mobile of [false, true]) {
    test(`@identity ${provider} 신규 가입은 제공된 번호 유무에 따라 다음 화면으로 이동한다 (${mobile ? "모바일" : "데스크톱"})`, async ({ page, context, baseURL }) => {
      if (mobile) await page.setViewportSize({ width: 390, height: 844 });
      await replaceSsrUpstreamFixtures(...homeSsrFixtures({ workshop: { name: "해피갤러리" } }));
      await installCsrfCookie(context, baseURL, "social-consent");
      let signedUp = false;
      let authorizations = 0;
      let completions = 0;
      await page.route("**/api/v1/**", async (route) => {
        const { pathname } = new URL(route.request().url());
        if (pathname === `/api/v1/auth/social/authorization/${provider}`) {
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
          return fulfillJson(route, { id: 91, name: "소셜 신규 회원", email: null, phone: provider === "google" ? null : "01081234567", phoneVerified: false, localPasswordEnabled: false });
        }
        if (pathname === "/api/v1/me") return signedUp
          ? fulfillJson(route, { id: 91, name: "소셜 신규 회원", email: null, phone: provider === "google" ? null : "01081234567", phoneVerified: false, localPasswordEnabled: false })
          : fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
        if (pathname === "/api/v1/policies/current") return fulfillJson(route, {
          terms: { version: TERMS_POLICY_VERSION, documentPath: `/terms/${TERMS_POLICY_VERSION}` },
          privacy: { version: PRIVACY_POLICY_VERSION, documentPath: `/privacy/${PRIVACY_POLICY_VERSION}` },
        });
        if (pathname === "/api/v1/workshop") return fulfillJson(route, { name: "해피갤러리" });
        return fulfillJson(route, []);
      });
      await page.goto(`/login?${new URLSearchParams({ redirect: "/?from=social" })}`);
      await page.getByRole("button", { name: `${provider === "naver" ? "네이버" : provider === "kakao" ? "카카오" : "Google"}로 로그인`, exact: true }).click();
      await expect(page.getByRole("heading", { name: "동의하고 가입 완료" })).toBeVisible();
      const submit = page.getByRole("button", { name: "동의하고 시작하기" });
      await expect(submit).toBeDisabled();
      expect(completions).toBe(0);
      await page.getByRole("checkbox").check();
      await expect(submit).toBeEnabled();
      await page.screenshot({ path: `/tmp/hg-social-consent-${provider}-${mobile ? "mobile" : "desktop"}.png` });
      await submit.click();
      if (provider === "google") {
        await expect(page).toHaveURL(/\/my$/);
        await expect(page.getByRole("dialog")).toContainText("휴대폰 번호 등록");
      } else {
        await expect(page).toHaveURL(/\/\?from=social$/);
        await expect(page.getByRole("dialog")).toHaveCount(0);
      }
      expect(authorizations).toBe(1);
      expect(completions).toBe(1);
      expect(await page.evaluate(() => sessionStorage.getItem("social_login_return_to"))).toBeNull();
    });
  }
}

for (const errorCode of ["SOCIAL_LOGIN_FAILED", "SOCIAL_PHONE_REQUIRED", "SOCIAL_ACCOUNT_LINK_REQUIRED"]) {
  test(`@identity 가입 오류 ${errorCode}는 재로그인을 안내하고 재제출하지 않는다`, async ({ page, context, baseURL }) => {
    await installCsrfCookie(context, baseURL);
    await page.route("**/api/v1/**", async (route) => {
      const { pathname } = new URL(route.request().url());
      if (pathname === "/api/v1/auth/social/signup-completion") return fulfillJson(route, { code: errorCode, message: "다시 로그인해 주세요." }, errorCode === "SOCIAL_LOGIN_FAILED" ? 401 : errorCode === "SOCIAL_ACCOUNT_LINK_REQUIRED" ? 409 : 422);
      if (pathname === "/api/v1/me") return fulfillJson(route, { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }, 401);
      if (pathname === "/api/v1/policies/current") return fulfillJson(route, {
        terms: { version: TERMS_POLICY_VERSION, documentPath: `/terms/${TERMS_POLICY_VERSION}` },
        privacy: { version: PRIVACY_POLICY_VERSION, documentPath: `/privacy/${PRIVACY_POLICY_VERSION}` },
      });
      if (pathname === "/api/v1/workshop") return fulfillJson(route, { name: "해피갤러리" });
      return fulfillJson(route, []);
    });
    await page.goto("/auth/callback?signupAttempt=expired-attempt");
    await page.getByRole("checkbox").check();
    await page.getByRole("button", { name: "동의하고 시작하기" }).click();
    await expect(page.getByRole("link", { name: "다시 로그인하기" })).toBeVisible();
    await expect(page.getByRole("button", { name: "동의하고 시작하기" })).toBeDisabled();
  });

}
