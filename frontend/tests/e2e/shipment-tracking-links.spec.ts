import { expect, test } from "@playwright/test";

test("배송조회는 공식 사이트로 연결하고 복사 실패 시 직접 복사를 안내한다 @payment", async ({ page }) => {
  let carrierCode = "CJ_LOGISTICS";
  await page.addInitScript(() => {
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: async () => { throw new Error("복사 권한 없음"); } },
    });
  });
  await page.route("**/api/v1/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    const json = (body: unknown) => route.fulfill({ contentType: "application/json", body: JSON.stringify(body) });
    if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "tracking@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
    if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
    if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
    if (path === "/api/v1/me/orders/200") return json({
      orderId: 200, orderNumber: "배송 주문", status: "SHIPPED", items: [],
      productAmount: 10000, shippingFee: 0, totalAmount: 10000, pgPaidAmount: 10000,
      couponDiscountAmount: 0, rewardUsedAmount: 0, refund: null, receiptUrl: null,
      paidAt: "2026-09-05T10:00:00", approvalDeadlineAt: null,
      fulfillment: { type: "SHIPPING", status: "SHIPPED", carrierCode, carrier: carrierCode,
        trackingNumber: "1234-5678-9012", shippedAt: "2026-09-06T10:00:00", deliveredAt: null,
        pickupDeadlineAt: null, trackingStatusText: null, trackingUpdatedAt: null, trackingEvents: [] },
    });
    if (path === "/api/v1/me/reviews/orders/200") return json([]);
    if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
    return route.fulfill({ status: 404 });
  });
  for (const [code, label, origin] of [
    ["CJ_LOGISTICS", "CJ대한통운 배송조회", "https://www.cjlogistics.com"],
    ["LOTTE", "롯데택배 배송조회", "https://www.lottegl.com"],
    ["KOREA_POST", "우체국택배 배송조회", "https://service.epost.go.kr"],
    ["HANJIN", "한진 배송조회", "https://www.hanjin.com"],
  ]) {
    carrierCode = code;
    await page.goto("/my/orders/200");
    const link = page.getByRole("link", { name: label });
    await expect(link).toBeVisible();
    await expect(link).toHaveAttribute("target", "_blank");
    await expect(link).toHaveAttribute("rel", "noreferrer");
    const url = new URL((await link.getAttribute("href"))!);
    expect(url.origin).toBe(origin);
    if (code === "HANJIN") expect(url.searchParams.get("wblnumText2")).toBe("123456789012");
    else await expect(page.getByText("운송장 번호를 복사한 뒤 택배사 사이트에 붙여넣으세요.")).toBeVisible();
  }
  await page.getByRole("button", { name: "운송장 복사" }).click();
  await expect(page.getByRole("alert")).toContainText("직접 선택해 복사해 주세요");
  await expect(page.getByRole("button", { name: "복사됨" })).toHaveCount(0);
});
