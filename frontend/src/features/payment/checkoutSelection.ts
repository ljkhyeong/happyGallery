export type CheckoutMethod = "DEFAULT" | "NAVERPAY";

export interface CheckoutSelection {
  method: CheckoutMethod;
  termsAgreed: boolean;
}

export class CheckoutTermsError extends Error {
  constructor() {
    super("네이버페이 결제에 필요한 약관에 동의해 주세요.");
  }
}

export function requireCheckoutTerms(selection?: CheckoutSelection): void {
  if (selection?.method === "NAVERPAY" && !selection.termsAgreed) {
    throw new CheckoutTermsError();
  }
}

export function tossCheckoutOptions(method: CheckoutMethod = "DEFAULT") {
  return method === "NAVERPAY"
    ? { card: { flowMode: "DIRECT" as const, easyPay: "NAVERPAY" as const }, windowTarget: "self" as const }
    : {};
}
