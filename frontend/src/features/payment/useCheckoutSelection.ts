import { useState } from "react";
import type { CheckoutSelection } from "./checkoutSelection";

export function useCheckoutSelection() {
  return useState<CheckoutSelection>({ method: "DEFAULT", termsAgreed: false });
}
