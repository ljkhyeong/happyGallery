import { expect, test } from "@playwright/test";
import {
  classFixture,
  currentPoliciesFixture,
  EMPTY_CURSOR_PAGE,
  fulfillJson,
  fulfillMemberCommon,
  installCsrfCookie,
  memberFixture,
} from "./mock-support";

test("@identity 예약 중 로그인하면 이전 컴포넌트가 결제하지 않고 새 세션에서 선택을 확인한다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const member = memberFixture({ id: 202, email: "booking-resume@example.com", name: "예약 재개 회원", phone: "01022222222" });
  let currentMember: typeof member | null = null;
  let slotReads = 0;
  const preparePayloads: Array<Record<string, unknown>> = [];

  await installCsrfCookie(context, baseURL);
  await page.addInitScript(() => {
    const browserGlobal = globalThis as unknown as {
      TossPayments: () => {
        payment: () => {
          requestPayment: () => Promise<void>;
        };
      };
    };
    browserGlobal.TossPayments = () => ({
      payment: () => ({ requestPayment: async () => undefined }),
    });
  });

  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (await fulfillMemberCommon(route, { member: currentMember })) return;
    if (pathname === "/api/v1/auth/login") {
      currentMember = member;
      await fulfillJson(route, member);
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [classFixture({ id: 42, name: "로그인 재개 클래스", bufferMin: 10 })]);
      return;
    }
    if (pathname === "/api/v1/slots/upcoming") {
      slotReads += 1;
      await fulfillJson(route, [{
        id: 77,
        classId: 42,
        startAt: "2099-01-02T10:00:00",
        endAt: "2099-01-02T11:30:00",
        capacity: 4,
        bookedCount: 0,
        remainingCapacity: slotReads === 1 ? 4 : 2,
      }]);
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      const body = request.postDataJSON() as {
        payload: Record<string, unknown>;
      };
      preparePayloads.push(body.payload);
      await fulfillJson(route, {
        orderId: "booking-resume-payment",
        amount: 5000,
        context: "BOOKING",
        statusToken: "booking-resume-status",
      });
      return;
    }
    if (pathname === "/api/v1/me/passes/page") {
      await fulfillJson(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, currentPoliciesFixture());
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption("42");
  await page.locator('[data-slot-id="77"]').click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();

  await page.getByLabel("이메일").fill(member.email);
  await page.getByLabel("비밀번호").fill("password123!");
  await page.getByRole("button", { name: "로그인 후 내용 확인" }).click();

  await expect(page.getByRole("status")).toContainText("로그인이 완료되었습니다.");
  await expect(page.locator('[data-slot-id="77"]')).toHaveClass(/active/);
  await expect(page.locator('[data-slot-id="77"]')).toContainText("2명 예약 가능");
  expect(preparePayloads).toHaveLength(0);

  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await expect.poll(() => preparePayloads.length).toBe(1);
  expect(preparePayloads[0]).toMatchObject({
    userId: member.id,
    slotId: 77,
    participantCount: 1,
    paymentMethod: "CARD",
  });
});

test("@identity 로그인 재개 초안은 이를 만든 회원과 다른 계정에 복원되지 않는다", async ({
  baseURL,
  context,
  page,
}) => {
  if (!baseURL) throw new Error("Playwright baseURL이 필요합니다.");

  const memberA = memberFixture({ id: 301, email: "booking-owner-a@example.com", name: "예약 초안 회원 A", phone: "01030103010" });
  const memberB = memberFixture({ id: 302, email: "booking-owner-b@example.com", name: "예약 초안 회원 B", phone: "01030203020" });
  let currentMember: typeof memberA | null = null;
  const preparePayloads: Array<Record<string, unknown>> = [];

  await installCsrfCookie(context, baseURL);
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const { pathname } = new URL(request.url());

    if (await fulfillMemberCommon(route, { member: currentMember })) return;
    if (pathname === "/api/v1/auth/login") {
      currentMember = memberA;
      await fulfillJson(route, memberA);
      return;
    }
    if (pathname === "/api/v1/classes") {
      await fulfillJson(route, [classFixture({ id: 51, name: "계정 경계 클래스", bufferMin: 10, price: 60000 })]);
      return;
    }
    if (pathname === "/api/v1/slots/upcoming") {
      await fulfillJson(route, [{
        id: 88,
        classId: 51,
        startAt: "2099-01-03T10:00:00",
        endAt: "2099-01-03T11:30:00",
        capacity: 4,
        bookedCount: 0,
        remainingCapacity: 4,
      }]);
      return;
    }
    if (pathname === "/api/v1/payments/prepare") {
      const body = request.postDataJSON() as { payload: Record<string, unknown> };
      preparePayloads.push(body.payload);
      await fulfillJson(route, {
        orderId: "cross-account-resume-payment",
        amount: 6000,
        context: "BOOKING",
        statusToken: "cross-account-resume-status",
      });
      return;
    }
    if (pathname === "/api/v1/me/passes/page") {
      await fulfillJson(route, EMPTY_CURSOR_PAGE);
      return;
    }
    if (pathname === "/api/v1/policies/current") {
      await fulfillJson(route, currentPoliciesFixture("2026-08"));
      return;
    }

    await fulfillJson(route, []);
  });

  await page.goto("/bookings/new");
  await page.getByLabel("클래스").selectOption("51");
  await page.locator('[data-slot-id="88"]').click();
  await page.getByRole("button", { name: "결제 진행하기" }).click();
  await page.getByLabel("이메일").fill(memberA.email);
  await page.getByLabel("비밀번호").fill("password123!");
  await page.getByRole("button", { name: "로그인 후 내용 확인" }).click();

  await expect(page.getByRole("status")).toContainText("로그인이 완료되었습니다.");
  await expect(page.locator('[data-slot-id="88"]')).toHaveClass(/active/);

  const publishExternalBoundary = async (
    member: typeof memberA | typeof memberB,
    epoch: string,
  ) => {
    currentMember = member;
    await page.evaluate(({ customerId, nextEpoch }) => {
      const key = "hg_customer_session_boundary";
      const oldValue = localStorage.getItem(key);
      const newValue = JSON.stringify({ epoch: nextEpoch, customerId });
      localStorage.setItem(key, newValue);
      window.dispatchEvent(new StorageEvent("storage", {
        key,
        oldValue,
        newValue,
        storageArea: localStorage,
      }));
    }, { customerId: member.id, nextEpoch: epoch });
    await expect(page.getByRole("link", { name: member.name, exact: true }).first())
      .toBeVisible();
  };

  await publishExternalBoundary(memberB, "booking-boundary-member-b");
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(page.getByLabel("클래스")).toHaveValue("");
  await expect(page.locator('[data-slot-id="88"].active')).toHaveCount(0);
  await expect(page.getByRole("button", { name: "결제 진행하기" })).toBeDisabled();
  expect(preparePayloads).toHaveLength(0);

  await page.evaluate(() => new Promise<void>((resolve) => {
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()));
  }));
  await publishExternalBoundary(memberA, "booking-boundary-member-a-returned");
  await expect(page.getByRole("status")).toHaveCount(0);
  await expect(page.getByLabel("클래스")).toHaveValue("");
  await expect(page.locator('[data-slot-id="88"].active')).toHaveCount(0);
});
