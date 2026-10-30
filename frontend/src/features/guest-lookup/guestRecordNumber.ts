import { parsePositiveId } from "../../shared/lib/number.ts";

// 다른 종류 번호를 붙여 넣으면 숫자만 남겨 엉뚱한 기록을 찾지 않도록 받지 않는다.
const OTHER_KIND_PREFIXES: Record<"orders" | "bookings", readonly string[]> = {
  orders: ["BK", "예약"],
  bookings: ["ORD", "주문"],
};

/**
 * 비회원 조회 칸의 주문·예약 번호를 읽는다. 화면·알림의 "주문 #123", 예전 상세 머리의 "ORD-00000123"·"BK-00000045",
 * 앞자리 0을 붙인 번호를 모두 받아 숫자만 남겨 해석한다.
 */
export function parseGuestRecordNumber(value: string, kind: "orders" | "bookings"): number | null {
  const normalized = value.trim().toUpperCase();
  if (OTHER_KIND_PREFIXES[kind].some((prefix) => normalized.startsWith(prefix))) return null;
  const digits = normalized.replace(/\D/g, "");
  return digits ? parsePositiveId(digits) : null;
}
