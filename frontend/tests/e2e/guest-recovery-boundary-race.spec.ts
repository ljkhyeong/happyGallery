import { expect, test, type Route } from "@playwright/test";

const EMPTY_CART_VERSION = "0".repeat(64);

async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(body),
  });
}

test("@identity 복구 토큰 저장 중 회원 세션이 바뀌면 방금 저장한 capability를 되돌린다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const nextMember = {
    id: 402,
    email: "recovery-race@example.com",
    name: "복구 경계 회원",
    phone: "01040204020",
    phoneVerified: true,
    localPasswordEnabled: true,
  };
  let currentMember: typeof nextMember | null = null;
  let recoveredPageReads = 0;

  await context.addCookies([{
    name: "XSRF-TOKEN",
    value: "guest-recovery-race-token",
    url: baseURL,
  }]);
  await page.addInitScript(({ customerId }) => {
    const originalSetItem = Storage.prototype.setItem;
    let boundaryInjected = false;
    Storage.prototype.setItem = function setItem(key: string, value: string) {
      const result = originalSetItem.call(this, key, value);
      if (
        this === window.sessionStorage
        && key === "guest_record_recovery"
        && !boundaryInjected
      ) {
        boundaryInjected = true;
        const boundaryKey = "hg_customer_session_boundary";
        const oldValue = localStorage.getItem(boundaryKey);
        const newValue = JSON.stringify({
          epoch: "guest-recovery-storage-race",
          customerId,
        });
        originalSetItem.call(localStorage, boundaryKey, newValue);
        window.dispatchEvent(new StorageEvent("storage", {
          key: boundaryKey,
          oldValue,
          newValue,
          storageArea: localStorage,
        }));
      }
      return result;
    };
  }, { customerId: nextMember.id });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (pathname === "/api/v1/me" && request.method() === "GET") {
      if (currentMember) {
        await fulfillJson(route, currentMember);
      } else {
        await fulfillJson(route, {
          code: "UNAUTHORIZED",
          message: "로그인이 필요합니다.",
        }, 401);
      }
      return;
    }
    if (pathname === "/api/v1/bookings/phone-verifications") {
      await fulfillJson(route, {
        phone: "01012345678",
        verificationId: 801,
      });
      return;
    }
    if (pathname === "/api/v1/guest-records/recovery" && request.method() === "POST") {
      await fulfillJson(route, {
        accessToken: "must-be-rolled-back",
        expiresAt: "2099-08-08T12:00:00",
        orders: [],
        bookings: [],
      });
      return;
    }
    if (
      pathname === "/api/v1/guest-records/recovery/orders"
      || pathname === "/api/v1/guest-records/recovery/bookings"
    ) {
      recoveredPageReads += 1;
      await fulfillJson(route, { content: [], hasMore: false, nextCursor: null });
      return;
    }
    if (pathname === "/api/v1/me/cart") {
      await fulfillJson(route, { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 });
      return;
    }
    if (pathname === "/api/v1/me/notifications/unread-count") {
      await fulfillJson(route, { count: 0 });
      return;
    }
    if (pathname === "/api/v1/workshop") {
      await fulfillJson(route, { name: "해피갤러리" });
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/guest");
  const recoveryCard = page.locator(".card")
    .filter({ hasText: "주문·예약 조회 정보 복구" })
    .first();
  await recoveryCard.getByLabel("휴대폰 번호").fill("01012345678");
  await recoveryCard.getByRole("button", { name: "인증코드 발송" }).click();
  await recoveryCard.getByLabel("인증코드").fill("123456");
  currentMember = nextMember;
  await recoveryCard.getByRole("button", { name: "조회 정보 복구" }).click();

  await expect(page.getByRole("link", { name: nextMember.name, exact: true }).first())
    .toBeVisible();
  await expect.poll(() => page.evaluate(() =>
    sessionStorage.getItem("guest_record_recovery")))
    .toBeNull();
  await expect(page.getByText("조회 정보를 복구했습니다.")).toHaveCount(0);
  expect(recoveredPageReads).toBe(0);
});
