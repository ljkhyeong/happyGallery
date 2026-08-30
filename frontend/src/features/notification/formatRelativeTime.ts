import { parseApiDateTime } from "@/shared/lib";

const relativeDateFormatter = new Intl.DateTimeFormat("ko-KR", {
  month: "short",
  day: "numeric",
  timeZone: "Asia/Seoul",
});

export function formatRelativeTime(isoString: string): string {
  const timestamp = parseApiDateTime(isoString);
  const diffMs = Date.now() - timestamp;
  const diffMin = Math.floor(diffMs / 60_000);

  if (diffMin < 1) return "방금 전";
  if (diffMin < 60) return `${diffMin}분 전`;

  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours}시간 전`;

  const diffDays = Math.floor(diffHours / 24);
  if (diffDays < 7) return `${diffDays}일 전`;

  return relativeDateFormatter.format(timestamp);
}
