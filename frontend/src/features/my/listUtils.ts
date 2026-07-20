import { getStatusLabel } from "@/shared/ui";
import { parseApiDateTime } from "@/shared/lib";
import type { MyPassSummary } from "./api";
import type { MyFilterOption } from "./MyListFilterBar";

export interface MyQuickTab extends MyFilterOption {
  count: number;
}

export function buildStatusFilterOptions(statuses: string[]): MyFilterOption[] {
  return Array.from(new Set(statuses))
    .sort((left, right) => getStatusLabel(left).localeCompare(getStatusLabel(right), "ko"))
    .map((status) => ({
      value: status,
      label: getStatusLabel(status),
    }));
}

export function buildQuickStatusTabs(statuses: string[], maxTabs = 3): MyQuickTab[] {
  const counts = new Map<string, number>();
  for (const status of statuses) {
    counts.set(status, (counts.get(status) ?? 0) + 1);
  }

  const quickTabs = Array.from(counts.entries())
    .sort((left, right) => {
      if (right[1] !== left[1]) {
        return right[1] - left[1];
      }
      return getStatusLabel(left[0]).localeCompare(getStatusLabel(right[0]), "ko");
    })
    .slice(0, maxTabs)
    .map(([value, count]) => ({
      value,
      label: getStatusLabel(value),
      count,
    }));

  return [
    { value: "ALL", label: "전체", count: statuses.length },
    ...quickTabs,
  ];
}

export function getPassFilterKey(pass: MyPassSummary): string {
  if (pass.remainingCredits <= 0) return "USED_UP";
  return parseApiDateTime(pass.expiresAt) < Date.now() ? "EXPIRED" : "ACTIVE";
}

export function isPassAvailableForBooking(pass: MyPassSummary): boolean {
  const expiresAt = parseApiDateTime(pass.expiresAt);
  return pass.refund === null
    && pass.remainingCredits > 0
    && Number.isFinite(expiresAt)
    && expiresAt > Date.now();
}

export function buildPassTabs(passes: MyPassSummary[]): MyQuickTab[] {
  const counts = {
    ACTIVE: 0,
    USED_UP: 0,
    EXPIRED: 0,
  };

  for (const pass of passes) {
    counts[getPassFilterKey(pass) as keyof typeof counts] += 1;
  }

  return [
    { value: "ALL", label: "전체", count: passes.length },
    { value: "ACTIVE", label: "사용 가능", count: counts.ACTIVE },
    { value: "USED_UP", label: "사용 완료", count: counts.USED_UP },
    { value: "EXPIRED", label: "만료", count: counts.EXPIRED },
  ];
}
