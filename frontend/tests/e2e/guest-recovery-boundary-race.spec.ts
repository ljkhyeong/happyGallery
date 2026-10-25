import { expect, test } from "@playwright/test";
import { EMPTY_CURSOR_PAGE, fulfillJson, fulfillMemberCommon, installCsrfCookie, memberFixture } from "./mock-support";

test("@identity 복구 토큰 저장 중 회원 세션이 바뀌면 방금 저장한 capability를 되돌린다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const nextMember = memberFixture({
    id: 402,
    email: "recovery-race@example.com",
    name: "복구 경계 회원",
    phone: "01040204020",
  });
  let currentMember: typeof nextMember | null = null;
  let recoveredPageReads = 0;

  await installCsrfCookie(context, baseURL);
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

    if (await fulfillMemberCommon(route, { member: currentMember })) return;
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
      });
      return;
    }
    if (
      pathname === "/api/v1/guest-records/recovery/orders"
      || pathname === "/api/v1/guest-records/recovery/bookings"
    ) {
      recoveredPageReads += 1;
      await fulfillJson(route, EMPTY_CURSOR_PAGE);
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/guest");
  await page.getByRole("button", { name: "주문·예약 번호나 조회 코드를 잊었어요" }).click();
  const recoveryCard = page.locator(".card")
    .filter({ hasText: "주문·예약 조회 코드 재발급" })
    .first();
  await recoveryCard.getByLabel("휴대폰 번호").fill("01012345678");
  await recoveryCard.getByRole("button", { name: "인증번호 발송" }).click();
  await recoveryCard.getByLabel("인증번호").fill("123456");
  currentMember = nextMember;
  await recoveryCard.getByRole("button", { name: "조회 코드 재발급" }).click();

  await expect(page.getByRole("link", { name: nextMember.name, exact: true }).first())
    .toBeVisible();
  await expect.poll(() => page.evaluate(() =>
    sessionStorage.getItem("guest_record_recovery")))
    .toBeNull();
  await expect(page.getByText("조회 정보를 복구했습니다.")).toHaveCount(0);
  expect(recoveredPageReads).toBe(0);
});
