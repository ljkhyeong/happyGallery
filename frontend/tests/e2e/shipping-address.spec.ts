import { expect, test } from "@playwright/test";

for (const member of [true, false]) {
  test(`${member ? "회원" : "비회원"}은 배송지를 수정하고 배송 준비 후에는 수정할 수 없다`, async ({ page, context, baseURL }) => {
    await context.addCookies([{ name: "XSRF-TOKEN", value: "address-csrf", url: baseURL! }]);
    let status = "PAID_APPROVAL_PENDING";
    let version = 0;
    let address = { recipientName: "수령인", phone: "01012345678", postalCode: "12345", addressLine1: "이전 기본 주소", addressLine2: "101호" };
    const requests: unknown[] = [];
    await page.route("**/api/v1/**", async (route) => {
      const url = new URL(route.request().url());
      const path = url.pathname;
      const json = (body: unknown, responseStatus = 200) => route.fulfill({ status: responseStatus, contentType: "application/json", body: JSON.stringify(body) });
      if (path === "/api/v1/me") return json({ id: 501, name: "회원", email: "address@example.com", phone: "01012345678", phoneVerified: true, localPasswordEnabled: true });
      if (path === "/api/v1/me/cart") return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      if (path === "/api/v1/me/notifications/unread-count") return json({ count: 0 });
      if (path === "/api/v1/workshop") return json({ name: "해피갤러리" });
      if (path.endsWith("/orders/200/shipping-address")) {
        const body = route.request().postDataJSON();
        requests.push(body);
        if (!member) expect(route.request().headers()["x-access-token"]).toBe("guest-address-token");
        expect(body.version).toBe(version);
        address = body.shippingAddress;
        version += 1;
        return route.fulfill({ status: 204 });
      }
      if (path.endsWith("/orders/200")) return json({
        orderId: 200, orderNumber: "주소 테스트 주문", status, items: [],
        productAmount: 30000, shippingFee: 3000, totalAmount: 33000, pgPaidAmount: 33000,
        couponDiscountAmount: 0, rewardUsedAmount: 0, refund: null, receiptUrl: null,
        paidAt: "2026-09-05T10:00:00", approvalDeadlineAt: null,
        fulfillment: { version, type: "SHIPPING", shippingAddress: address, trackingEvents: [] },
      });
      return json([]);
    });
    await page.goto(member ? "/my/orders/200" : "/guest/orders");
    if (!member) {
      await page.getByLabel("주문 번호", { exact: true }).fill("200");
      await page.getByLabel("조회 코드", { exact: true }).fill("guest-address-token");
      await page.getByRole("button", { name: "조회", exact: true }).click();
    }
    await page.getByRole("button", { name: "배송지 수정", exact: true }).click();
    await page.getByLabel("기본 주소", { exact: true }).fill("변경한 기본 주소");
    await page.getByRole("button", { name: "배송지 저장", exact: true }).click();
    await expect(page.getByText("(12345) 변경한 기본 주소 101호", { exact: true })).toBeVisible();
    expect(requests).toHaveLength(1);
    status = "SHIPPING_PREPARING";
    if (member) await page.reload();
    else await page.getByRole("button", { name: "조회", exact: true }).click();
    await expect(page.getByText("배송 준비 중", { exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "배송지 수정", exact: true })).toHaveCount(0);
  });
}
