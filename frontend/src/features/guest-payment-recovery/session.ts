import {
  removePaymentStatusToken,
  storePaymentStatusToken,
} from "@/features/payment";
import { parseApiDateTime } from "@/shared/lib";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import type { GuestPaymentStatusRecoveryResponse } from "./api";

function isRecoveryResult(value: unknown): value is GuestPaymentStatusRecoveryResponse {
  if (!value || typeof value !== "object") return false;
  const result = value as Partial<GuestPaymentStatusRecoveryResponse>;
  const expiresAt = typeof result.expiresAt === "string"
    ? parseApiDateTime(result.expiresAt)
    : Number.NaN;
  return typeof result.statusToken === "string"
    && Array.isArray(result.payments)
    && Number.isFinite(expiresAt)
    && expiresAt > Date.now();
}

export function saveGuestPaymentStatusRecovery(
  result: GuestPaymentStatusRecoveryResponse,
): void {
  result.payments.forEach((payment) => {
    storePaymentStatusToken(payment.orderId, result.statusToken);
  });
  try {
    sessionStorage.setItem(
      SESSION_KEYS.guestPaymentStatusRecovery,
      JSON.stringify(result),
    );
  } catch {
    // 상세 조회용 토큰 저장에 성공했다면 현재 화면의 복구 결과는 계속 사용할 수 있다.
  }
}

export function loadGuestPaymentStatusRecovery(): GuestPaymentStatusRecoveryResponse | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEYS.guestPaymentStatusRecovery);
    if (!raw) return null;
    const parsed: unknown = JSON.parse(raw);
    if (!isRecoveryResult(parsed)) {
      clearGuestPaymentStatusRecovery();
      return null;
    }
    return parsed;
  } catch {
    clearGuestPaymentStatusRecovery();
    return null;
  }
}

export function clearGuestPaymentStatusRecovery(): void {
  let orderIds: string[] = [];
  try {
    const raw = sessionStorage.getItem(SESSION_KEYS.guestPaymentStatusRecovery);
    if (raw) {
      const parsed = JSON.parse(raw) as Partial<GuestPaymentStatusRecoveryResponse>;
      orderIds = parsed.payments
        ?.map((payment) => payment.orderId)
        .filter((orderId): orderId is string => typeof orderId === "string") ?? [];
    }
  } catch {
    // 손상된 복구 결과에서도 저장소 키와 확인 가능한 개별 토큰은 아래에서 정리한다.
  }
  orderIds.forEach(removePaymentStatusToken);
  try {
    sessionStorage.removeItem(SESSION_KEYS.guestPaymentStatusRecovery);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}
