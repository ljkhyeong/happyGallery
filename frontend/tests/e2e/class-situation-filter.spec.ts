import { expect, test } from "@playwright/test";
import type { ClassResponse, PublicSlotResponse } from "../../src/generated/api/booking";
import {
  clearSsrUpstreamFixtures,
  homeSsrFixtures,
  replaceSsrUpstreamFixtures,
  ssrApiFixture,
} from "./ssr-upstream-fixture";

const resin: ClassResponse = {
  id: 61, name: "레진 키링 원데이", category: "RESIN", durationMin: 90, price: 42000,
  bufferMin: 30, capacity: 8, passEligible: false, description: null, imageUrl: null,
  preparationInfo: null, targetAudience: null, situationTags: ["DATE", "FRIENDS"], status: "ACTIVE",
};
const upcycling: ClassResponse = {
  ...resin, id: 62, name: "양말목 바구니 원데이", category: "UPCYCLING", situationTags: ["WITH_KIDS"],
};

function slot(id: number, classId: number, startAt: string): PublicSlotResponse {
  return { id, classId, startAt, endAt: startAt.replace("T10:", "T12:"), capacity: 8, bookedCount: 2, remainingCapacity: 6 };
}

function slotsFixture(classId: number, slots: PublicSlotResponse[]) {
  return ssrApiFixture(`/slots/upcoming?classId=${classId}&days=14&includeFull=true`, slots);
}

test.afterEach(async () => {
  await clearSsrUpstreamFixtures();
});

test("홈 상황별 바로가기와 클래스 목록 상황 필터가 태그·주말 일정으로 거르고 새로고침에도 유지된다", async ({ page }) => {
  await replaceSsrUpstreamFixtures(
    ...homeSsrFixtures({ workshop: { name: "해피갤러리" }, classes: [resin, upcycling] }),
    { ...ssrApiFixture("/me", { code: "UNAUTHORIZED", message: "로그인이 필요합니다." }), status: 401 },
    // 2099-01-05(월) 기준: 레진은 수요일, 양말목은 이번 주 토요일에만 자리가 있다.
    slotsFixture(61, [slot(801, 61, "2099-01-07T10:00:00")]),
    slotsFixture(62, [slot(802, 62, "2099-01-10T10:00:00")]),
  );
  await page.clock.setFixedTime(new Date("2099-01-05T10:00:00+09:00"));

  await page.goto("/");
  await page.getByRole("navigation", { name: "상황별로 수업 찾기" }).getByRole("link", { name: "데이트" }).click();
  await expect(page).toHaveURL(/\/classes\?tag=DATE$/);
  const cards = page.locator(".class-catalog-item");
  await expect(cards.getByRole("heading")).toHaveText(["레진 키링 원데이"]);
  await expect(page.getByRole("button", { name: "데이트", exact: true })).toHaveAttribute("aria-pressed", "true");

  // 상황 조건은 하나만 고른다. 주말 조건은 태그를 지우고 실제 주말 일정으로 거른다.
  await page.getByRole("button", { name: "이번 주말", exact: true }).click();
  await expect(page).toHaveURL(/\/classes\?when=weekend$/);
  await expect(cards.getByRole("heading")).toHaveText(["양말목 바구니 원데이"]);
  await page.reload();
  await expect(cards.getByRole("heading")).toHaveText(["양말목 바구니 원데이"]);

  await page.getByRole("button", { name: "오늘 바로", exact: true }).click();
  await expect(page.getByText("오늘 바로 예약 가능한 수업이 없습니다.")).toBeVisible();
  await page.getByRole("button", { name: "전체 수업 보기", exact: true }).click();
  await expect(page).toHaveURL(/\/classes$/);
  await expect(cards).toHaveCount(2);
});
