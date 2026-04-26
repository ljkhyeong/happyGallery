/**
 * Toss redirect 직전·직후로 결제 후 표시할 작은 힌트들을 잠시 보관한다.
 *
 * <p>도메인 access token은 confirm 응답에 포함되며, 비회원 success 페이지가 그 token을
 * 다음 화면으로 react-router history state로 전달해 URL 노출 없이 자동 조회한다.
 */
export const PAYMENT_RETURN_KEY = "hg_payment_return_hint";

export interface PaymentReturnHint {
  customerName?: string;
  customerPhone?: string;
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
