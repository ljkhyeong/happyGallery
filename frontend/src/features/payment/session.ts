import {
  isCurrentCustomerSessionStorageOwner,
  ownedSessionValue,
  parseOwnedSessionValue,
  readOwnedSessionValue,
  removeOwnedSessionValue,
  sameCustomerSessionStorageOwner,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import type { CustomerSessionSnapshot } from "@/shared/api/customerSession";

/**
 * Toss redirect 직전·직후로 결제 후 표시할 작은 힌트들을 잠시 보관한다.
 *
 * <p>도메인 access token은 confirm 응답에 포함되며, 비회원 success 페이지가 그 token을
 * 다음 화면으로 react-router history state로 전달해 URL 노출 없이 자동 조회한다.
 */
const PAYMENT_RETURN_KEY = "hg_payment_return_hint";
const PAYMENT_CONFIRM_REQUEST_KEY = "hg_payment_confirm_request";
const PAYMENT_STATUS_TOKEN_PREFIX = "hg_payment_status_token:";

export interface PaymentReturnHint {
  customerName?: string;
  customerPhone?: string;
  returnPath?: string;
  orderId?: string;
}

export interface PaymentConfirmRequest {
  paymentKey: string | null;
  orderId: string;
  amount: number;
}

export type PaymentSessionHandle<T> = CustomerSessionStorageHandle<T>;

function isPaymentConfirmRequest(value: unknown): value is PaymentConfirmRequest {
  if (typeof value !== "object" || value === null) return false;
  const request = value as Partial<PaymentConfirmRequest>;
  return typeof request.orderId === "string"
    && request.orderId.trim().length > 0
    && typeof request.amount === "number"
    && Number.isSafeInteger(request.amount)
    && (request.amount === 0
      ? request.paymentKey === null
      : request.amount > 0 && typeof request.paymentKey === "string" && request.paymentKey.trim().length > 0);
}

function samePaymentConfirmRequest(left: PaymentConfirmRequest, right: PaymentConfirmRequest): boolean {
  return left.paymentKey === right.paymentKey
    && left.orderId === right.orderId
    && left.amount === right.amount;
}

function isPaymentReturnHint(value: unknown): value is PaymentReturnHint {
  if (typeof value !== "object" || value === null) return false;
  const hint = value as Partial<PaymentReturnHint>;
  return (hint.customerName === undefined || typeof hint.customerName === "string")
    && (hint.customerPhone === undefined || typeof hint.customerPhone === "string")
    && (hint.returnPath === undefined || typeof hint.returnPath === "string")
    && (hint.orderId === undefined || typeof hint.orderId === "string");
}

function samePaymentReturnHint(left: PaymentReturnHint, right: PaymentReturnHint): boolean {
  return left.customerName === right.customerName
    && left.customerPhone === right.customerPhone
    && left.returnPath === right.returnPath
    && left.orderId === right.orderId;
}

function isPaymentStatusToken(value: unknown): value is string {
  return typeof value === "string" && value.length > 0;
}

function paymentStatusTokenKey(orderId: string): string {
  return `${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`;
}

export function storePaymentConfirmRequest(
  request: PaymentConfirmRequest,
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<PaymentConfirmRequest> | null {
  try {
    const envelope = ownedSessionValue(request, expectedSnapshot);
    if (!envelope) return null;
    sessionStorage.setItem(
      PAYMENT_CONFIRM_REQUEST_KEY,
      JSON.stringify(envelope),
    );
    return envelope;
  } catch {
    // sessionStorage 비활성 환경에서는 Toss 콜백 URL의 값으로 현재 시도만 처리한다.
    return null;
  }
}

export function readPaymentConfirmSession(
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<PaymentConfirmRequest> | null {
  return readOwnedSessionValue(PAYMENT_CONFIRM_REQUEST_KEY, isPaymentConfirmRequest, expectedSnapshot);
}

export function removePaymentConfirmRequest(
  expected?: PaymentSessionHandle<PaymentConfirmRequest>,
): void {
  if (expected) {
    removeOwnedSessionValue(PAYMENT_CONFIRM_REQUEST_KEY, isPaymentConfirmRequest, expected, samePaymentConfirmRequest);
    return;
  }
  try {
    sessionStorage.removeItem(PAYMENT_CONFIRM_REQUEST_KEY);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}

export function storePaymentReturnHint(
  hint: PaymentReturnHint,
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<PaymentReturnHint> | null {
  try {
    const envelope = ownedSessionValue(hint, expectedSnapshot);
    if (!envelope) return null;
    sessionStorage.setItem(PAYMENT_RETURN_KEY, JSON.stringify(envelope));
    return envelope;
  } catch {
    // sessionStorage 비활성 환경 — 무시
    return null;
  }
}

export function readPaymentReturnHint(
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<PaymentReturnHint> | null {
  return readOwnedSessionValue(PAYMENT_RETURN_KEY, isPaymentReturnHint, expectedSnapshot);
}

export function consumePaymentReturnHint(
  expected?: PaymentSessionHandle<PaymentReturnHint>,
): PaymentReturnHint | null {
  try {
    const raw = sessionStorage.getItem(PAYMENT_RETURN_KEY);
    if (!raw) return null;
    const actual = parseOwnedSessionValue(raw, isPaymentReturnHint);
    if (!actual) return null;
    if (expected) {
      if (
        !sameCustomerSessionStorageOwner(actual.owner, expected.owner)
        || !samePaymentReturnHint(actual.value, expected.value)
      ) {
        return null;
      }
    } else if (!isCurrentCustomerSessionStorageOwner(actual.owner)) {
      return null;
    }
    sessionStorage.removeItem(PAYMENT_RETURN_KEY);
    return actual.value;
  } catch {
    return null;
  }
}

export function removePaymentReturnHint(
  expected?: PaymentSessionHandle<PaymentReturnHint>,
): void {
  if (expected) {
    void consumePaymentReturnHint(expected);
    return;
  }
  try {
    sessionStorage.removeItem(PAYMENT_RETURN_KEY);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}

export function storePaymentStatusToken(
  orderId: string,
  token: string | null,
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<string> | null {
  if (!token) return null;
  try {
    const envelope = ownedSessionValue(token, expectedSnapshot);
    if (!envelope) return null;
    sessionStorage.setItem(paymentStatusTokenKey(orderId), JSON.stringify(envelope));
    return envelope;
  } catch {
    // sessionStorage 비활성 환경 — 회원 결제는 세션으로, 비회원은 고객센터 복구 경로로 확인한다.
    return null;
  }
}

export function readPaymentStatusToken(
  orderId: string,
  expectedSnapshot?: CustomerSessionSnapshot,
): string | null {
  return readOwnedSessionValue(paymentStatusTokenKey(orderId), isPaymentStatusToken, expectedSnapshot)
    ?.value ?? null;
}

export function removePaymentStatusToken(
  orderId: string,
  expected?: PaymentSessionHandle<string>,
): void {
  const key = paymentStatusTokenKey(orderId);
  if (expected) {
    removeOwnedSessionValue(key, isPaymentStatusToken, expected, (left, right) => left === right);
    return;
  }
  try {
    sessionStorage.removeItem(key);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}
