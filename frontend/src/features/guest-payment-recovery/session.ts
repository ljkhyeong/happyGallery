import {
  removePaymentStatusToken,
  storePaymentStatusToken,
  type PaymentSessionHandle,
} from "@/features/payment";
import type { CustomerSessionSnapshot } from "@/shared/api";
import { parseApiDateTime } from "@/shared/lib";
import {
  ownedSessionValue,
  readOwnedSessionValue,
  removeOwnedSessionValue,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import type { GuestPaymentStatusRecoveryResponse } from "./api";

export type GuestPaymentStatusRecoverySession =
  CustomerSessionStorageHandle<GuestPaymentStatusRecoveryResponse>;

function isRecoveryResult(value: unknown): value is GuestPaymentStatusRecoveryResponse {
  if (!value || typeof value !== "object") return false;
  const result = value as Partial<GuestPaymentStatusRecoveryResponse>;
  return typeof result.statusToken === "string"
    && result.statusToken.length > 0
    && typeof result.expiresAt === "string"
    && Array.isArray(result.payments);
}

function sameRecoveryResult(
  left: GuestPaymentStatusRecoveryResponse,
  right: GuestPaymentStatusRecoveryResponse,
): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}

function paymentTokenHandle(
  recovery: GuestPaymentStatusRecoverySession,
): PaymentSessionHandle<string> {
  return {
    owner: recovery.owner,
    value: recovery.value.statusToken,
  };
}

function clearPaymentStatusTokens(
  recovery: GuestPaymentStatusRecoverySession,
): void {
  const expectedToken = paymentTokenHandle(recovery);
  recovery.value.payments.forEach((payment) => {
    removePaymentStatusToken(payment.orderId, expectedToken);
  });
}

export function saveGuestPaymentStatusRecovery(
  result: GuestPaymentStatusRecoveryResponse,
  expectedSnapshot?: CustomerSessionSnapshot,
): GuestPaymentStatusRecoverySession | null {
  const recovery = ownedSessionValue(result, expectedSnapshot);
  if (!recovery) return null;
  result.payments.forEach((payment) => {
    storePaymentStatusToken(
      payment.orderId,
      result.statusToken,
      expectedSnapshot,
    );
  });
  try {
    sessionStorage.setItem(
      SESSION_KEYS.guestPaymentStatusRecovery,
      JSON.stringify(recovery),
    );
  } catch {
    // 상세 조회용 토큰 저장에 성공했다면 현재 화면의 복구 결과는 계속 사용할 수 있다.
  }
  return recovery;
}

export function loadGuestPaymentStatusRecovery(
  expectedSnapshot?: CustomerSessionSnapshot,
): GuestPaymentStatusRecoverySession | null {
  const recovery = readOwnedSessionValue(
    SESSION_KEYS.guestPaymentStatusRecovery,
    isRecoveryResult,
    expectedSnapshot,
  );
  if (!recovery) return null;
  const expiresAt = parseApiDateTime(recovery.value.expiresAt);
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    clearGuestPaymentStatusRecovery(recovery);
    return null;
  }
  return recovery;
}

export function clearGuestPaymentStatusRecovery(
  expected?: GuestPaymentStatusRecoverySession,
): void {
  const recovery = expected ?? loadGuestPaymentStatusRecovery();
  if (!recovery) return;
  removeOwnedSessionValue(
    SESSION_KEYS.guestPaymentStatusRecovery,
    isRecoveryResult,
    recovery,
    sameRecoveryResult,
  );
  clearPaymentStatusTokens(recovery);
}
