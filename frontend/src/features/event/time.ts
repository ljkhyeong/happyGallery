import {
  formatDate,
  formatDateInput,
  formatDateTime,
  formatDateTimeInput,
  parseApiDateTime,
} from "../../shared/lib/format.ts";

interface EventPeriod {
  startAt: string;
  endAt: string;
}

export interface EventPeriodLabel {
  start: string;
  /** 하루짜리 이벤트는 종료 표기를 생략한다. */
  end: string | null;
}

function isSeoulMidnight(timestamp: number): boolean {
  return formatDateTimeInput(timestamp).endsWith("T00:00");
}

/** 종료 시각에는 이미 끝난 것으로 판정하므로 서울 자정에 끝나면 마지막 날은 그 전날이다. 자정이 아니면 null. */
function lastDayBeforeMidnightEnd(end: number): string | null {
  return isSeoulMidnight(end) ? formatDateInput(end - 1) : null;
}

/** 시작·종료가 모두 서울 자정이면 날짜 단위 이벤트로 보고 날짜만 보여 준다. */
export function eventPeriodLabel(event: EventPeriod): EventPeriodLabel {
  const start = parseApiDateTime(event.startAt);
  const end = parseApiDateTime(event.endAt);
  const lastDay = start < end && isSeoulMidnight(start) ? lastDayBeforeMidnightEnd(end) : null;
  if (lastDay === null) {
    return { start: formatDateTime(event.startAt), end: formatDateTime(event.endAt) };
  }

  const firstDay = formatDateInput(start);
  return {
    start: formatDate(firstDay),
    end: lastDay === firstDay ? null : formatDate(lastDay),
  };
}

/**
 * 쿠폰 사용 기한처럼 끝 시각만 있는 기한의 "~까지" 표기. 서울 자정에 끝나면 이벤트 기간과 같이
 * 전날 날짜만 보이고(`2026-12-01T00:00:00` → `2026. 11. 30.`), 다른 시각은 시각까지 보인다.
 */
export function periodEndLabel(endAt: string): string {
  const lastDay = lastDayBeforeMidnightEnd(parseApiDateTime(endAt));
  return lastDay === null ? formatDateTime(endAt) : formatDate(lastDay);
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
