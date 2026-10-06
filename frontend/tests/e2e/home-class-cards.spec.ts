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

test("홈 클래스 카드는 마감 일정을 건너뛴 다음 수업과 남은 자리를 보여 주고 클래스 상세로 연결한다", async ({ page }) => {
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
      throw new Error(`홈 클래스 카드 테스트에서 예상하지 않은 변경 요청: ${request.method()} ${url.pathname}`);
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
      case "/api/v1/orders/policy": return json({ shippingFee: 3000 });
      case "/api/v1/slots/upcoming":
        return json(slotsByClass[Number(url.searchParams.get("classId"))] ?? []);
      default:
        throw new Error(`홈 클래스 카드 테스트에서 정의하지 않은 요청: ${url.pathname}`);
    }
  });

  await page.goto("/");
  const section = page.getByRole("region", { name: "공방에서 직접 만들어 보기" });

  // 마감된 오전 수업은 건너뛰고, 남은 자리가 적은 오후 수업을 다음 수업으로 보여 준다.
  const resinCard = section.getByRole("link", { name: /레진아트 원데이.*다음 수업 1\/2\(금\) 오후 01:00.*마감 임박 · 2자리/ });
  await expect(resinCard).toHaveAttribute("href", "/classes/42");
  await expect(section.getByRole("link", { name: /4회권 사용 가능.*가죽 카드지갑 정규.*다음 수업 1\/4\(일\) 오전 10:00/ }))
    .toHaveAttribute("href", "/classes/41");

  // 상황별 바로가기는 클래스 목록의 상황 조건으로 연결된다.
  await expect(section.getByRole("navigation", { name: "상황별로 수업 찾기" }).getByRole("link", { name: "이번 주말" }))
    .toHaveAttribute("href", "/classes?when=weekend");
});
