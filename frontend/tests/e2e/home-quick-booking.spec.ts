import { expect, test } from "@playwright/test";
import type { ClassResponse, PublicSlotResponse } from "../../src/generated/api/booking";
import {
  clearSsrUpstreamFixtures,
  homeSsrFixtures,
  replaceSsrUpstreamFixtures,
} from "./ssr-upstream-fixture";

const leather: ClassResponse = {
  id: 41, name: "가죽 카드지갑 정규", category: "LEATHER", durationMin: 120,
  price: 50000, bufferMin: 30, capacity: 8, passEligible: true,
  description: null, imageUrl: null, preparationInfo: null, targetAudience: null, situationTags: [], status: "ACTIVE",
};
const resin: ClassResponse = {
  ...leather, id: 42, name: "레진아트 원데이", category: "RESIN", durationMin: 90, price: 42000, passEligible: false,
};

function slot(id: number, classId: number, startAt: string, remainingCapacity: number): PublicSlotResponse {
  const hour = Number(startAt.slice(11, 13));
  return {
    id, classId, startAt, endAt: `${startAt.slice(0, 11)}${String(hour + 2).padStart(2, "0")}:00:00`,
    capacity: 8, bookedCount: 8 - remainingCapacity, remainingCapacity,
  };
}

const slotsByClass: Record<number, PublicSlotResponse[]> = {
  41: [slot(911, 41, "2099-01-04T10:00:00", 8)],
  42: [
    slot(901, 42, "2099-01-02T10:00:00", 0),
    slot(902, 42, "2099-01-02T13:00:00", 2),
    slot(903, 42, "2099-01-03T10:00:00", 8),
  ],
};

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

test("홈에서 수업·날짜·시간을 고르면 같은 일정이 선택된 예약 화면으로 이동한다", async ({ page }) => {
  await replaceSsrUpstreamFixtures(...homeSsrFixtures({
    workshop: { name: "해피갤러리" },
    classes: [leather, resin],
  }));
  await page.route("**/api/v1/**", async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const json = (body: unknown) => route.fulfill({
      status: 200, contentType: "application/json", body: JSON.stringify(body),
    });
    if (request.method() !== "GET") {
      throw new Error(`홈 빠른 예약 테스트에서 예상하지 않은 변경 요청: ${request.method()} ${url.pathname}`);
    }
    switch (url.pathname) {
      case "/api/v1/me": return json({
        id: 601, name: "회원", email: "quick@example.com", phone: "01012345678",
        phoneVerified: true, localPasswordEnabled: true,
      });
      case "/api/v1/workshop": return json({ name: "해피갤러리" });
      case "/api/v1/me/cart": return json({ cartVersion: "0".repeat(64), items: [], totalAmount: 0 });
      case "/api/v1/me/notifications/unread-count": return json({ count: 0 });
      case "/api/v1/me/passes/page": return json({ content: [], hasMore: false, nextCursor: null });
      case "/api/v1/me/vacancy-alerts": return json([]);
      case "/api/v1/policies/current": return json({
        terms: { version: "2026-09", documentPath: "/terms" },
        privacy: { version: "2026-09", documentPath: "/privacy" },
      });
      case "/api/v1/classes": return json([leather, resin]);
      case "/api/v1/products":
      case "/api/v1/events":
      case "/api/v1/notices":
        return json([]);
      case "/api/v1/slots/upcoming":
        return json(slotsByClass[Number(url.searchParams.get("classId"))] ?? []);
      default:
        throw new Error(`홈 빠른 예약 테스트에서 정의하지 않은 요청: ${url.pathname}`);
    }
  });

  await page.goto("/");
  const panel = page.getByRole("complementary", { name: "바로 예약하기" });

  // 첫 수업의 가장 이른 빈 일정이 기본으로 선택된다.
  await expect(panel.getByRole("link", { name: "1/4(일) 오전 10:00 예약하기" }))
    .toHaveAttribute("href", "/bookings/new?classId=41&slotId=911&selectSlot=1");

  // 마감 시간은 고를 수 없고, 같은 날 남은 시간이 기본 선택된다. 카드도 마감을 건너뛴 다음 수업을 보여 준다.
  await panel.getByRole("button", { name: /레진아트 원데이/ }).click();
  await expect(panel.getByRole("button", { name: /오전 10:00/ })).toBeDisabled();
  await expect(panel.getByRole("link", { name: "1/2(금) 오후 01:00 예약하기" }))
    .toHaveAttribute("href", "/bookings/new?classId=42&slotId=902&selectSlot=1");
  await expect(page.getByRole("link", { name: /레진아트 원데이.*다음 수업 1\/2\(금\) 오후 01:00.*마감 임박 · 2자리/ }))
    .toBeVisible();

  await panel.getByRole("button", { name: "1월 3일 토요일" }).click();
  await panel.getByRole("link", { name: "1/3(토) 오전 10:00 예약하기" }).click();

  // 홈에서 고른 시간은 예약 화면에서 다시 고르지 않아도 선택되어 있다.
  await expect(page).toHaveURL(/\/bookings\/new\?classId=42&slotId=903&selectSlot=1$/);
  await expect(page.locator('[data-booking-date="2099-01-03"]')).toHaveAttribute("aria-pressed", "true");
  await expect(page.locator('[data-slot-id="903"]')).toHaveClass(/active/);
  await expect(page.getByLabel("예약 인원", { exact: true })).toHaveValue("1");
});
