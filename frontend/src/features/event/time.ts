import { parseApiDateTime } from "../../shared/lib/format.ts";

interface EventPeriod {
  startAt: string;
  endAt: string;
}

export function isEventOngoing(event: EventPeriod, now = Date.now()): boolean {
  return parseApiDateTime(event.startAt) <= now && now < parseApiDateTime(event.endAt);
}

export function isEventAvailable(event: EventPeriod, now = Date.now()): boolean {
  return now < parseApiDateTime(event.endAt);
}

export function eventTimingLabel(
  event: EventPeriod,
  now = Date.now(),
): "진행 중" | "예정" | "종료" {
  if (!isEventAvailable(event, now)) return "종료";
  return isEventOngoing(event, now) ? "진행 중" : "예정";
}
