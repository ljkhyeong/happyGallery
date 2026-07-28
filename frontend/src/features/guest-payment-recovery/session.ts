import {
  removePaymentStatusToken,
  storePaymentStatusToken,
  type PaymentSessionHandle,
} from "@/features/payment";
import type { CustomerSessionSnapshot } from "@/shared/api";
import { parseApiDateTime } from "@/shared/lib";
import {
  currentCustomerSessionStorageOwner,
  isCurrentCustomerSessionStorageOwner,
  isCustomerSessionStorageOwner,
  sameCustomerSessionStorageOwner,
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

function readGuestPaymentStatusRecovery(
  raw: string,
): GuestPaymentStatusRecoverySession | null {
  const parsed = JSON.parse(raw) as Partial<CustomerSessionStorageHandle<unknown>>;
  if (
    !isCustomerSessionStorageOwner(parsed.owner)
    || !isRecoveryResult(parsed.value)
  ) {
    return null;
  }
  return { owner: parsed.owner, value: parsed.value };
}

function sameGuestPaymentStatusRecovery(
  left: GuestPaymentStatusRecoverySession,
  right: GuestPaymentStatusRecoverySession,
): boolean {
  return sameCustomerSessionStorageOwner(left.owner, right.owner)
    && JSON.stringify(left.value) === JSON.stringify(right.value);
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
  const owner = currentCustomerSessionStorageOwner(expectedSnapshot);
  if (!owner) return null;
  const recovery = { owner, value: result };
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
  try {
    const raw = sessionStorage.getItem(SESSION_KEYS.guestPaymentStatusRecovery);
    if (!raw) return null;
    const recovery = readGuestPaymentStatusRecovery(raw);
    if (
      !recovery
      || !isCurrentCustomerSessionStorageOwner(
        recovery.owner,
        expectedSnapshot,
      )
    ) {
      return null;
    }
    const expiresAt = parseApiDateTime(recovery.value.expiresAt);
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      clearGuestPaymentStatusRecovery(recovery);
      return null;
    }
    return recovery;
  } catch {
    return null;
  }
}

export function clearGuestPaymentStatusRecovery(
  expected?: GuestPaymentStatusRecoverySession,
): void {
  const recovery = expected ?? loadGuestPaymentStatusRecovery();
  if (!recovery) return;
  try {
    const raw = sessionStorage.getItem(SESSION_KEYS.guestPaymentStatusRecovery);
    if (!raw) return;
    const stored = readGuestPaymentStatusRecovery(raw);
    if (!stored || !sameGuestPaymentStatusRecovery(stored, recovery)) return;
    sessionStorage.removeItem(SESSION_KEYS.guestPaymentStatusRecovery);
    clearPaymentStatusTokens(recovery);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}
