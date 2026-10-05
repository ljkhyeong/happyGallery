import { getStatusLabel } from "@/shared/ui";
import { formatKRW, parseApiDateTime } from "@/shared/lib";
import type { MyPassSummary } from "./api";
import type { MyFilterOption } from "./MyListFilterBar";

export function buildStatusFilterOptions(statuses: string[]): MyFilterOption[] {
  return Array.from(new Set(statuses))
    .sort((left, right) => getStatusLabel(left).localeCompare(getStatusLabel(right), "ko"))
    .map((status) => ({
      value: status,
      label: getStatusLabel(status),
    }));
}

export function getPassFilterKey(pass: MyPassSummary): string {
  if (pass.remainingCredits <= 0) return "USED_UP";
  return parseApiDateTime(pass.expiresAt) <= Date.now() ? "EXPIRED" : "ACTIVE";
}

export function isPassAvailableForBooking(pass: MyPassSummary): boolean {
  const expiresAt = parseApiDateTime(pass.expiresAt);
  return pass.refund === null
    && pass.remainingCredits > 0
    && Number.isFinite(expiresAt)
    && expiresAt > Date.now();
}

export function isPassRefundable(pass: MyPassSummary): boolean {
  return isPassAvailableForBooking(pass);
}

/** 이용권·무료 예약은 예약금이 0원이라 금액 대신 '예약금 없음'으로 보여 준다. */
export function bookingDepositLabel(depositAmount: number): string {
  return depositAmount > 0 ? `예약금 ${formatKRW(depositAmount)}` : "예약금 없음";
}
