import {
  removePaymentStatusToken,
  storePaymentStatusToken,
  type PaymentSessionHandle,
} from "@/features/payment";
import {
  createOwnedRecoverySessionStore,
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

export const {
  save: saveGuestPaymentStatusRecovery,
  load: loadGuestPaymentStatusRecovery,
  clear: clearGuestPaymentStatusRecovery,
} = createOwnedRecoverySessionStore({
  key: SESSION_KEYS.guestPaymentStatusRecovery,
  isValue: isRecoveryResult,
  // 상세 조회용 주문별 토큰을 봉투보다 먼저 저장해, 봉투 저장이 실패해도 결제 상세는 열 수 있다.
  beforeStore: (recovery, expectedSnapshot) => {
    recovery.value.payments.forEach((payment) => {
      storePaymentStatusToken(payment.orderId, recovery.value.statusToken, expectedSnapshot);
    });
  },
  afterClear: clearPaymentStatusTokens,
});
