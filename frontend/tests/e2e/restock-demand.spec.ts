import { expect, test } from "@playwright/test";

test("관리자는 재입고 대기 인원을 페이지로 확인하고 재고 관리로 이동한다", async ({ page }) => {
  await page.addInitScript(() => sessionStorage.setItem("hg_admin_token", "restock-admin"));
  await page.route("**/api/v1/**", async (route) => {
    const url = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    if (url.pathname === "/api/v1/me") return json({ code: "UNAUTHORIZED", message: "로그인 필요" }, 401);
    if (url.pathname === "/api/v1/admin/restock-demand") {
      const pageNumber = Number(url.searchParams.get("page") ?? 0);
      return json({ content: [{ productId: pageNumber ? 43 : 42, productName: pageNumber ? "두번째 상품" : "레진 키링",
        productVariantId: pageNumber ? null : 51, optionLabel: pageNumber ? "기본 상품" : "색상: 파랑", waitingCount: pageNumber ? 2 : 9 }],
      page: pageNumber, size: 20, totalCount: 21, totalPages: 2 });
    }
    return json({ code: "SERVICE_UNAVAILABLE", message: "다른 운영 항목" }, 503);
  });
  await page.goto("/admin?view=products");
  const section = page.locator("#admin-restock-demand");
  await expect(section.getByText("9명", { exact: true })).toBeVisible();
  await expect(section.getByText("색상: 파랑", { exact: true })).toBeVisible();
  await expect(section.getByRole("link", { name: "재고 확인" })).toHaveAttribute("href", "/admin?view=products&productId=42&variantId=51");
  await section.getByRole("button", { name: "다음", exact: true }).click();
  await expect(section.getByText("두번째 상품", { exact: true })).toBeVisible();
  await section.getByRole("button", { name: "이전", exact: true }).click();
  await expect(section.getByText("레진 키링", { exact: true })).toBeVisible();
});
