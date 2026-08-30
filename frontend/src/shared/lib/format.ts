const krwFormatter = new Intl.NumberFormat("ko-KR", {
  style: "currency",
  currency: "KRW",
});

const ISO_DATE_ONLY = /^\d{4}-\d{2}-\d{2}$/;
const ISO_OFFSET_SUFFIX = /(?:Z|[+-]\d{2}:?\d{2})$/i;

/**
 * API의 offset 없는 업무 시각은 Asia/Seoul 현지시각으로 해석한다.
 * UTC 또는 offset을 명시한 DB 생성 시각은 원래 offset을 그대로 보존한다.
 */
export function parseApiDateTime(value: string): number {
  const trimmed = value.trim();
  if (ISO_OFFSET_SUFFIX.test(trimmed)) {
    return Date.parse(trimmed);
  }
  if (ISO_DATE_ONLY.test(trimmed)) {
    return Date.parse(`${trimmed}T00:00:00+09:00`);
  }
  return Date.parse(`${trimmed}+09:00`);
}

export function formatKRW(amount: number): string {
  return krwFormatter.format(amount);
}

const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  timeZone: "Asia/Seoul",
});

const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Asia/Seoul",
});

export function formatDate(iso: string): string {
  return dateFormatter.format(parseApiDateTime(iso));
}

export function formatDateTime(iso: string): string {
  return dateTimeFormatter.format(parseApiDateTime(iso));
}
