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
}

export interface PaymentConfirmRequest {
  paymentKey: string;
  orderId: string;
  amount: number;
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

export function storePaymentConfirmRequest(request: PaymentConfirmRequest): void {
  try {
    sessionStorage.setItem(PAYMENT_CONFIRM_REQUEST_KEY, JSON.stringify(request));
  } catch {
    // sessionStorage 비활성 환경에서는 Toss 콜백 URL의 값으로 현재 시도만 처리한다.
  }
}

export function readPaymentConfirmRequest(): PaymentConfirmRequest | null {
  try {
    const raw = sessionStorage.getItem(PAYMENT_CONFIRM_REQUEST_KEY);
    if (!raw) return null;
    const request = JSON.parse(raw) as unknown;
    if (isPaymentConfirmRequest(request)) return request;
    sessionStorage.removeItem(PAYMENT_CONFIRM_REQUEST_KEY);
    return null;
  } catch {
    return null;
  }
}

export function removePaymentConfirmRequest(orderId?: string): void {
  try {
    if (orderId) {
      const request = readPaymentConfirmRequest();
      if (request && request.orderId !== orderId) return;
    }
    sessionStorage.removeItem(PAYMENT_CONFIRM_REQUEST_KEY);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}

export function storePaymentReturnHint(hint: PaymentReturnHint): void {
  try {
    sessionStorage.setItem(PAYMENT_RETURN_KEY, JSON.stringify(hint));
  } catch {
    // sessionStorage 비활성 환경 — 무시
  }
}

export function consumePaymentReturnHint(): PaymentReturnHint | null {
  try {
    const raw = sessionStorage.getItem(PAYMENT_RETURN_KEY);
    if (!raw) return null;
    sessionStorage.removeItem(PAYMENT_RETURN_KEY);
    return JSON.parse(raw) as PaymentReturnHint;
  } catch {
    return null;
  }
}

export function storePaymentStatusToken(orderId: string, token: string | null): void {
  if (!token) return;
  try {
    sessionStorage.setItem(`${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`, token);
  } catch {
    // sessionStorage 비활성 환경 — 회원 결제는 세션으로, 비회원은 고객센터 복구 경로로 확인한다.
  }
}

export function readPaymentStatusToken(orderId: string): string | null {
  try {
    return sessionStorage.getItem(`${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`);
  } catch {
    return null;
  }
}

export function removePaymentStatusToken(orderId: string): void {
  try {
    sessionStorage.removeItem(`${PAYMENT_STATUS_TOKEN_PREFIX}${orderId}`);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}
