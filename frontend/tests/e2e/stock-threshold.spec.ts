import { expect, test } from "@playwright/test";

test("최소 보유 수량을 저장하면 오늘 할 일에 표시하고 해제하면 목록에서 빠진다", async ({ page }) => {
  await page.addInitScript(() => sessionStorage.setItem("hg_admin_token", "stock-threshold-admin"));
  let minimumStock: number | null = null;
  let version = 0;
  const product = { id: 42, name: "재고 기준 작품", type: "READY_STOCK", category: null, price: 10000, status: "ACTIVE", quantity: 2, available: true, variants: [], optionGroups: [] };
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/me") return json({ code: "UNAUTHORIZED", message: "로그인 필요" }, 401);
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    if (path === "/api/v1/admin/products") return json([product]);
    if (path === "/api/v1/admin/products/42/inventory-adjustments") return json([]);
    if (path === "/api/v1/admin/stock-levels/threshold") {
      const request = route.request().postDataJSON();
      expect(request.productId).toBe(42);
      expect(request.productVariantId).toBeNull();
      expect(request.version).toBe(version);
      minimumStock = request.minimumStock;
      version += 1;
      return route.fulfill({ status: 204 });
    }
    if (path === "/api/v1/admin/stock-levels") return json([{
      productId: 42, productVariantId: null, productName: product.name, type: product.type,
      quantity: 2, minimumStock, version, active: true, lowStock: minimumStock !== null && 2 <= minimumStock,
    }]);
    return json({ code: "SERVICE_UNAVAILABLE", message: "이 테스트에서는 조회하지 않는 운영 항목입니다." }, 503);
  });
  await page.goto("/admin?view=products&productId=42");
  await page.getByLabel("최소 보유 수량", { exact: true }).fill("3");
  await page.getByRole("button", { name: "기준 저장", exact: true }).click();
  await expect(page.getByText("최소 보유 수량을 저장했습니다.", { exact: true })).toBeVisible();
  await page.getByRole("dialog").getByRole("button", { name: "Close", exact: true }).click();
  await page.getByRole("button", { name: "오늘 할 일", exact: true }).click();
  const panel = page.locator("section.admin-workspace-panel").filter({ hasText: "품절·재고 부족 상품과 옵션" });
  await expect(panel.getByText("재고 기준 작품", { exact: true })).toBeVisible();
  await expect(panel.getByText("재고 부족", { exact: true })).toBeVisible();
  await panel.getByRole("link", { name: "재고 조정" }).click();
  await page.getByLabel("최소 보유 수량", { exact: true }).fill("");
  await page.getByRole("button", { name: "기준 저장", exact: true }).click();
  await expect.poll(() => minimumStock).toBeNull();
  await page.getByRole("dialog").getByRole("button", { name: "Close", exact: true }).click();
  await page.getByRole("button", { name: "오늘 할 일", exact: true }).click();
  await expect(panel.getByText("재고를 채워야 할 판매 중 상품이 없습니다.", { exact: true })).toBeVisible();
});
