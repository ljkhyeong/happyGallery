import { type Page } from "@playwright/test";

export function successAlert(page: Page, text: string) {
  return page.locator(".alert.alert-success").filter({ hasText: text }).first();
}

export function navLogoutButton(page: Page) {
  return page.locator(".app-navbar").getByRole("button", { name: "로그아웃" });
}

export function navLoginLink(page: Page) {
  return page.locator(".app-navbar").getByRole("link", { name: "로그인" });
}

export async function extractCodeText(page: Page): Promise<string> {
  const code = await page.locator(".card code").first().textContent();
  if (!code) {
    throw new Error("Could not find code value in success card");
  }
  return code.trim();
}
