import {
  currentCustomerSessionStorageOwner,
  isCurrentCustomerSessionStorageOwner,
  isCustomerSessionStorageOwner,
  sameCustomerSessionStorageOwner,
  type CustomerSessionStorageHandle,
  type CustomerSessionStorageOwner,
} from "@/shared/storage/customerSessionOwner";
import type { CustomerSessionSnapshot } from "@/shared/api/customerSession";

/**
 * Toss redirect 직전·직후로 결제 후 표시할 작은 힌트들을 잠시 보관한다.
 *
 * <p>도메인 access token은 confirm 응답에 포함되며, 비회원 success 페이지가 그 token을
 * 다음 화면으로 react-router history state로 전달해 URL 노출 없이 자동 조회한다.
 */
export const PAYMENT_RETURN_KEY = "hg_payment_return_hint";
export const PAYMENT_CONFIRM_REQUEST_KEY = "hg_payment_confirm_request";
const PAYMENT_STATUS_TOKEN_PREFIX = "hg_payment_status_token:";

export interface PaymentReturnHint {
  customerName?: string;
  customerPhone?: string;
  returnPath?: string;
  orderId?: string;
}

export interface PaymentConfirmRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
}

export type PaymentSessionOwner = CustomerSessionStorageOwner;
export type PaymentSessionHandle<T> = CustomerSessionStorageHandle<T>;

function ownedPaymentSessionValue<T>(
  value: T,
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<T> | null {
  const owner = currentCustomerSessionStorageOwner(expectedSnapshot);
  return owner ? { owner, value } : null;
}

function readPaymentSessionEnvelope<T>(
  raw: string,
  isValue: (value: unknown) => value is T,
): PaymentSessionHandle<T> | null {
  const parsed = JSON.parse(raw) as Partial<PaymentSessionHandle<unknown>>;
  if (
    !isCustomerSessionStorageOwner(parsed.owner)
    || !isValue(parsed.value)
  ) {
    return null;
  }
  return { owner: parsed.owner, value: parsed.value };
}

function readOwnedPaymentSessionHandle<T>(
  raw: string,
  isValue: (value: unknown) => value is T,
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<T> | null {
  const envelope = readPaymentSessionEnvelope(raw, isValue);
  if (
    !envelope
    || !isCurrentCustomerSessionStorageOwner(
      envelope.owner,
      expectedSnapshot,
    )
  ) {
    return null;
  }
  return envelope;
}

function samePaymentSessionHandle<T>(
  left: PaymentSessionHandle<T>,
  right: PaymentSessionHandle<T>,
  sameValue: (leftValue: T, rightValue: T) => boolean,
): boolean {
  return sameCustomerSessionStorageOwner(left.owner, right.owner)
    && sameValue(left.value, right.value);
}

function isPaymentConfirmRequest(value: unknown): value is PaymentConfirmRequest {
  if (typeof value !== "object" || value === null) return false;
  const request = value as Partial<PaymentConfirmRequest>;
  return typeof request.paymentKey === "string"
    && request.paymentKey.trim().length > 0
    && typeof request.orderId === "string"
    && request.orderId.trim().length > 0
    && typeof request.amount === "number"
    && Number.isSafeInteger(request.amount)
    && request.amount > 0;
}

function isPaymentReturnHint(value: unknown): value is PaymentReturnHint {
  if (typeof value !== "object" || value === null) return false;
  const hint = value as Partial<PaymentReturnHint>;
  return (hint.customerName === undefined || typeof hint.customerName === "string")
    && (hint.customerPhone === undefined || typeof hint.customerPhone === "string")
    && (hint.returnPath === undefined || typeof hint.returnPath === "string")
    && (hint.orderId === undefined || typeof hint.orderId === "string");
}

function isPaymentStatusToken(value: unknown): value is string {
  return typeof value === "string" && value.length > 0;
}

export function storePaymentConfirmRequest(
  request: PaymentConfirmRequest,
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentSessionHandle<PaymentConfirmRequest> | null {
  try {
    const envelope = ownedPaymentSessionValue(request, expectedSnapshot);
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
  try {
    const raw = sessionStorage.getItem(PAYMENT_CONFIRM_REQUEST_KEY);
    if (!raw) return null;
    return readOwnedPaymentSessionHandle(
      raw,
      isPaymentConfirmRequest,
      expectedSnapshot,
    );
  } catch {
    return null;
  }
}

export function readPaymentConfirmRequest(
  expectedSnapshot?: CustomerSessionSnapshot,
): PaymentConfirmRequest | null {
  return readPaymentConfirmSession(expectedSnapshot)?.value ?? null;
}

export function removePaymentConfirmRequest(
  expected?: PaymentSessionHandle<PaymentConfirmRequest>,
): void {
  try {
    if (expected) {
      const raw = sessionStorage.getItem(PAYMENT_CONFIRM_REQUEST_KEY);
      if (!raw) return;
      const actual = readPaymentSessionEnvelope(raw, isPaymentConfirmRequest);
      if (
        !actual
        || !samePaymentSessionHandle(
          actual,
          expected,
          (left, right) =>
            left.paymentKey === right.paymentKey
            && left.orderId === right.orderId
            && left.amount === right.amount,
        )
      ) {
        return;
      }
    }
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
    const envelope = ownedPaymentSessionValue(hint, expectedSnapshot);
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
  try {
    const raw = sessionStorage.getItem(PAYMENT_RETURN_KEY);
    if (!raw) return null;
    return readOwnedPaymentSessionHandle(
      raw,
      isPaymentReturnHint,
      expectedSnapshot,
    );
  } catch {
    return null;
  }
}

export function consumePaymentReturnHint(
  expected?: PaymentSessionHandle<PaymentReturnHint>,
): PaymentReturnHint | null {
  try {
    const raw = sessionStorage.getItem(PAYMENT_RETURN_KEY);
    if (!raw) return null;
    const actual = readPaymentSessionEnvelope(raw, isPaymentReturnHint);
    if (!actual) return null;
    if (expected) {
      if (
        !samePaymentSessionHandle(
          actual,
          expected,
          (left, right) =>
            left.customerName === right.customerName
            && left.customerPhone === right.customerPhone
            && left.returnPath === right.returnPath
            && left.orderId === right.orderId,
        )
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
    const envelope = ownedPaymentSessionValue(token, expectedSnapshot);
    if (!envelope) return null;
    sessionStorage.setItem(
      `${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`,
      JSON.stringify(envelope),
    );
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
  try {
    const key = `${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`;
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    return readOwnedPaymentSessionHandle(
      raw,
      isPaymentStatusToken,
      expectedSnapshot,
    )?.value ?? null;
  } catch {
    return null;
  }
}

export function removePaymentStatusToken(
  orderId: string,
  expected?: PaymentSessionHandle<string>,
): void {
  try {
    const key = `${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`;
    if (expected) {
      const raw = sessionStorage.getItem(key);
      if (!raw) return;
      const actual = readPaymentSessionEnvelope(raw, isPaymentStatusToken);
      if (
        !actual
        || !samePaymentSessionHandle(
          actual,
          expected,
          (left, right) => left === right,
        )
      ) {
        return;
      }
    }
    sessionStorage.removeItem(key);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}
