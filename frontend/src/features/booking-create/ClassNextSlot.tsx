import type { UseQueryResult } from "@tanstack/react-query";
import type { PublicSlotResponse } from "@/generated/api/booking";
import { formatSlotStart, nextOpenSlot, remainingSeatLabel, UPCOMING_SLOT_DAYS } from "./upcomingSlots";

/** 클래스 카드 아래에 가장 가까운 예약 가능 일정과 남은 자리를 보여 준다. */
export function ClassNextSlot({
  result,
  className = "class-next-slot",
}: {
  result: UseQueryResult<PublicSlotResponse[]> | undefined;
  className?: string;
}) {
  if (!result || result.isPending) {
    return <p className={className}>일정 확인 중</p>;
  }
  if (result.isError) {
    return <p className={className}>일정은 수업 상세에서 확인해 주세요</p>;
  }
  const next = nextOpenSlot(result.data);
  if (!next) {
    return <p className={className}>{UPCOMING_SLOT_DAYS}일 안에 예약 가능한 일정 없음</p>;
  }
  return (
    <p className={className}>
      <span>다음 수업 {formatSlotStart(next)}</span>
      <b className={next.remainingCapacity <= 2 ? "is-few" : undefined}>{remainingSeatLabel(next.remainingCapacity)}</b>
    </p>
  );
}
