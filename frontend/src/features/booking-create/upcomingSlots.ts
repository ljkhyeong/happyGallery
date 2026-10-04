import { queryKeys } from "@/shared/api";
import { formatTime, isPerfumeClassCategory, parseApiDateTime } from "@/shared/lib";
import type { ClassResponse, PublicSlotResponse } from "@/generated/api/booking";
import { fetchUpcomingSlots } from "./api";

/** 예약·일정 변경·홈 빠른 예약이 같은 캐시를 쓰도록 조회 기간을 한 곳에서 정한다. */
export const UPCOMING_SLOT_DAYS = 14;

/** 마감 슬롯까지 받아 빈자리 알림과 남은 자리 표시를 함께 처리한다. */
export function upcomingSlotsQuery(classId: number) {
  return {
    queryKey: queryKeys.slotAvailability.upcoming.byClass(classId, UPCOMING_SLOT_DAYS),
    queryFn: () => fetchUpcomingSlots(classId, UPCOMING_SLOT_DAYS),
  };
}

const dayFormatter = new Intl.DateTimeFormat("ko-KR", {
  month: "numeric",
  day: "numeric",
  weekday: "short",
  timeZone: "Asia/Seoul",
});

/** `2026-10-06` 또는 슬롯 시작 시각을 서울 기준 월·일·요일로 나눈다. */
export function slotDayParts(value: string): { month: string; day: string; weekday: string } {
  const parts = Object.fromEntries(
    dayFormatter.formatToParts(parseApiDateTime(value)).map(({ type, value: part }) => [type, part]),
  );
  return { month: parts.month ?? "", day: parts.day ?? "", weekday: parts.weekday ?? "" };
}

/** 예: `10/6(화) 오전 10:00` */
export function formatSlotStart(slot: Pick<PublicSlotResponse, "startAt">): string {
  const { month, day, weekday } = slotDayParts(slot.startAt);
  return `${month}/${day}(${weekday}) ${formatTime(slot.startAt)}`;
}

export function nextOpenSlot(slots: PublicSlotResponse[] | undefined): PublicSlotResponse | null {
  return slots
    ?.filter((slot) => slot.remainingCapacity > 0)
    .sort((left, right) => left.startAt.localeCompare(right.startAt))[0] ?? null;
}

export function remainingSeatLabel(remainingCapacity: number): string {
  return remainingCapacity <= 2 ? `마감 임박 · ${remainingCapacity}자리` : `${remainingCapacity}자리 남음`;
}

export function bookingPaymentHint(bookingClass: Pick<ClassResponse, "passEligible" | "category">): string {
  return bookingClass.passEligible && !isPerfumeClassCategory(bookingClass.category)
    ? "날짜를 고른 뒤 예약금 결제 또는 4회권으로 예약할 수 있습니다."
    : "날짜를 고른 뒤 예약금을 결제하면 예약이 확정됩니다.";
}

/** `selectSlot`은 사용자가 이미 시간을 고른 경우에만 붙여 예약 화면이 해당 일정을 바로 선택하게 한다. */
export function bookingCreateHref(classId: number, slotId?: number, options?: { selectSlot?: boolean }): string {
  const params = new URLSearchParams({ classId: String(classId) });
  if (slotId != null) params.set("slotId", String(slotId));
  if (slotId != null && options?.selectSlot) params.set("selectSlot", "1");
  return `/bookings/new?${params.toString()}`;
}
