import { parseApiDateTime } from "../../shared/lib/format.ts";

interface EventPeriod {
  startAt: string;
  endAt: string;
}

const EVENT_REFRESH_MAX_INTERVAL_MS = 60_000;
const EVENT_REFRESH_MIN_INTERVAL_MS = 1_000;
const EVENT_BOUNDARY_SETTLE_MS = 250;

/**
 * 열린 화면이 이벤트 시작·종료 경계를 넘기기 직후 서버 상태를 다시 확인한다.
 * 가까운 경계가 없어도 새 이벤트 게시를 반영하도록 1분 간격은 유지한다.
 */
export function eventRefetchInterval(
  events: readonly EventPeriod[] | undefined,
  now = Date.now(),
): number {
  const nextBoundary = (events ?? [])
    .flatMap((event) => [event.startAt, event.endAt])
    .map(parseApiDateTime)
    .filter((boundary) => Number.isFinite(boundary) && boundary > now)
    .reduce((nearest, boundary) => Math.min(nearest, boundary), Number.POSITIVE_INFINITY);

  if (!Number.isFinite(nextBoundary)) return EVENT_REFRESH_MAX_INTERVAL_MS;

  return Math.max(
    EVENT_REFRESH_MIN_INTERVAL_MS,
    Math.min(
      EVENT_REFRESH_MAX_INTERVAL_MS,
      nextBoundary - now + EVENT_BOUNDARY_SETTLE_MS,
    ),
  );
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
