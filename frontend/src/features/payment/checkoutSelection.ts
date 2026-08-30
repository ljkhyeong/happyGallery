export type CheckoutMethod = "DEFAULT" | "NAVERPAY" | "KAKAOPAY";

export interface CheckoutSelection {
  method: CheckoutMethod;
  termsAgreed: boolean;
}

export class CheckoutTermsError extends Error {
  constructor() {
    super("간편결제에 필요한 약관에 동의해 주세요.");
  }
}

export function requireCheckoutTerms(selection?: CheckoutSelection): void {
  if (selection && selection.method !== "DEFAULT" && !selection.termsAgreed) {
    throw new CheckoutTermsError();
  }
}

export function tossCheckoutOptions(method: CheckoutMethod = "DEFAULT") {
  return method !== "DEFAULT"
    ? { card: { flowMode: "DIRECT" as const, easyPay: method }, windowTarget: "self" as const }
    : {};
}
