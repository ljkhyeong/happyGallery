import { expect, test, type Page, type Route } from "@playwright/test";

async function json(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) });
}

async function openCheckout(page: Page, kind: "ORDER" | "BOOKING") {
  const prepares: Array<{ payload: { verificationCode: string; name: string } }> = [];
  await page.addInitScript(() => {
    (window as unknown as { TossPayments: unknown }).TossPayments = () => ({ payment: () => ({
      requestPayment: async () => { throw new Error("결제창 취소"); },
    }) });
  });
  await page.route("**/api/v1/**", async (route) => {
    const { pathname } = new URL(route.request().url());
    if (pathname === "/api/v1/me") return json(route, { code: "UNAUTHORIZED" }, 401);
    if (pathname === "/api/v1/bookings/phone-verifications") return json(route, { verificationId: 1 });
    if (pathname === "/api/v1/policies/current") return json(route, {
      terms: { version: "2026-07", documentPath: "/terms/2026-07" },
      privacy: { version: "2026-07", documentPath: "/privacy/2026-07" },
    });
    if (pathname === "/api/v1/products") return json(route, [{
      id: 42, name: "재인증 확인 작품", price: 12000, type: "READY_STOCK", status: "ACTIVE",
      available: true, stockQuantity: 10, variants: [], optionGroups: [], fulfillmentOptions: ["SHIPPING", "PICKUP"],
    }]);
    if (pathname === "/api/v1/orders/policy") return json(route, {
      shippingFee: 0, madeToOrderConsentVersion: "2026-08", madeToOrderConsentText: "제작 동의",
    });
    if (pathname === "/api/v1/classes") return json(route, [{
      id: 42, name: "재인증 확인 수업", price: 50000, category: "LEATHER", status: "ACTIVE",
      durationMin: 90, bufferMin: 0, passEligible: false,
    }]);
    if (pathname === "/api/v1/slots/upcoming") return json(route, [{
      id: 77, classId: 42, startAt: "2099-01-02T10:00:00", endAt: "2099-01-02T11:30:00",
      capacity: 4, bookedCount: 0, remainingCapacity: 4,
    }]);
    if (pathname === "/api/v1/payments/prepare") {
      prepares.push(route.request().postDataJSON());
      if (prepares.length === 1) return json(route, { code: "PHONE_VERIFICATION_FAILED" }, 400);
      if (prepares.length === 2) return json(route, { code: "INSUFFICIENT_STOCK" }, 409);
      return json(route, { orderId: "guest-retry", amount: 5000, context: kind, statusToken: "guest-token" });
    }
    if (pathname === "/api/v1/payments/guest-retry/abandon") return route.fulfill({ status: 204 });
    if (pathname === "/api/v1/workshop") return json(route, { name: "해피갤러리", version: 1 });
    return json(route, []);
  });
  await page.goto(kind === "ORDER" ? "/orders/new?productId=42" : "/bookings/new");
  await page.context().addCookies([{ name: "XSRF-TOKEN", value: "guest-retry-xsrf", url: page.url() }]);
  if (kind === "BOOKING") {
    await page.getByLabel("클래스").selectOption("42");
    await page.locator('[data-slot-id="77"]').click();
    await page.getByRole("button", { name: "결제 진행하기" }).click();
    await page.getByRole("button", { name: "비회원", exact: true }).click();
  }
  return prepares;
}

for (const kind of ["ORDER", "BOOKING"] as const) {
  test(`@payment @identity 비회원 ${kind === "ORDER" ? "주문" : "예약"}은 인증 실패·결제창 취소 뒤 선택을 보존하고 재인증한다`, async ({ page }) => {
    const prepares = await openCheckout(page, kind);
    const submit = () => page.getByRole("button", { name: kind === "ORDER" ? "결제 진행하기" : "비회원으로 진행", exact: true });
    const reopen = async () => {
      if (kind === "BOOKING") await page.getByRole("button", { name: "결제 진행하기" }).click();
    };
    const verify = async (code: string) => {
      await page.getByLabel("휴대폰 번호", { exact: true }).fill("01012345678");
      await page.getByRole("button", { name: "인증코드 발송", exact: true }).click();
      await page.getByLabel("인증코드", { exact: true }).fill(code);
      await page.getByRole("button", { name: "확인", exact: true }).click();
    };
    await verify("111111");
    await page.getByLabel(kind === "ORDER" ? "주문자 이름" : "이름", { exact: true }).fill("재인증 고객");
    if (kind === "ORDER") await page.getByRole("button", { name: "매장 수령" }).click();
    await page.getByRole("checkbox", { name: /이용약관/ }).check();
    await submit().click();
    await expect.poll(() => prepares.length).toBe(1);
    await expect(page.getByText("휴대폰 인증에 실패했습니다. 인증코드를 확인해 주세요.")).toBeVisible();
    await reopen();
    await expect(page.getByText("인증코드가 올바르지 않거나 만료되었습니다. 새 인증코드를 받아 주세요.")).toBeVisible();
    await verify("222222");
    await expect(page.getByLabel(kind === "ORDER" ? "주문자 이름" : "이름", { exact: true })).toHaveValue("재인증 고객");
    await submit().click();
    await expect.poll(() => prepares.length).toBe(2);
    await reopen();
    if (kind === "BOOKING") await expect(page.getByLabel("인증코드", { exact: true })).toHaveCount(0);
    await submit().click();
    await expect.poll(() => prepares.length).toBe(3);
    await expect(page.getByText("요청 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.")).toBeVisible();
    await reopen();
    await expect(page.getByText("인증코드가 결제 준비에 사용되었습니다. 다시 결제하려면 새 인증코드를 받아 주세요.")).toBeVisible();
    if (kind === "ORDER") await expect(submit()).toBeDisabled();
    await verify("333333");
    if (kind === "ORDER") {
      await page.getByRole("button", { name: "재발송", exact: true }).click();
      await expect(submit()).toBeDisabled();
      await page.getByLabel("인증코드", { exact: true }).fill("333333");
      await page.getByRole("button", { name: "확인", exact: true }).click();
    }
    await submit().click();
    await expect.poll(() => prepares.length).toBe(4);
    expect(prepares.map(({ payload }) => payload.verificationCode)).toEqual(["111111", "222222", "222222", "333333"]);
    if (kind === "BOOKING") await expect(page.locator('[data-slot-id="77"]')).toHaveClass(/active/);
    else expect(prepares[3]!.payload).toMatchObject({ name: "재인증 고객", items: [{ productId: 42, qty: 1 }] });
  });
}
