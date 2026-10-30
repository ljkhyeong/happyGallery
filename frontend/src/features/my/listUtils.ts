import { formatKRW, getStatusLabel, parseApiDateTime } from "@/shared/lib";
import type { MyPassSummary } from "./api";
import type { MyFilterOption } from "./MyListFilterBar";

/** 상태 선택 상자는 이름순으로, 빠른 칩은 진행 순서를 지키도록 넘긴 순서대로 만든다. */
export function buildStatusFilterOptions(statuses: string[], { keepOrder = false } = {}): MyFilterOption[] {
  const unique = Array.from(new Set(statuses));
  if (!keepOrder) {
    unique.sort((left, right) => getStatusLabel(left).localeCompare(getStatusLabel(right), "ko"));
  }
  return unique.map((status) => ({
    value: status,
    label: getStatusLabel(status),
  }));
}

export function getPassFilterKey(pass: MyPassSummary): string {
  if (pass.remainingCredits <= 0) return "USED_UP";
  return parseApiDateTime(pass.expiresAt) <= Date.now() ? "EXPIRED" : "ACTIVE";
}

/** 이용권 행의 상태 배지. 환불한 이용권은 남은 횟수가 0이어도 '모두 사용'이 아니라 환불 상태로 보인다. */
export function passBadgeStatus(pass: MyPassSummary): string {
  if (pass.refund) return pass.refund.status === "SUCCEEDED" ? "REFUNDED" : "REFUNDING";
  return getPassFilterKey(pass);
}

export function isPassAvailableForBooking(pass: MyPassSummary): boolean {
  const expiresAt = parseApiDateTime(pass.expiresAt);
  return pass.refund === null
    && pass.remainingCredits > 0
    && Number.isFinite(expiresAt)
    && expiresAt > Date.now();
}

/** 이용권·무료 예약은 예약금이 0원이라 금액 대신 '예약금 없음'으로 보여 준다. */
export function bookingDepositLabel(depositAmount: number): string {
  return depositAmount > 0 ? `예약금 ${formatKRW(depositAmount)}` : "예약금 없음";
}
